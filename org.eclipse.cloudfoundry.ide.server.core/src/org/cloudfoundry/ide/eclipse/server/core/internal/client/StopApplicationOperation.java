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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

@SuppressWarnings("restriction")
class StopApplicationOperation extends AbstractPublishApplicationOperation {

	/**
	 * 
	 */

	protected StopApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour, modules);
	}

	@Override
	public String getOperationName() {
		return Messages.StopApplicationOperation_STOPPING_APP;
	}

	@Override
	protected void doApplicationOperation(IProgressMonitor monitor) throws CoreException {
		Server server = (Server) getBehaviour().getServer();

		boolean succeeded = false;
		try {
			server.setModuleState(getModules(), IServer.STATE_STOPPING);

			CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

			final CloudFoundryApplicationModule cloudModule = cloudServer.getExistingCloudModule(getModule());

			if (cloudModule == null) {
				throw CloudErrorUtil.toCoreException("Unable to stop application as no cloud module found for: " //$NON-NLS-1$
						+ getModules()[0].getName());
			}

			SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

			String stoppingApplicationMessage = NLS.bind(Messages.CONSOLE_STOPPING_APPLICATION,
					cloudModule.getDeployedApplicationName());

			getBehaviour().clearAndPrintlnConsole(cloudModule, stoppingApplicationMessage);

			subMonitor.worked(20);

			getBehaviour().new BehaviourRequest<Void>(stoppingApplicationMessage) {
				@Override
				protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
					client.stopApplication(cloudModule.getDeployedApplicationName());
					return null;
				}
			}.run(subMonitor.newChild(20));

			server.setModuleState(getModules(), IServer.STATE_STOPPED);
			succeeded = true;

			ServerEventHandler.getDefault().fireServerEvent(
					new ModuleChangeEvent(getBehaviour().getCloudFoundryServer(), CloudServerEvent.EVENT_APP_STOPPED,
							cloudModule.getLocalModule(), Status.OK_STATUS));

			// Update the module
			getBehaviour().updateCloudModuleWithInstances(cloudModule.getDeployedApplicationName(),
					subMonitor.newChild(40));

			getBehaviour().printlnToConsole(cloudModule, Messages.CONSOLE_APP_STOPPED);
			CloudFoundryPlugin.getCallback().stopApplicationConsole(cloudModule, cloudServer);
			subMonitor.worked(20);
		}
		finally {
			if (!succeeded) {
				server.setModuleState(getModules(), IServer.STATE_UNKNOWN);
			}
		}

	}

}