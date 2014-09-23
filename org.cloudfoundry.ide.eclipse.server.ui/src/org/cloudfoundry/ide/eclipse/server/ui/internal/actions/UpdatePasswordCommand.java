/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UpdatePasswordDialog;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IServerWorkingCopy;

public class UpdatePasswordCommand extends BaseCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		IServerWorkingCopy wc = selectedServer.createWorkingCopy();
		final CloudFoundryServer cfServer = (CloudFoundryServer) wc.loadAdapter(CloudFoundryServer.class, null);
		final UpdatePasswordDialog dialog = new UpdatePasswordDialog(Display.getDefault().getActiveShell(), cfServer.getUsername());
		
		if (dialog.open() == IDialogConstants.OK_ID) {
			String errorMsg = CloudUiUtil.updatePassword(dialog.getPassword(), cfServer, wc);
			if (errorMsg != null) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.UpdatePasswordCommand_TEXT_PW_UPDATE, NLS.bind(Messages.UpdatePasswordCommand_ERROR_PW_UPDATE_BODY, errorMsg));
			} else {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.UpdatePasswordCommand_TEXT_PW_UPDATE, Messages.UpdatePasswordCommand_TEXT_PW_UPDATE_SUCC);
			}
		}
		return null;
	}
}
