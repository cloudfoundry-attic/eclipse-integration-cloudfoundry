/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UpdatePasswordDialog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class UpdatePasswordOperation implements ICloudFoundryOperation {

	private final CloudFoundryServer cloudServer;

	public UpdatePasswordOperation(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {

		final String[] updatedPassword = new String[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				Shell shell = CloudUiUtil.getShell();
				if (shell == null || shell.isDisposed()) {
					CloudFoundryPlugin.logError("No shell available to open update password dialogue"); //$NON-NLS-1$
					return;
				}
				final UpdatePasswordDialog dialog = new UpdatePasswordDialog(shell, cloudServer.getUsername(),
						cloudServer.getServer().getId());

				if (dialog.open() == Window.OK) {
					updatedPassword[0] = dialog.getPassword();
				}
			}
		});

		// Perform this outside of UI thread as to not lock it if it is long
		// running
		if (updatedPassword[0] != null) {
			cloudServer.setAndSavePassword(updatedPassword[0]);

			// Once password has been changed, reconnect the server to verify
			// that the password is valid
			final IStatus[] changeStatus = { Status.OK_STATUS };
			try {
				cloudServer.getBehaviour().reconnect(monitor);
			}
			catch (CoreException e) {
				changeStatus[0] = e.getStatus();
			}

			ServerEventHandler.getDefault().firePasswordUpdated(cloudServer, changeStatus[0]);

			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!changeStatus[0].isOK()) {
						if (changeStatus[0].getException() != null) {
							CloudFoundryPlugin.logError(changeStatus[0].getException());
						}
						MessageDialog.openError(
								Display.getDefault().getActiveShell(),
								Messages.UpdatePasswordDialog_ERROR_VERIFY_PW_TITLE,
								NLS.bind(Messages.UpdatePasswordCommand_ERROR_PW_UPDATE_BODY,
										changeStatus[0].getMessage()));
					}
					else {
						MessageDialog.openInformation(Display.getDefault().getActiveShell(),
								Messages.UpdatePasswordCommand_TEXT_PW_UPDATE,
								Messages.UpdatePasswordCommand_TEXT_PW_UPDATE_SUCC);
					}
				}
			});
		}
	}

}
