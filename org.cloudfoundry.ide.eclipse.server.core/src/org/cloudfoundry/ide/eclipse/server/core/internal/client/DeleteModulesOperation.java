/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

/**
 * Deletes a given set of application modules. The modules need not have
 * associated deployed applications.
 */
public class DeleteModulesOperation extends BehaviourOperation {

	/**
	 * 
	 */

	private final boolean deleteServices;

	private final IModule[] modules;

	public DeleteModulesOperation(CloudFoundryServerBehaviour cloudFoundryServerBehaviour, IModule[] modules,
			boolean deleteServices) {
		super(cloudFoundryServerBehaviour, modules.length > 0 ? modules[0] : null);
		this.modules = modules;
		this.deleteServices = deleteServices;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		doDelete(monitor);
		getBehaviour().getRefreshHandler().scheduleRefreshForDeploymentChange(getModule());
	}

	protected void doDelete(IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

		for (IModule module : modules) {
			final CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

			if (appModule == null) {
				continue;
			}

			// Fetch an updated application. Do not fetch all applications as it
			// may slow down the
			// deletion process, only fetch the app being deleted
			CloudApplication application = null;

			try {
				application = getBehaviour().getCloudApplication(appModule.getDeployedApplicationName(), monitor);
			}
			catch (Throwable t) {
				// Ignore if not found. The app does not exist
				if (!CloudErrorUtil.isNotFoundException(t)) {
					throw t;
				}
			}

			// NOTE that modules do NOT necessarily have to have deployed
			// applications, so it's incorrect to assume
			// that any module being deleted will also have a corresponding
			// CloudApplication.
			// A case for this is if a user cancels an application deployment.
			// The
			// IModule would have already been created
			// but there would be no corresponding CloudApplication.

			List<String> servicesToDelete = new ArrayList<String>();

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

				getBehaviour().deleteApplication(application.getName(), monitor);

			}

			CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, cloudServer);

			// Delete the module locally
			cloudServer.removeApplication(appModule);

			ServerEventHandler.getDefault().fireServerEvent(
					new ModuleChangeEvent(getBehaviour().getCloudFoundryServer(), CloudServerEvent.EVENT_APP_DELETED,
							appModule.getLocalModule(), Status.OK_STATUS));

			// Be sure the cloud application mapping is removed
			// in case other components still have a reference to
			// the
			// module
			appModule.setCloudApplication(null);

			// Prompt the user to delete services as well
			if (deleteServices && !servicesToDelete.isEmpty()) {
				CloudFoundryPlugin.getCallback().deleteServices(servicesToDelete, cloudServer);
			}
		}
	}

}