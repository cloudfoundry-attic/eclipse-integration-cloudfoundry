/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Terry Denney
 */
public class UpdatePasswordDialog extends Dialog {
	
	private String password;
	
	private String verifyPassword;
	
	private final String username;

	private Label description;
	
	public UpdatePasswordDialog(Shell parentShell, String username) {
		super(parentShell);
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.UpdatePasswordDialog_TEXT_CHANGE_PW_TITLE);
		
		Composite control = (Composite) super.createDialogArea(parent);
		
		Composite composite = new Composite(control, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		description = new Label(composite, SWT.NONE);
		description.setText(NLS.bind(Messages.UpdatePasswordDialog_TEXT_ENTER_NEW_PW, username));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(description);
		
		Label newPasswordLabel = new Label(composite, SWT.NONE);
		newPasswordLabel.setText(Messages.UpdatePasswordDialog_TEXT_NEW_PW_LABEL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(newPasswordLabel);
		
		final Text newPasswordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).hint(250, SWT.DEFAULT).applyTo(newPasswordText);
		newPasswordText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				password = newPasswordText.getText();
				update();
			}
		});
		
		Label verifyPasswordLabel = new Label(composite, SWT.NONE);
		verifyPasswordLabel.setText(Messages.UpdatePasswordDialog_TEXT_VERIFY_PW_LABEL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(verifyPasswordLabel);
		
		final Text verifyPasswordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(verifyPasswordText);
		verifyPasswordText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				verifyPassword = verifyPasswordText.getText();
				update();
			}
		});
		
		return control;
	}
	
	private void update() {
		getButton(OK).setEnabled(password != null && password.length() > 0 && verifyPassword != null && verifyPassword.length() > 0);
		
		if (password == null || password.length() == 0) {
			description.setText(NLS.bind(Messages.UpdatePasswordDialog_TEXT_ENTER_NEW_PW, username));
			getButton(OK).setEnabled(false);
		} else if (verifyPassword == null || verifyPassword.length() == 0) {
			description.setText(NLS.bind(Messages.UpdatePasswordDialog_TEXT_VERIFY_PW_FOR, username));
			getButton(OK).setEnabled(false);
		} else if (! password.equals(verifyPassword)) {
			description.setText(Messages.UpdatePasswordDialog_TEXT_MISMATCH_PW);
			getButton(OK).setEnabled(false);
		} else {
			description.setText(Messages.UpdatePasswordDialog_TEXT_PROMPT_OK);
			getButton(OK).setEnabled(true);
		}
	}
	
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(OK).setEnabled(false);
		return buttonBar;
	}
	
	@Override
	protected void okPressed() {
		if (! verifyPassword.equals(password)) {
			MessageDialog.openError(getParentShell(), Messages.UpdatePasswordDialog_ERROR_VERIFY_PW_TITLE, Messages.UpdatePasswordDialog_ERROR_VERIFY_PW_BODY);
			return;
		}
		
		super.okPressed();
	}
	
}
