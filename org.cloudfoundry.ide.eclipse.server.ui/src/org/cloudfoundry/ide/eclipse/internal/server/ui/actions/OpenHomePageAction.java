/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.net.URL;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.ui.IServerModule;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Steffen Pingel
 */
public class OpenHomePageAction implements IObjectActionDelegate {

	private IModule selectedModule;

	private IServer selectedServer;
	
	private URL homePageUrl;

	public void run(IAction action) {
		CloudUiUtil.openUrl(homePageUrl.toExternalForm());
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
			CloudFoundryApplicationModule cloudModule = getSelectedCloudAppModule();
			if (cloudModule != null) {
				int state = cloudModule.getState();
				if (state == IServer.STATE_STARTED) {
					IURLProvider cloudServer = (IURLProvider)selectedServer
							.loadAdapter(IURLProvider.class, null);
					homePageUrl = cloudServer.getModuleRootURL(selectedModule);
					if (homePageUrl != null) {
						action.setEnabled(true);
						return;
					}
				}
			}
		}
		action.setEnabled(false);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	private CloudFoundryApplicationModule getSelectedCloudAppModule() {
		CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer
				.loadAdapter(CloudFoundryServer.class, null);
		return cloudServer.getExistingCloudModule(selectedModule);
	}
}
