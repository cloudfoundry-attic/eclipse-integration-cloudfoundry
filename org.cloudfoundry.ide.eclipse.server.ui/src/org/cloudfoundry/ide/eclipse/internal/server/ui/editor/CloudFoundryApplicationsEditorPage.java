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

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction.RefreshArea;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.RefreshApplicationEditorAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.wst.server.core.IModule;
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

		createRefreshAction();
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

	/**
	 * Refresh application state with the one on the server. This method can
	 * take a long time. Always run within a job.
	 * @param area
	 * @param module
	 * @throws CoreException
	 */
	public IStatus refreshStates(IModule module, RefreshArea area, IProgressMonitor monitor) throws CoreException {
		// cloudServer is not set up yet, don't refresh
		if (cloudServer == null) {
			return Status.CANCEL_STATUS;
		}
		
		CloudFoundryServerBehaviour serverBehaviour = cloudServer.getBehaviour();
		
		if (area == RefreshArea.MASTER || area == RefreshArea.ALL) {
			// refresh applications
			serverBehaviour.refreshModules(monitor);
	
			// refresh services
			setServices(serverBehaviour.getServices(monitor));
			
			setApplicationMemoryChoices(serverBehaviour.getApplicationMemoryChoices());
		}
		
		if (area == RefreshArea.DETAIL || area == RefreshArea.ALL) {	
			if (module != null) {
				ApplicationModule appModule = cloudServer.getApplication(module);

				try {
					CloudApplication application = serverBehaviour.getApplication(appModule.getApplicationId(), monitor);
					appModule.setCloudApplication(application);
				} catch (CoreException e) {
					// application is not deployed to server yet
				}

				if (appModule.getApplication() != null) {
					// refresh application stats
					ApplicationStats stats = serverBehaviour.getApplicationStats(appModule.getApplicationId(), monitor);
					InstancesInfo info = serverBehaviour.getInstancesInfo(appModule.getApplicationId(), monitor);
					appModule.setApplicationStats(stats);
					appModule.setInstancesInfo(info);
				}
				else {
					appModule.setApplicationStats(null);
				}
			}
		}

		return Status.OK_STATUS;
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
		if (message == null) {
			sform.setMessage(null, IMessageProvider.NONE);
		}
		else {
			sform.setMessage(message, messageType);
		}
	}

	public void setServices(List<CloudService> services) {
		this.services = services;
	}
	
	public void setApplicationMemoryChoices(int[] applicationMemoryChoices) {
		this.applicationMemoryChoices = applicationMemoryChoices;
	}

	private void createRefreshAction() {
		IToolBarManager toolBarManager = sform.getToolBarManager();

		if (toolBarManager.getItems().length > 0) {
			toolBarManager.add(new Separator());
		}

		toolBarManager.add(new RefreshApplicationEditorAction(this));

		toolBarManager.update(true);
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
			
			// ignore EVENT_UPDATE_INSTANCES as refresh will be called after instances are updated
			if (event.getType() != CloudServerEvent.EVENT_UPDATE_INSTANCES) {
				refresh();
			}
		}

		public void serverChanged(ServerEvent event) {
			// refresh when server is saved, e.g. due to add/remove of modules 
			if (event.getKind() == ServerEvent.SERVER_CHANGE) {
				refresh();	
			}
		}

		private void refresh() {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (mform != null && mform.getForm() != null
							&& !mform.getForm().isDisposed()) {
						masterDetailsBlock.refreshUI(RefreshArea.ALL);
					}
				}
			});
		}
	}

}
