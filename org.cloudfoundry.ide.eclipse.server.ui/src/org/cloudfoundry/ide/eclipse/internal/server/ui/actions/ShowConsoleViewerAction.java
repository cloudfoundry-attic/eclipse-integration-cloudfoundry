/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IServer;

public class ShowConsoleViewerAction extends AbstractCloudFoundryServerAction {

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// nothing
	}

	public void doRun(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		// Only show console for first instance.
		new ShowConsoleEditorAction(cloudServer, appModule, 0).run();
	}


	protected void serverSelectionChanged(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		if (cloudServer != null && (cloudServer.getServer().getServerState() == IServer.STATE_STARTED)) {
			if (appModule != null) {
				int state = appModule.getState();
				// Enable only if application is running and deployed
				if (state == IServer.STATE_STARTED && appModule.getApplication() != null) {
					action.setEnabled(true);
					return;
				}
			}
		}
		action.setEnabled(false);

	}
}
