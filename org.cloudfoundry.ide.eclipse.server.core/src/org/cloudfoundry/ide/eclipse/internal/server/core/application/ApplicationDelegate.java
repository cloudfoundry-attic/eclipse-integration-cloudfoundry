/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.LocalCloudService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class ApplicationDelegate implements IApplicationDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.core.application.
	 * IApplicationDelegate
	 * #getDefaultApplicationDeploymentInfo(org.cloudfoundry.
	 * ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule,
	 * org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer)
	 */
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {

		// Set default values.
		String appName = appModule.getDeployedApplicationName();
		ApplicationDeploymentInfo deploymentInfo = new ApplicationDeploymentInfo(appName);
		deploymentInfo.setMemory(CloudUtil.DEFAULT_MEMORY);
		deploymentInfo.setDeploymentMode(ApplicationAction.START);

		return deploymentInfo;
	}

	public ApplicationDeploymentInfo resolveApplicationDeploymentInfo(CloudApplication cloudApplication,
			CloudFoundryServer cloudServer) {

		return parseApplicationDeploymentInfo(cloudApplication);
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
			deploymentInfo.setDeploymentMode(ApplicationAction.START);
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
