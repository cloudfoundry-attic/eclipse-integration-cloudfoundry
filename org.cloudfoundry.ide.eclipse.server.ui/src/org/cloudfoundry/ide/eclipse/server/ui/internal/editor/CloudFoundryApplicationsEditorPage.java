/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.EditorAction.EditorCloudEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.EditorAction.RefreshArea;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.RefreshEditorAction;
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
import org.eclipse.ui.statushandlers.StatusManager;
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

	private final List<CloudServerListener> cloudServerListeners = new ArrayList<CloudServerListener>();

	private List<CloudService> services;

	private ScrolledForm sform;

	private int[] applicationMemoryChoices;

	private final int MAX_ERROR_MESSAGE = 100;

	private UIJob refreshJob;

	private RefreshEditorOperation currentRefreshOp;

	@Override
	public void createPartControl(Composite parent) {

		mform = new ManagedForm(parent);
		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		sform = mform.getForm();
		sform.getForm().setText(Messages.COMMONTXT_APPLICATIONS);
		toolkit.decorateFormHeading(sform.getForm());

		cloudServer = (CloudFoundryServer) getServer().getOriginal().loadAdapter(CloudFoundryServer.class, null);

		masterDetailsBlock = new ApplicationMasterDetailsBlock(this, cloudServer);
		masterDetailsBlock.createContent(mform);

		sform.getForm().setImage(CloudFoundryImages.getImage(CloudFoundryImages.OBJ_APPLICATION));
		refresh(RefreshArea.MASTER);

		serverListener = new ServerListener();
		addCloudServerListener(serverListener);
		getServer().getOriginal().addServerListener(serverListener);
	}

	/**
	 * 
	 * @param listener to be notified of Cloud server and application changes.
	 * The editor manages the lifecycle of the listener once it is added,
	 * including removing it when the editor is disposed.
	 */
	public void addCloudServerListener(CloudServerListener listener) {
		if (listener != null && !cloudServerListeners.contains(listener)) {
			ServerEventHandler.getDefault().addServerListener(listener);
			cloudServerListeners.add(listener);
		}
	}

	@Override
	public void dispose() {
		for (CloudServerListener listener : cloudServerListeners) {
			ServerEventHandler.getDefault().removeServerListener(listener);
		}

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

	public void refresh(RefreshArea area) {
		RefreshEditorAction.getRefreshAction(this, area).run();
	}

	public void selectAndReveal(IModule module) {
		// Refresh the UI immediately with the local information for the module
		masterDetailsBlock.refreshUI(RefreshArea.MASTER);
		TableViewer viewer = masterDetailsBlock.getMasterPart().getApplicationsViewer();
		viewer.setSelection(new StructuredSelection(module));

		// Launch a fresh operation that will update the module. As this is
		// longer
		// running, it will eventually refresh the UI via events
		refresh(RefreshArea.DETAIL);
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
					buffer.replace(i, i + 1, " "); //$NON-NLS-1$
				}
			}

			if (buffer.length() > MAX_ERROR_MESSAGE) {
				String endingSegment = Messages.CloudFoundryApplicationsEditorPage_TEXT_SEE_ERRORLOG;

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

	private synchronized void setRefreshOp(RefreshEditorOperation op) {
		this.currentRefreshOp = op;
	}

	private synchronized RefreshEditorOperation getRefreshOp() {
		return this.currentRefreshOp;
	}

	private class ServerListener implements CloudServerListener, IServerListener {
		public void serverChanged(final CloudServerEvent event) {

			if (event.getServer() == null) {
				CloudFoundryPlugin
						.logError("Internal error: unable to refresh editor. No Cloud server specified in the server event."); // $NON-NLS-1$
				return;
			}
			// Do not refresh if not from the same server
			if (!cloudServer.getServer().getId().equals(event.getServer().getServer().getId())) {
				return;
			}

			// Don't trigger editor refresh on instances update to avoid
			// multiple
			// refreshes as it is performed
			// as part of an application update operation which triggers
			// a separate module refresh event
			if (event.getType() != CloudServerEvent.EVENT_INSTANCES_UPDATED) {
				RefreshArea area = event instanceof EditorCloudEvent ? ((EditorCloudEvent) event).getRefreshArea()
						: RefreshArea.ALL;
				refresh(cloudServer.getServer(), event.getStatus(), event.getType(), area);
			}
		}

		public void serverChanged(ServerEvent event) {
			// refresh when server is saved, e.g. due to add/remove of modules
			if (event.getKind() == ServerEvent.SERVER_CHANGE) {
				refresh(event.getServer(), event.getStatus(), CloudServerEvent.EVENT_SERVER_REFRESHED, RefreshArea.ALL);
			}
		}

		private void refresh(final IServer server, IStatus status, final int refreshType, final RefreshArea area) {
			launchRefresh(new RefreshEditorOperation(server, status, refreshType, area));
		}
	}

	protected void launchRefresh(RefreshEditorOperation refreshOp) {

		setRefreshOp(refreshOp);

		// Only schedule one job per editor page session, in case multiple
		// refresh requests are received, only the one that is currently
		// scheduled should execute
		if (refreshJob == null) {
			refreshJob = new UIJob(Messages.CloudFoundryApplicationsEditorPage_JOB_REFRESH) {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {

					RefreshEditorOperation op = getRefreshOp();
					if (op != null) {
						op.run(monitor);
					}
					return Status.OK_STATUS;
				}
			};
		}

		refreshJob.schedule();
	}

	protected void setErrorInPage(String message) {
		if (message == null) {
			setMessage(null, IMessageProvider.NONE);
		}
		else {
			setMessage(message, IMessageProvider.ERROR);
		}
	}

	protected void setMessageInPage(IStatus status) {
		String message = status.getMessage();
		int providerStatus = IMessageProvider.NONE;
		switch (status.getSeverity()) {
		case IStatus.INFO:
			providerStatus = IMessageProvider.INFORMATION;
			break;
		case IStatus.WARNING:
			providerStatus = IMessageProvider.WARNING;
			break;
		}

		setMessage(message, providerStatus);
	}

	/**
	 * Refresh operation that should only be run in UI thread.
	 *
	 */
	private class RefreshEditorOperation {

		private final IServer server;

		private final IStatus status;

		private final int refreshType;

		private final RefreshArea area;

		public RefreshEditorOperation(final IServer server, IStatus status, final int refreshType,
				final RefreshArea area) {
			this.server = server;
			this.status = status != null ? status : Status.OK_STATUS;
			this.refreshType = refreshType;
			this.area = area;
		}

		public void run(IProgressMonitor monitor) {

			if (isDisposed() || mform == null || mform.getForm() == null || mform.getForm().isDisposed()
					|| masterDetailsBlock.getMasterPart().getManagedForm().getForm().isDisposed()) {
				return;
			}

			Throwable error = status.getException();
			try {
				if ((refreshType == CloudServerEvent.EVENT_UPDATE_SERVICES || refreshType == CloudServerEvent.EVENT_SERVER_REFRESHED)
						&& server != null) {
					CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class,
							monitor);
					if (cloudServer != null) {
						setServices(cloudServer.getBehaviour().getServices(monitor));
					}
				}
			}
			catch (CoreException e) {
				error = e;
			}

			// Refresh the UI before handing any errors
			masterDetailsBlock.refreshUI(area);

			// Process errors
			if (status.getSeverity() == IStatus.WARNING || status.getSeverity() == IStatus.INFO) {
				setMessageInPage(status);
			}
			else if (error != null) {
				StatusManager.getManager().handle(status, StatusManager.LOG);
				setErrorInPage(status.getMessage());
			}
			else {
				IModule currentModule = getMasterDetailsBlock().getCurrentModule();

				// If no error is found, be sure to set null for the
				// message to
				// clear any error messages
				String errorMessage = null;
				if (currentModule != null) {
					CloudFoundryApplicationModule appModule = getCloudServer().getExistingCloudModule(currentModule);
					if (appModule != null && appModule.getErrorMessage() != null) {
						errorMessage = appModule.getErrorMessage();
					}
				}
				setErrorInPage(errorMessage);
			}
		}
	}

}
