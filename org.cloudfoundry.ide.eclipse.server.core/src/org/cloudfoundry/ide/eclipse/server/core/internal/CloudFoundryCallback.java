/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.BehaviourEventType;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Callback interface to support clients to hook into CloudFoundry Server
 * processes.
 * 
 * <p/>
 * INTERNAL API: Adopters should not extend or use as API may change.
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public abstract class CloudFoundryCallback {

	public void printToConsole(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule, String message,
			boolean clearConsole, boolean isError) {
		// optional
	}

	public void trace(CloudLog log, boolean clear) {
		// optional
	}

	public void showTraceView(boolean showTrace) {
		// optional
	}

	public void startApplicationConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {

	}

	public abstract void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	/**
	 * Show deployed application's Cloud Foundry log files locally.
	 * @param cloudServer
	 * @param cloudModule
	 * @param showIndex if -1 shows the first app instance
	 */
	public void showCloudFoundryLogs(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {

	}

	/**
	 * Stops all consoles for the given application for all application
	 * instances.
	 * @param cloudModule
	 * @param cloudServer
	 */
	public abstract void stopApplicationConsole(CloudFoundryApplicationModule cloudModule,
			CloudFoundryServer cloudServer);

	public abstract void disconnecting(CloudFoundryServer server);

	public abstract void getCredentials(CloudFoundryServer server);

	public abstract void displayCaldecottTunnelConnections(CloudFoundryServer server,
			List<CaldecottTunnelDescriptor> descriptors);

	/**
	 * Prepares an application to either be deployed, started or restarted. The
	 * main purpose to ensure that the application's deployment information is
	 * complete. If incomplete, it will prompt the user for missing information.
	 * @param monitor
	 * @return {@link DeploymentConfiguration} Defines local deployment
	 * configuration of the application, for example which deployment mode
	 * should be used like starting an application, restarting, etc..May be
	 * null. If null, the framework will attempt to determine an appropriate
	 * deployment configuration.
	 * @throws CoreException if failure while preparing the application for
	 * deployment
	 * @throws OperationCanceledException if the user cancelled deploying or
	 * starting the application. The application's deployment information should
	 * not be modified in this case.
	 */
	public abstract DeploymentConfiguration prepareForDeployment(CloudFoundryServer server,
			CloudFoundryApplicationModule module, IProgressMonitor monitor) throws CoreException,
			OperationCanceledException;

	public abstract void deleteServices(List<String> services, CloudFoundryServer cloudServer);

	public abstract void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer);

	public boolean isAutoDeployEnabled() {
		return true;
	}

	public void handleError(IStatus status, BehaviourEventType eventType) {

	}

	/**
	 * Shows a confirmation dialog with the given operation title and message, and
	 * asks the user the determine whether to execute the target operation.
	 * 
	 * @param title the title of the target operation
	 * @param message the message presented to the user
	 * @return true if the user chooses to execute the target operation, false otherwise
	 */
	public abstract boolean confirmTheOperation(final String title, final String message);
}
