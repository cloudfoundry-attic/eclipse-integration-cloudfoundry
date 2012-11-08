/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

public class CaldecottDisconnectAllAction extends Action {

	protected final CloudFoundryServer cloudServer;

	public CaldecottDisconnectAllAction(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		setActionValues();
	}

	protected void setActionValues() {
		setText("Disconnect All Tunnels");
		setImageDescriptor(CloudFoundryImages.DISCONNECT);
		setToolTipText("Disconnect All Tunnels");
		setEnabled(true);
	}

	public void run() {

		Job job = new Job("Stopping all tunnels for: " + cloudServer.getDeploymentName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					new TunnelBehaviour(cloudServer).stopAndDeleteAllTunnels(monitor);
				}
				catch (CoreException e) {
					return CloudFoundryPlugin.getErrorStatus(e);
				}
				return Status.OK_STATUS;
			}

		};
		job.setSystem(false);
		job.schedule();
	}
}
