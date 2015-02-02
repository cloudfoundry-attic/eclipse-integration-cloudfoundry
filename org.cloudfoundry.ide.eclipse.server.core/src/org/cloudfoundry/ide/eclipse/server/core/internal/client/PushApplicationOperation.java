/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Operation publish an application. If the application is already deployed and
 * synchronised, it will only update the mapping between the module and the
 * {@link CloudApplication}.
 * 
 * <p/>
 * 1. Prompts for deployment information.
 * <p/>
 * 2. Creates the application if the application does not currently exist in the
 * server
 * <p/>
 * 3. Starts the application if specified in the deployment configuration for
 * the application.
 * <p/>
 * If the application is already published (it exists in the server), it will
 * ONLY update the published cloud application mapping in the
 * {@link CloudFoundryApplicationModule}. It will NOT re-create, re-publish, or
 * restart the application.
 * <p/>
 *
 */
class PushApplicationOperation extends StartOperation {

	/**
	 * 
	 */

	public PushApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour, false, modules);
	}

	@Override
	protected DeploymentConfiguration prepareForDeployment(CloudFoundryApplicationModule appModule,
			IProgressMonitor monitor) throws CoreException {
		// If the app is already published, just refresh the application
		// mapping.
		int moduleState = getBehaviour().getServer()
				.getModulePublishState(new IModule[] { appModule.getLocalModule() });
		if (appModule.isDeployed() && moduleState == IServer.PUBLISH_STATE_NONE) {

			getBehaviour().printlnToConsole(appModule, Messages.CONSOLE_APP_FOUND);

			getBehaviour().printlnToConsole(appModule,
					NLS.bind(Messages.CONSOLE_APP_MAPPING_STARTED, appModule.getDeployedApplicationName()));
			try {
				getBehaviour().updateCloudModule(appModule.getDeployedApplicationName(), monitor);
				getBehaviour().printlnToConsole(appModule,
						NLS.bind(Messages.CONSOLE_APP_MAPPING_COMPLETED, appModule.getDeployedApplicationName()));

			}
			catch (CoreException e) {
				// Do not log the error. The application may not exist
				// anymore. If it is a network error, it will become evident
				// in further steps
			}
		}
		else {
			try {
				CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

				// prompt user for missing details
				return CloudFoundryPlugin.getCallback().prepareForDeployment(cloudServer, appModule, monitor);
			}
			catch (OperationCanceledException oce) {
				// Prepare for deployment prompts the user for missing
				// information for a non-published app. If a user
				// cancels
				// delete the application module
				getBehaviour().getCloudFoundryServer().doDeleteModules(Arrays.asList(getModules()));
				throw oce;
			}
		}
		return null;
	}

	@Override
	protected void pushApplication(CloudFoundryOperations client, final CloudFoundryApplicationModule appModule,
			File warFile, ApplicationArchive applicationArchive, final IProgressMonitor monitor) throws CoreException {
		String appName = appModule.getDeploymentInfo().getDeploymentName();

		List<CloudApplication> existingApps = client.getApplications();
		boolean found = false;
		for (CloudApplication existingApp : existingApps) {
			if (existingApp.getName().equals(appName)) {
				found = true;
				break;
			}
		}

		// Create the application if it doesn't already exist
		if (!found) {
			getBehaviour().printlnToConsole(appModule, Messages.CONSOLE_APP_CREATION);

			Staging staging = appModule.getDeploymentInfo().getStaging();
			List<String> uris = appModule.getDeploymentInfo().getUris() != null ? appModule.getDeploymentInfo()
					.getUris() : new ArrayList<String>(0);
			List<String> services = appModule.getDeploymentInfo().asServiceBindingList();

			if (staging == null) {
				// For v2, a non-null staging is required.
				staging = new Staging();
			}
			client.createApplication(appName, staging, appModule.getDeploymentInfo().getMemory(), uris, services);
		}
		super.pushApplication(client, appModule, warFile, applicationArchive, monitor);
	}

	@Override
	protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		if (!appModule.isDeployed()) {
			super.performDeployment(appModule, monitor);
		}
	}

}