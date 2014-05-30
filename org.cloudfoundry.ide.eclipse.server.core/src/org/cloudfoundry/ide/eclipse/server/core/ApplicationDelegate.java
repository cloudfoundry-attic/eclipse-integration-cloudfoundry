/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.LocalCloudService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class ApplicationDelegate implements IApplicationDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.core.internal.application.
	 * IApplicationDelegate
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

	public ApplicationDeploymentInfo resolveApplicationDeploymentInfo(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		return parseApplicationDeploymentInfo(appModule.getApplication());
	}

	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		return basicValidateDeploymentInfo(deploymentInfo);
	}

	public static IStatus basicValidateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		IStatus status = Status.OK_STATUS;

		String errorMessage = null;

		if (deploymentInfo == null) {
			errorMessage = "Missing application deployment information.";
		}
		else if (ValueValidationUtil.isEmpty(deploymentInfo.getDeploymentName())) {
			errorMessage = "Missing application name in application deployment information.";
		}
		else if (deploymentInfo.getMemory() <= 0) {
			errorMessage = "No memory set in application deployment information.";
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
