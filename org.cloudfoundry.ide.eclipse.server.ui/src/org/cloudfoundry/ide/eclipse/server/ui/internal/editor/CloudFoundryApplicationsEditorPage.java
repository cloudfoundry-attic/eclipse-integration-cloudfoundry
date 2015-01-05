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
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.RefreshApplicationEditorAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.CloudFoundryEditorAction.RefreshArea;
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

	private final List<CloudServerListener> cloudServerListeners = new ArrayList<CloudServerListener>();

	private List<CloudService> services;

	private ScrolledForm sform;

	private int[] applicationMemoryChoices;

	private final int MAX_ERROR_MESSAGE = 100;

	private UIJob refreshJob;

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
		refresh(RefreshArea.MASTER, true);

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

			// ignore EVENT_UPDATE_INSTANCES and DEBUG as refresh will be called
			// separately for these events
			if (event.getType() != CloudServerEvent.EVENT_UPDATE_INSTANCES
					&& event.getType() != CloudServerEvent.EVENT_APP_DEBUG) {
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

			if (refreshJob == null) {
				refreshJob = new UIJob(Messages.CloudFoundryApplicationsEditorPage_JOB_REFRESH) {

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						try {
							if (server != null) {
								CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(
										CloudFoundryServer.class, monitor);
								if (cloudServer != null) {
									setServices(cloudServer.getBehaviour().getServices(monitor));
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
			}

			refreshJob.schedule();

		}
	}

}
