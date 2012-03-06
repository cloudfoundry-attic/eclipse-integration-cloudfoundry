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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UpdatePasswordDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.IServerModule;


/**
 * @author Terry Denney
 */
public class UpdatePasswordAction implements IObjectActionDelegate, IViewActionDelegate {

	private IServer selectedServer;
	
	public void run(IAction action) {
		IServerWorkingCopy wc = selectedServer.createWorkingCopy();
		final CloudFoundryServer cfServer = (CloudFoundryServer) wc.loadAdapter(CloudFoundryServer.class, null);
		final UpdatePasswordDialog dialog = new UpdatePasswordDialog(Display.getDefault().getActiveShell(), cfServer.getUsername());
		
		if (dialog.open() == IDialogConstants.OK_ID) {
			String errorMsg = CloudUiUtil.updatePassword(dialog.getPassword(), cfServer, wc);
			if (errorMsg != null) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Password Update", "Password update failed: " + errorMsg);
			} else {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Password Update", "Password update successful.");
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		selectedServer = null;
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof IServer) {
					selectedServer = (IServer) obj;
				}
				else if (obj instanceof IServerModule) {
					IServerModule sm = (IServerModule) obj;
					IModule[] module = sm.getModule();
					IModule selectedModule = module[module.length - 1];
					if (selectedModule != null) {
						selectedServer = sm.getServer();
					}
				}
			}
		}
		if (selectedServer != null) {
			action.setEnabled(selectedServer != null);
		}
	}

	public void init(IViewPart view) {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
