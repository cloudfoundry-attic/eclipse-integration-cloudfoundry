/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.BehaviourEventType;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.server.core.IModule;

/**
 * Callback interface to support clients to hook into CloudFoundry Server
 * processes.
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public abstract class CloudFoundryCallback {

	public void printToConsole(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule, String message,
			boolean clearConsole, boolean isError, IProgressMonitor monitor) {
		// optional
	}

	public abstract void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	/**
	 * Show deployed application's  Cloud Foundry log files locally.
	 * @param cloudServer
	 * @param cloudModule
	 * @param showIndex if -1 shows the first app instance
	 */
	public  void showCloudFoundryLogs(CloudFoundryServer cloudServer, 
			CloudFoundryApplicationModule cloudModule, int showIndex) {
		
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
	 * Prepares an application to either be deployed, started or restarted.
	 * The main purpose to ensure that the application's deployment
	 * information is complete. If incomplete, it will prompt the user for
	 * missing information.
	 * @param monitor
	 * @return Cloud Foundry application mapped to the deployed WST
	 * {@link IModule}. Must not be null. If null, it indicates error,
	 * therefore throw {@link CoreException} instead.
	 * @throws CoreException if failure while preparing the application for deployment
	 * @throws OperationCanceledException if the user cancelled deploying or
	 * starting the application. The application's deployment information
	 * should not be modified in this case.
	 */
	public abstract void prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) throws CoreException, OperationCanceledException;

	public abstract void deleteServices(List<String> services, CloudFoundryServer cloudServer);

	public abstract void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer);

	public boolean isAutoDeployEnabled() {
		return true;
	}

	public void handleError(IStatus status, BehaviourEventType eventType) {

	}

}
