/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Deletes a given set of application modules. The modules need not have associated deployed
 * applications.
 */
public class DeleteModulesOperation extends AbstractDeploymentOperation {

	/**
	 * 
	 */
	private final CloudFoundryServerBehaviour cloudFoundryServerBehaviour;

	private final boolean deleteServices;

	private final IModule[] modules;

	public DeleteModulesOperation(CloudFoundryServerBehaviour cloudFoundryServerBehaviour, IModule[] modules,
			boolean deleteServices, CloudFoundryServerBehaviour behaviour) {
		super(behaviour);
		this.cloudFoundryServerBehaviour = cloudFoundryServerBehaviour;
		this.modules = modules;
		this.deleteServices = deleteServices;
	}

	protected void performOperation(IProgressMonitor monitor) throws CoreException {
		// NOTE that modules do NOT necessarily have to have deployed
		// applications, so it's incorrect to assume
		// that any module being deleted will also have a corresponding
		// CloudApplication.
		// A case for this is if a user cancels an application deployment. The
		// IModule would have already been created
		// but there would be no corresponding CloudApplication.
		final CloudFoundryServer cloudServer = this.cloudFoundryServerBehaviour.getCloudFoundryServer();

		List<CloudApplication> updatedApplications = this.cloudFoundryServerBehaviour.getApplications(monitor);

		for (IModule module : modules) {
			final CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

			if (appModule == null) {
				continue;
			}

			List<String> servicesToDelete = new ArrayList<String>();

			// Fetch an updated application so that it has the lastest
			// service list
			CloudApplication application = null;
			if (updatedApplications != null) {
				for (CloudApplication app : updatedApplications) {
					if (app.getName().equals(appModule.getDeployedApplicationName())) {
						application = app;
						break;
					}
				}
			}

			// ONLY delete a remote application if an application is found.
			if (application != null) {
				List<String> actualServices = application.getServices();
				if (actualServices != null) {
					// This has to be used instead of addAll(..), as
					// there is a chance the list is non-empty but
					// contains null entries
					for (String serviceName : actualServices) {
						if (serviceName != null) {
							servicesToDelete.add(serviceName);
						}
					}
				}

				this.cloudFoundryServerBehaviour.deleteApplication(application.getName(), monitor);

			}

			CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, cloudServer);

			// Delete the module locally
			cloudServer.removeApplication(appModule);

			// Be sure the cloud application mapping is removed
			// in case other components still have a reference to
			// the
			// module
			appModule.setCloudApplication(null);

			// Prompt the user to delete services as well
			if (deleteServices && !servicesToDelete.isEmpty()) {
				CloudFoundryPlugin.getCallback().deleteServices(servicesToDelete, cloudServer);
				ServerEventHandler.getDefault().fireServicesUpdated(cloudServer);
			}
		}
	}

}