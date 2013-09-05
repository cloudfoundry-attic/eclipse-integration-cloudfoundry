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
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Creates a new server instance from a selected space in an organization and
 * spaces viewer in the wizard. It only creates the server instance if another
 * server instance to that space does not exist.
 */
public class OrgsAndSpacesWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CloneSpacePage cloudSpacePage;

	public OrgsAndSpacesWizard(CloudFoundryServer server) {
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {

		cloudSpacePage = new CloneSpacePage(cloudServer);
		cloudSpacePage.setWizard(this);
		addPage(cloudSpacePage);
	}

	@Override
	public boolean performFinish() {

		final CloudSpace selectedSpace = cloudSpacePage.getSelectedCloudSpace();

		// Only create a new space, if it doesnt match the existing space
		if (selectedSpace != null
				&& !CloudSpaceChangeHandler.matchesExisting(selectedSpace, cloudServer.getCloudFoundrySpace())) {

			String serverName = cloudSpacePage.getServerName();
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

	static class CloneSpacePage extends CloudFoundryCloudSpaceWizardpage {

		private String serverName;

		private Text serverNameText;

		private IStatus status;

		private CloudSpace selectedSpace;

		CloneSpacePage(CloudFoundryServer server) {
			super(server, null);

			spaceChangeHandler = new CloudSpaceChangeHandler(cloudServer) {

				@Override
				public void setSelectedSpace(CloudSpace selectedCloudSpace) {

					CloneSpacePage.this.selectedSpace = selectedCloudSpace;

					CloneSpacePage.this.setServerNameInUI(selectedCloudSpace != null ? selectedCloudSpace.getName()
							: null);

				}
			};
		}

		public String getServerName() {
			return serverName;
		}

		@Override
		public boolean isPageComplete() {
			boolean isComplete = super.isPageComplete();
			if (isComplete) {
				isComplete = getSelectedCloudSpace() != null && (status == null || status.isOK());
			}

			return isComplete;
		}

		@Override
		public void createControl(Composite parent) {
			Composite mainComposite = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(mainComposite);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(mainComposite);

			Composite serverNameComp = new Composite(mainComposite, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(serverNameComp);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(serverNameComp);
			Label label = new Label(serverNameComp, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(label);
			label.setText("Server Name:");

			serverNameText = new Text(serverNameComp, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(serverNameText);

			serverNameText.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent arg0) {

					serverName = serverNameText.getText();
					refresh();

				}
			});
			spacesPart = new CloudSpacesSelectionPart(spaceChangeHandler, cloudServer, this);
			spacesPart.createComposite(mainComposite);

			// Make sure the description is set after the part is created, to
			// avoid using the default parent description.
			setDescription("Please select a space and enter a server name to create a new server instance to that space.");

			setControl(mainComposite);
		}

		public CloudSpace getSelectedCloudSpace() {
			return selectedSpace;
		}

		protected void refresh() {
			validate();
			if (status == null || status.isOK()) {
				setErrorMessage(null);
			}
			else {
				setErrorMessage(status.getMessage());
			}
			getWizard().getContainer().updateButtons();
		}

		protected void validate() {
			status = Status.OK_STATUS;

			if (serverName == null || serverName.trim().length() == 0) {
				status = CloudFoundryPlugin.getErrorStatus("Please enter a valid server name.");
			}
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible) {
				// Launch it as a job, to give the wizard time to display
				// the spaces viewer
				UIJob job = new UIJob("Refreshing list of organization and spaces") {

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						updateSpacesDescriptor();
						refreshListOfSpaces();
						CloudSpace defaultSpace = spaceChangeHandler.getCurrentSpacesDescriptor() != null ? spaceChangeHandler
								.getCurrentSpacesDescriptor().getOrgsAndSpaces().getDefaultCloudSpace()
								: null;

						if (defaultSpace != null) {
							setServerNameInUI(defaultSpace.getName());
						}

						return Status.OK_STATUS;
					}

				};
				job.setSystem(true);
				job.schedule();

			}
		}

		protected void setServerNameInUI(String name) {
			if (serverNameText != null && name != null) {
				serverName = name;
				serverNameText.setText(name);
			}
			refresh();
		}

		protected IStatus updateSpacesDescriptor() {
			if (spaceChangeHandler != null) {
				String url = cloudServer.getUrl();
				String userName = cloudServer.getUsername();
				String password = cloudServer.getPassword();
				try {
					spaceChangeHandler.getUpdatedDescriptor(url, userName, password, getWizard().getContainer());
					return Status.OK_STATUS;
				}
				catch (CoreException e) {
					return e.getStatus();
				}
			}
			return CloudFoundryPlugin
					.getErrorStatus("No handler to resolve organizations and spaces for the given server was found.");
		}
	}

}
