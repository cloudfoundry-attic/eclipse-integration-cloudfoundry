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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class AddServiceStartCaldecottAction extends CloudFoundryEditorAction {

	private final String jobName;

	private List<String> services;

	public AddServiceStartCaldecottAction(List<String> services, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage, String jobName) {
		// Null application module, as it is resolved only at action run time
		super(editorPage, RefreshArea.ALL);
		this.jobName = jobName;
		setText(jobName);
		setImageDescriptor(CloudFoundryImages.CONNECT);
		this.services = new ArrayList<String>(services);
	}

	@Override
	public String getJobName() {
		return jobName;
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	@Override
	protected Job getJob() {
		Job job = super.getJob();
		// As starting a Caldecott tunnel may take time, show progress dialog
		// that also
		// allows the user to run it as a background job.
		job.setUser(true);
		return job;
	}

	@Override
	public IStatus performAction(IProgressMonitor monitor) throws CoreException {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(getBehavior().getCloudFoundryServer());

		if (services != null && !services.isEmpty()) {

			try {
				handler.startCaldecottTunnel(services.get(0), monitor);
			}
			catch (CoreException e) {
				return CloudFoundryPlugin.getErrorStatus(e);
			}

		}
		return Status.OK_STATUS;

	}

}
