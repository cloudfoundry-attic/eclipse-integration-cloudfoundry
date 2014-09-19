/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *     IBM Corporation - combine IApplicationDelegate and ApplicationDelegate
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.LocalCloudService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * API that application contributions through the extension point:
 * <p/>
 * org.cloudfoundry.ide.eclipse.server.core.application
 * <p/>
 * are required to implement. Applications are represented as Eclipse WST
 * IModules. The main contributions for an application is:
 * 
 * <ul>
 * <li>Whether the application requires a URL to be set before being published.
 * URLs are set through the Cloud Foundry plugin UI. In most case, this is true,
 * except Java standalone applications</li>
 * <li>Optionally, an archiving mechanism for the application's resources that
 * should be pushed to a Cloud Foundry server</li>
 * 
 * </ul>
 * 
 */
public abstract class AbstractApplicationDelegate {

	/**
	 * 
	 * In most cases, this is true. Java Web type applications require a URL,
	 * but some application types, like Java Standalone do not
	 * @return true if the application requires that a URL be set when
	 * publishing the application. False otherwise.
	 */
	public abstract boolean requiresURL();

	/**
	 * A light-weight way of telling the framework whether this application
	 * delegate contributes an application archive for application resource
	 * serialisation when pushing the application's resources to a Cloud Foundry
	 * Server. If false, it means the framework should create it's default
	 * payload for the application (typically by using a .war file).
	 * @param module corresponding to an application that should be published to
	 * a Cloud Foundry server
	 * @return true if this delegate provides its own application serialisation
	 * mechanism, or false otherwise
	 */
	public abstract boolean providesApplicationArchive(IModule module);

	/**
	 * An application archive generates input streams for an application's files
	 * when the Cloud Foundry framework is ready to push an application to a
	 * Cloud Foundry server. Files are represented by IModuleResource, and the
	 * archive generates an input stream for each IModuleResource.
	 * 
	 * <p/>
	 * In addition, the application archive is also used to calculate sha1 hash
	 * codes for each application file so that the Cloud Foundry server can
	 * determine what resources have changed prior to the Cloud Foundry
	 * framework pushing any changes to the application.
	 * <p/>
	 * For Java Web type applications (Spring, Grails, Java Web), it is not
	 * necessary to provide an explicit application archive, as the Cloud
	 * Foundry plugin framework generates .war files for such applications and
	 * uses a built-in default application archive that reads the .war file.
	 * <p/>
	 * However for some other application types like for example Java
	 * standalone, .war files are not generated, and therefore serialisation of
	 * the application files is performed through an application archive
	 * specific to Java standalone applications
	 * 
	 * <p/>
	 * The default implementation provides a general way to generated an
	 * application archive from IModuleResources for a given IModule. Subclasses
	 * can override this and provide their own archive which converts
	 * IModuleResource into archive entries.
	 * 
	 * <p/>
	 * Alternately, subclasses can return null if no application archive needs
	 * to be used and the framework .war file generation should be used instead.
	 * 
	 * @param module for the application that needs to be published.
	 * @param cloudServer server where application should be deployed (or where
	 * it is currently deployed, and app resources need to be updated).
	 * @param moduleResources corresponding module resources for the module that
	 * needs to be published. These module resources are typically used to
	 * generated the archive
	 * @return Application archive for the give module resources, or null if no
	 * archive is required and the framework should create a .war file for the
	 * application
	 * @throws CoreException if the application delegate provides an application
	 * archive but it failed to create one.
	 */
	public abstract ApplicationArchive getApplicationArchive(CloudFoundryApplicationModule module,
			CloudFoundryServer cloudServer, IModuleResource[] moduleResources, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * {@link IStatus#OK} If the deployment information is valid. Otherwise
	 * return {@link IStatus#ERROR} if error. Must NOT return a null status.
	 * @param deploymentInfo
	 * @return non-null status.
	 */
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		return basicValidateDeploymentInfo(deploymentInfo);
	}

	/**
	 * Resolve an application deployment for the given application. Return null
	 * if it cannot be resolved. If returning non-null value, it should always
	 * be a new copy of a deployment info.
	 * @param Cloud server where application exists.
	 * @param module application that already exists in the server
	 * @return A new copy of the deployment information for an existing
	 * application, or null if it cannot be resolved.
	 */
	public ApplicationDeploymentInfo resolveApplicationDeploymentInfo(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		return parseApplicationDeploymentInfo(appModule.getApplication());
	}

	/**
	 * Get a default application deployment information, regardless of whether
	 * the application is deployed or not. It should contain default settings
	 * for this type of application (e.g. memory, default URL, if necessary,
	 * etc..). Should not be null.
	 * @param appModule
	 * @param cloudServer
	 * @return Non-null application deployment information with default values.
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.core.internal.application.
	 * AbstractApplicationDelegate
	 * #getDefaultApplicationDeploymentInfo(org.cloudfoundry.
	 * ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule,
	 * org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException {

		// Set default values.
		String appName = appModule.getDeployedApplicationName();
		ApplicationDeploymentInfo deploymentInfo = new ApplicationDeploymentInfo(appName);
		deploymentInfo.setMemory(CloudUtil.DEFAULT_MEMORY);

		return deploymentInfo;
	}

	public static IStatus basicValidateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		IStatus status = Status.OK_STATUS;

		String errorMessage = null;

		if (deploymentInfo == null) {
			errorMessage = Messages.AbstractApplicationDelegate_ERROR_MISSING_DEPLOY_INFO;
		}
		else if (ValueValidationUtil.isEmpty(deploymentInfo.getDeploymentName())) {
			errorMessage = Messages.AbstractApplicationDelegate_ERROR_MISSING_APPNAME;
		}
		else if (deploymentInfo.getMemory() <= 0) {
			errorMessage = Messages.AbstractApplicationDelegate_ERROR_MISSING_MEM;
		}

		if (errorMessage != null) {
			status = CloudFoundryPlugin.getErrorStatus(errorMessage);
		}

		return status;
	}

	/**
	 * Parses deployment information from a deployed Cloud Application. Returns
	 * null if the cloud application is null.
	 * @param cloudApplication deployed in a CF server
	 * @return Parsed deployment information, or null if Cloud Application is
	 * null.
	 */
	public static ApplicationDeploymentInfo parseApplicationDeploymentInfo(CloudApplication cloudApplication) {

		if (cloudApplication != null) {

			String deploymentName = cloudApplication.getName();
			ApplicationDeploymentInfo deploymentInfo = new ApplicationDeploymentInfo(deploymentName);

			deploymentInfo.setStaging(cloudApplication.getStaging());
			deploymentInfo.setMemory(cloudApplication.getMemory());
			List<String> boundServiceNames = cloudApplication.getServices();
			if (boundServiceNames != null) {
				List<CloudService> services = new ArrayList<CloudService>();
				for (String name : boundServiceNames) {
					if (name != null) {
						services.add(new LocalCloudService(name));
					}
				}
				deploymentInfo.setServices(services);
			}

			if (cloudApplication.getUris() != null) {
				deploymentInfo.setUris(new ArrayList<String>(cloudApplication.getUris()));
			}

			Map<String, String> envMap = cloudApplication.getEnvAsMap();

			if (envMap != null) {
				List<EnvironmentVariable> variables = new ArrayList<EnvironmentVariable>();
				for (Entry<String, String> entry : envMap.entrySet()) {
					String varName = entry.getKey();
					if (varName != null) {
						EnvironmentVariable variable = new EnvironmentVariable();
						variable.setVariable(varName);
						variable.setValue(entry.getValue());
						variables.add(variable);
					}
				}
				deploymentInfo.setEnvVariables(variables);
			}
			return deploymentInfo;

		}
		return null;
	}

}
