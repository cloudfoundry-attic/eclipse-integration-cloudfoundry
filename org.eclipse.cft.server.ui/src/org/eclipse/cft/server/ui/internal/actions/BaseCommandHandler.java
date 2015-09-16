/*******************************************************************************
 * Copyright (c) 2014,2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
 *     Keith Chong, IBM - Support more general branded server type IDs via org.eclipse.ui.menus
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.ui.internal.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;

public abstract class BaseCommandHandler extends AbstractHandler implements IHandler {

	protected IServer selectedServer;

	protected IModule selectedModule;

	protected IWorkbenchPartSite partSite;

	// Must first init selected server or module. Or just override execute
	protected void initializeSelection(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart != null) {
			partSite = activePart.getSite();
			if (partSite != null) {
				ISelectionProvider selectionProvider = partSite.getSelectionProvider();
				if (selectionProvider != null) {
					ISelection selection = selectionProvider.getSelection();
					if (selection instanceof IStructuredSelection) {
						Object obj = ((IStructuredSelection) selection).getFirstElement();
						if (obj instanceof IServer) {
							this.selectedServer = (IServer) obj;
						}
						else if (obj instanceof IServerModule) {
							IServerModule sm = (IServerModule) obj;
							IModule[] module = sm.getModule();
							this.selectedModule = module[module.length - 1];
							if (this.selectedModule != null) {
								this.selectedServer = sm.getServer();
							}
						}
					}
				}
				else {
					logSelectionDetectionFailure();
				}
			}
			else {
				logSelectionDetectionFailure();
			}
		}
		else {
			logSelectionDetectionFailure();
		}
	}

	private void logSelectionDetectionFailure() {
		if (Logger.WARNING) {
			Logger.println(Logger.WARNING_LEVEL, this,
					"logSelectionDetectionFailure", "Failed to initialize command selection."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
