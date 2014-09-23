/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.LaunchTunnelCommandManager;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.CloudFoundryEditorAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
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

		/**
		 * FIXNS: Disabled for CF 1.5.0 until tunnel support at client level are
		 * updated.
		 */
		setEnabled(false);
		setToolTipText(TunnelActionProvider.DISABLED_V2_TOOLTIP_MESSAGE);
	}

	public String getJobName() {
		return displayName;
	}

	protected Job getJob() {
		Job job = super.getJob();
		job.setUser(true);
		return job;
	}

	protected IStatus launch(IProgressMonitor monitor) {
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

			UIJob uiJob = new UIJob(Messages.LaunchTunnelCommandAction_JOB_PROMPT) {

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
						Job job = new Job(Messages.LaunchTunnelCommandAction_JOB_LAUNCH) {

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
									CloudFoundryPlugin.log(errorStatus);
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

	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ModifyEditorOperation() {

			@Override
			protected void performOperation(IProgressMonitor monitor) throws CoreException {
				IStatus status = launch(monitor);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
			}
		};
	}
}