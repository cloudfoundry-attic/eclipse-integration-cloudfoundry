/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerHandler;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerHandlerCallback;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceChangeHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Creates a new server instance from a selected space in an organization and
 * spaces viewer in the wizard. It only creates the server instance if another
 * server instance to that space does not exist.
 */
public class OrgsAndSpacesWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CloudSpace selectedSpace;

	private CloudFoundryCloudSpaceWizardpage cloudSpacePage;

	private CloudSpaceChangeHandler handler;

	public OrgsAndSpacesWizard(CloudFoundryServer server) {
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);

	}

	@Override
	public void addPages() {
		handler = new CloudSpaceChangeHandler(cloudServer) {

			@Override
			public void setSelectedSpace(CloudSpace selectedCloudSpace) {

				OrgsAndSpacesWizard.this.selectedSpace = selectedCloudSpace;

			}
		};

		cloudSpacePage = new CloudFoundryCloudSpaceWizardpage(cloudServer, handler) {

			@Override
			public void setVisible(boolean visible) {
				super.setVisible(visible);
				if (visible) {
					// Launch it as a job, to give the wizard time to display
					// the spaces viewer
					UIJob job = new UIJob("Refreshing list of organization and spaces") {

						@Override
						public IStatus runInUIThread(IProgressMonitor arg0) {
							updateSpacesDescriptor();
							cloudSpacePage.refreshListOfSpaces();
							return Status.OK_STATUS;
						}

					};
					job.setSystem(true);
					job.schedule();

				}
			}

		};
		cloudSpacePage.setWizard(this);
		addPage(cloudSpacePage);
	}

	protected IStatus updateSpacesDescriptor() {
		if (handler != null) {
			String url = cloudServer.getUrl();
			String userName = cloudServer.getUsername();
			String password = cloudServer.getPassword();
			try {
				handler.getUpdatedDescriptor(url, userName, password, OrgsAndSpacesWizard.this.getContainer());
				return Status.OK_STATUS;
			}
			catch (CoreException e) {
				return e.getStatus();
			}
		}
		return CloudFoundryPlugin
				.getErrorStatus("No handler to resolve organizations and spaces for the given server was found.");
	}

	@Override
	public boolean performFinish() {

		// Only create a new space, if it doesnt match the existing space
		if (selectedSpace != null
				&& !CloudSpaceChangeHandler.matchesExisting(selectedSpace, cloudServer.getCloudFoundrySpace())) {

			String serverName = selectedSpace.getName();
			final ServerDescriptor descriptor = ServerDescriptor.getServerDescriptor(cloudServer, serverName);

			if (descriptor == null) {
				CloudFoundryPlugin.logError("No cloud server descriptor found that matches : "
						+ cloudServer.getServerId() + ". Unable to create a server instance to another space.");
				return false;
			}
			else {
				final String password = cloudServer.getPassword();
				final String userName = cloudServer.getUsername();
				final String url = cloudServer.getUrl();

				CloudUiUtil.runForked(new ICoreRunnable() {
					public void run(final IProgressMonitor monitor) throws CoreException {

						ServerHandler serverHandler = new ServerHandler(descriptor);

						serverHandler.createServer(monitor, ServerHandler.NEVER_OVERWRITE, new ServerHandlerCallback() {

							@Override
							public void configureServer(IServerWorkingCopy wc) throws CoreException {
								CloudFoundryServer cloudServer = (CloudFoundryServer) wc.loadAdapter(
										CloudFoundryServer.class, null);

								if (cloudServer != null) {
									cloudServer.setPassword(password);
									cloudServer.setUsername(userName);
									cloudServer.setUrl(url);
									cloudServer.setSpace(selectedSpace);
									cloudServer.saveConfiguration(monitor);
								}

							}
						});

					}
				}, this);
			}

		}

		return true;
	}

}
