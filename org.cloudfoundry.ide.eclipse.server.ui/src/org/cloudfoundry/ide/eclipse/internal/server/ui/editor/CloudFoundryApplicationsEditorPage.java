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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction.RefreshArea;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.RefreshApplicationEditorAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;

/**
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryApplicationsEditorPage extends ServerEditorPart {

	private CloudFoundryServer cloudServer;

	private ApplicationMasterDetailsBlock masterDetailsBlock;

	private ManagedForm mform;

	private ServerListener serverListener;

	private List<CloudService> services;

	private ScrolledForm sform;

	private int[] applicationMemoryChoices;

	private final int MAX_ERROR_MESSAGE = 100;

	@Override
	public void createPartControl(Composite parent) {
		mform = new ManagedForm(parent);
		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		sform = mform.getForm();
		sform.getForm().setText("Applications");
		toolkit.decorateFormHeading(sform.getForm());

		cloudServer = (CloudFoundryServer) getServer().getOriginal().loadAdapter(CloudFoundryServer.class, null);

		masterDetailsBlock = new ApplicationMasterDetailsBlock(this, cloudServer);
		masterDetailsBlock.createContent(mform);

		sform.getForm().setImage(CloudFoundryImages.getImage(CloudFoundryImages.OBJ_APPLICATION));
		refresh(RefreshArea.MASTER, true);

		serverListener = new ServerListener();
		CloudFoundryPlugin.getDefault().addServerListener(serverListener);
		getServer().getOriginal().addServerListener(serverListener);
	}

	@Override
	public void dispose() {
		CloudFoundryPlugin.getDefault().removeServerListener(serverListener);
		getServer().getOriginal().removeServerListener(serverListener);

		if (mform != null) {
			mform.dispose();
			mform = null;
		}
		super.dispose();
	};

	public CloudFoundryServer getCloudServer() {
		return cloudServer;
	}

	public ApplicationMasterDetailsBlock getMasterDetailsBlock() {
		return masterDetailsBlock;
	}

	public List<CloudService> getServices() {
		return services;
	}

	public int[] getApplicationMemoryChoices() {
		return applicationMemoryChoices;
	}

	public boolean isDisposed() {
		return sform.isDisposed();
	}

	public void reflow() {
		mform.getForm().reflow(true);
	}

	public void refresh(RefreshArea area, boolean userAction) {
		RefreshApplicationEditorAction action = new RefreshApplicationEditorAction(this, area);
		action.setUserAction(userAction);
		action.run();
	}

	public void selectAndReveal(IModule module) {
		masterDetailsBlock.refreshUI(RefreshArea.MASTER);
		TableViewer viewer = masterDetailsBlock.getMasterPart().getApplicationsViewer();
		viewer.setSelection(new StructuredSelection(module));
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	public void setMessage(String message, int messageType) {
		String messageToDisplay = message;
		if (messageToDisplay == null) {
			sform.setMessage(null, IMessageProvider.NONE);
		}
		else {
			// First replace all return carriages, or new lines with spaces
			StringBuffer buffer = new StringBuffer(messageToDisplay);
			for (int i = 0; i < buffer.length(); i++) {
				char ch = buffer.charAt(i);
				if (ch == '\r' || ch == '\n') {
					buffer.replace(i, i + 1, " ");
				}
			}

			if (buffer.length() > MAX_ERROR_MESSAGE) {
				String endingSegment = "... (see error log)";

				messageToDisplay = buffer.substring(0, MAX_ERROR_MESSAGE).trim() + endingSegment;
				CloudFoundryPlugin.logError(message);
			}
			else {
				messageToDisplay = buffer.toString();
			}

			sform.setMessage(messageToDisplay, messageType);
		}
	}

	public void setServices(List<CloudService> services) {
		this.services = services;
	}

	public void setApplicationMemoryChoices(int[] applicationMemoryChoices) {
		this.applicationMemoryChoices = applicationMemoryChoices;
	}

	private class ServerListener implements CloudServerListener, IServerListener {
		public void serverChanged(final CloudServerEvent event) {
			if (event.getType() == CloudServerEvent.EVENT_UPDATE_SERVICES) {
				// refresh services
				try {
					setServices(cloudServer.getBehaviour().getServices(null));
				}
				catch (CoreException e) {
					// FIXME: error handling
				}
			}

			// ignore EVENT_UPDATE_INSTANCES as refresh will be called after
			// instances are updated
			if (event.getType() != CloudServerEvent.EVENT_UPDATE_INSTANCES) {
				refresh(cloudServer.getServer());
			}
		}

		public void serverChanged(ServerEvent event) {
			// refresh when server is saved, e.g. due to add/remove of modules
			if (event.getKind() == ServerEvent.SERVER_CHANGE) {
				refresh(event.getServer());
			}
		}

		private void refresh(final IServer server) {

			UIJob job = new UIJob("Refreshing editor") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						if (server != null) {
							CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(
									CloudFoundryServer.class, monitor);
							if (cloudServer != null) {
								setServices(cloudServer.getBehaviour().getServices(monitor));

								setApplicationMemoryChoices(cloudServer.getBehaviour().getApplicationMemoryChoices(
										monitor));
							}
						}

						if (mform != null && mform.getForm() != null && !mform.getForm().isDisposed()) {
							masterDetailsBlock.refreshUI(RefreshArea.ALL);
						}
					}
					catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};

			job.schedule();

		}
	}

}
