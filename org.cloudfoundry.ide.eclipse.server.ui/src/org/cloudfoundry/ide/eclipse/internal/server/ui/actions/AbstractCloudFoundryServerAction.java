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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;


public abstract class AbstractCloudFoundryServerAction implements IObjectActionDelegate {

	protected IModule selectedModule;

	protected IServer selectedServer;

	public void selectionChanged(IAction action, ISelection selection) {
		selectedServer = getSelectedServer(selection);
		serverSelectionChanged(action);
	}

	/**
	 * Subclasses can override if they want to perform some behaviour on a valid
	 * server selection, like enabling/disabling the action.
	 * @param action
	 */
	protected void serverSelectionChanged(IAction action) {
		// Do nothing
	}

	protected IServer getSelectedServer(ISelection selection) {
		IServer server = null;
		selectedModule = null;
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof IServer) {
					server = (IServer) obj;
				}
				else if (obj instanceof IServerModule) {
					IServerModule sm = (IServerModule) obj;
					IModule[] module = sm.getModule();
					selectedModule = module[module.length - 1];
					if (selectedModule != null) {
						server = sm.getServer();
					}
				}
			}
		}
		return server;
	}

	protected CloudFoundryServer getCloudFoundryServer() {
		if (selectedServer == null) {
			return null;
		}
		return (CloudFoundryServer) selectedServer.loadAdapter(CloudFoundryServer.class, null);

	}

	protected CloudFoundryApplicationModule getSelectedCloudAppModule() {
		CloudFoundryServer cloudServer = getCloudFoundryServer();
		if (cloudServer == null || selectedModule == null) {
			return null;
		}
		return cloudServer.getApplication(selectedModule);
	}

}
