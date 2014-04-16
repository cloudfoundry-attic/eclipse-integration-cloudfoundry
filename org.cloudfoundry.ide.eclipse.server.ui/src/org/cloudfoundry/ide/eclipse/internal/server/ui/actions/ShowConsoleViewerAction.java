/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
