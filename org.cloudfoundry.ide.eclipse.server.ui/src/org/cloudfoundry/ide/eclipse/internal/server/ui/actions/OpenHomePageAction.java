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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Steffen Pingel
 */
public class OpenHomePageAction implements IObjectActionDelegate {

	private IModule selectedModule;

	private IServer selectedServer;

	public void run(IAction action) {
		ApplicationModule cloudApp = getSelectedCloudAppModule();
		open(cloudApp);
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		selectedServer = null;
		selectedModule = null;
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof IServer) {
					selectedServer = (IServer) obj;
				}
				else if (obj instanceof IServerModule) {
					IServerModule sm = (IServerModule) obj;
					IModule[] module = sm.getModule();
					selectedModule = module[module.length - 1];
					if (selectedModule != null) {
						selectedServer = sm.getServer();
					}
				}
			}
		}

		if (selectedServer != null && (selectedServer.getServerState() == IServer.STATE_STARTED)) {
			ApplicationModule cloudModule = getSelectedCloudAppModule();
			if (cloudModule != null) {
				int state = cloudModule.getState();
				if (state == IServer.STATE_STARTED) {
					action.setEnabled(true);
					return;
				}
			}
		}
		action.setEnabled(false);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	private ApplicationModule getSelectedCloudAppModule() {
		CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer
				.loadAdapter(CloudFoundryServer.class, null);
		return cloudServer.getApplication(selectedModule);
	}

	public static boolean open(ApplicationModule cloudApp) {
		// verify that URIs are set, as it may be a standalone application with
		// no URI
		List<String> uris = cloudApp.getApplication().getUris();
		if (uris != null && !uris.isEmpty()) {
			CloudUiUtil.openUrl("http://" + uris.get(0));
		}
		return true;
	}
}
