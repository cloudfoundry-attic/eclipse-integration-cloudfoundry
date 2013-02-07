/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.LaunchTunnelCommandManager;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

public class LaunchTunnelCommandAction extends CloudFoundryEditorAction {

	private final String displayName;

	private final ServiceCommand serviceCommand;

	private CaldecottTunnelDescriptor descriptor;

	private final CloudFoundryServer cloudServer;

	private final CloudService cloudService;

	public LaunchTunnelCommandAction(CloudFoundryApplicationsEditorPage editorPage,
			CaldecottTunnelDescriptor descriptor, ServiceCommand command, CloudFoundryServer cloudServer) {
		this(editorPage, null, descriptor, command, cloudServer);
	}

	public LaunchTunnelCommandAction(CloudFoundryApplicationsEditorPage editorPage, CloudService cloudService,
			ServiceCommand command, CloudFoundryServer cloudServer) {
		this(editorPage, cloudService, null, command, cloudServer);
	}

	protected LaunchTunnelCommandAction(CloudFoundryApplicationsEditorPage editorPage, CloudService cloudService,
			CaldecottTunnelDescriptor descriptor, ServiceCommand command, CloudFoundryServer cloudServer) {
		super(editorPage, RefreshArea.ALL);
		setText(command.getDisplayName());
		setImageDescriptor(CloudFoundryImages.TUNNEL_EXTERNAL_TOOLS);
		displayName = command.getDisplayName();
		this.descriptor = descriptor;
		this.serviceCommand = command;
		this.cloudServer = cloudServer;
		this.cloudService = cloudService;
	}

	public String getJobName() {
		return displayName;
	}

	protected Job getJob() {
		Job job = super.getJob();
		job.setUser(true);
		return job;
	}

	public IStatus performAction(IProgressMonitor monitor) throws CoreException {

		// if there is no tunnel descriptor, create the tunnel first
		if (descriptor == null && cloudService != null) {
			try {
				TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
				descriptor = handler.startCaldecottTunnel(cloudService.getName(), monitor, false);
			}
			catch (CoreException e) {
				return CloudFoundryPlugin.getErrorStatus(e);
			}

		}

		if (descriptor != null) {
			// Now check if there are any options that require values,
			// and fill in any tunnel
			// options
			// THis must be wrapped in a UI Job

			UIJob uiJob = new UIJob("Prompting for variable options") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
					final ServiceCommand resolvedCommand = new CommandOptionsUIHandler(shell, serviceCommand,
							descriptor).promptForValues();

					// Once prompted, launch it asynchronously outside the UI
					// thread as it may be a long
					// running process that may block the thread while waiting
					// for the user to exist the external
					// application
					if (resolvedCommand != null) {
						Job job = new Job("Launching external tool.") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								// Finally launch the external tool after
								// options
								// are filled
								// in.
								try {
									new LaunchTunnelCommandManager(resolvedCommand).run(monitor);
								}
								catch (CoreException e) {
									IStatus errorStatus = CloudFoundryPlugin.getErrorStatus(e);
									CloudFoundryPlugin.logError(errorStatus);
									return errorStatus;
								}
								return Status.OK_STATUS;
							}
						};

						// As this may be a long running process, so set it as a
						// system job
						job.setSystem(true);
						job.schedule();
					}

					return Status.OK_STATUS;
				}
			};

			uiJob.schedule();

		}

		return Status.OK_STATUS;

	}

}