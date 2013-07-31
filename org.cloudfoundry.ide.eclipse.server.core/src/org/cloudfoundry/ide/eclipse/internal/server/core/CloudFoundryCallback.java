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

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Callback interface to support clients to hook into CloudFoundry Server
 * processes.
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public abstract class CloudFoundryCallback {

	public abstract void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStopping(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStopped(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer);

	public abstract void disconnecting(CloudFoundryServer server);

	public abstract void getCredentials(CloudFoundryServer server);

	public abstract void displayCaldecottTunnelConnections(CloudFoundryServer server,
			List<CaldecottTunnelDescriptor> descriptors);

	public abstract DeploymentDescriptor prepareForDeployment(CloudFoundryServer server,
			CloudFoundryApplicationModule module, IProgressMonitor monitor);

	public static class DeploymentDescriptor {

		public ApplicationInfo applicationInfo;

		public DeploymentInfo deploymentInfo;

		public ApplicationAction deploymentMode;

		public ApplicationArchive applicationArchive;

		public boolean isIncrementalPublish;

		public Staging staging;

		public ApplicationPlan applicationPlan;

	}

	public abstract void deleteServices(List<String> services, CloudFoundryServer cloudServer);

	public abstract void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer);

	public boolean isAutoDeployEnabled() {
		return true;
	}

}
