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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.AddServicesToApplicationAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;

public class AddServiceStartCaldecottAction extends AddServicesToApplicationAction {

	private final String jobName;

	public AddServiceStartCaldecottAction(List<String> services, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage, String jobName) {
		// Null application module, as it is resolved only at action run time
		super(services, null, serverBehaviour, editorPage, RefreshArea.ALL);
		this.jobName = jobName;
		setText(jobName);
		setImageDescriptor(CloudFoundryImages.CONNECT);
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

		IModule caldecottApp = handler.getCaldecottModule(monitor);
		if (caldecottApp instanceof ApplicationModule) {
			ApplicationModule caldecottModule = (ApplicationModule) caldecottApp;
			// Application Module MUST be set first before invoking parent
			// action, as the latter
			// requires a valid application module
			setApplicationModule(caldecottModule);

			List<String> servicesToAdd = getServicesToAdd();

			if (servicesToAdd != null && !servicesToAdd.isEmpty()) {

				try {
					super.performAction(monitor);

					handler.startCaldecottTunnel(servicesToAdd.get(0), monitor);
				}
				catch (CoreException e) {
					return CloudFoundryPlugin.getErrorStatus(e);
				}

			}
			return Status.OK_STATUS;
		}
		else {
			return CloudFoundryPlugin
					.getErrorStatus("Unable to resolve Caldecott application module. Failed to open Caldecott tunnel.");
		}

	}

}
