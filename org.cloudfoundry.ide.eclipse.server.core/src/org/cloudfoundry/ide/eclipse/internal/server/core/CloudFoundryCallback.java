/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.BehaviourEvent;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.BehaviourListener;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Callback interface to support clients to hook into CloudFoundry Server
 * processes.
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public abstract class CloudFoundryCallback implements BehaviourListener {

	// FIXNS: event-driven handler still in development as of CF 1.6.0
	public <T> void handle(BehaviourEvent<T> event) {
		if (event == null || event.getType() == null || event.getServer() == null) {
			String message = null;
			if (event == null) {
				message = "Null event.";
			}
			else if (event.getType() == null) {
				message = "No event type specified.";
			}
			else if (event.getServer() == null) {
				message = "No server specified.";
			}
			CloudFoundryPlugin.logError("Unable to handle server behaviour event due to: " + message);
			return;
		}
		CloudFoundryApplicationModule cloudModule = event.getApplicationModule();
		CloudFoundryServer server = event.getServer();
		T resultObj = event.getResult();
		switch (event.getType()) {
		case APP_PRE_START:
			applicationAboutToStart(server, cloudModule);
			break;
		case APP_STARTING:
			applicationStarting(server, cloudModule);

			break;
		case APP_STARTED:
			applicationStarted(server, cloudModule);
			break;
		case APP_STOPPED:
			stopApplicationConsole(cloudModule, server);
			break;
		case APP_DELETE:
			deleteApplication(cloudModule, server);
			break;

		case DISCONNECT:
			disconnecting(server);
			break;
		case PROMPT_CREDENTIALS:
			getCredentials(server);
			break;
		case REFRESH_TUNNEL_CONNECTIONS:
			if (resultObj instanceof List<?>) {
				displayCaldecottTunnelConnections(server, (List<CaldecottTunnelDescriptor>) resultObj);
			}
			break;
		case SERVICES_DELETED:
			if (resultObj instanceof List<?>) {

				deleteServices((List<String>) resultObj, server);
			}
			break;
		}
	}

	public abstract void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	/**
	 * Starts application instances console (log files shown in console), and shows the specified console in the Eclipse console.
	 * @param cloudServer
	 * @param cloudModule
	 * @param showIndex if -1 shows the first app instance
	 */
	public abstract void startApplicationConsole(CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule cloudModule, int showIndex);

	
	/**
	 * Stops all consoles for the given application for all application instances.
	 * @param cloudModule
	 * @param cloudServer
	 */
	public abstract void stopApplicationConsole(CloudFoundryApplicationModule cloudModule,
			CloudFoundryServer cloudServer);

	public abstract void disconnecting(CloudFoundryServer server);

	public abstract void getCredentials(CloudFoundryServer server);

	public abstract void displayCaldecottTunnelConnections(CloudFoundryServer server,
			List<CaldecottTunnelDescriptor> descriptors);

	public abstract void prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) throws CoreException, OperationCanceledException;

	public void applicationAboutToStart(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {

	}

	public abstract void deleteServices(List<String> services, CloudFoundryServer cloudServer);

	public abstract void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer);

	public boolean isAutoDeployEnabled() {
		return true;
	}

}
