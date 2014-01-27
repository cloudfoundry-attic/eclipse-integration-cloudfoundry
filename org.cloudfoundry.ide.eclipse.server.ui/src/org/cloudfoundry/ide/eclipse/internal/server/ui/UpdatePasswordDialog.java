/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
		getShell().setText("Change Password");
		
		Composite control = (Composite) super.createDialogArea(parent);
		
		Composite composite = new Composite(control, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		description = new Label(composite, SWT.NONE);
		description.setText("Enter new password for '" + username + "'");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(description);
		
		Label newPasswordLabel = new Label(composite, SWT.NONE);
		newPasswordLabel.setText("New password: ");
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
		verifyPasswordLabel.setText("Verify password: ");
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
			description.setText("Enter new password for '" + username + "'");
			getButton(OK).setEnabled(false);
		} else if (verifyPassword == null || verifyPassword.length() == 0) {
			description.setText("Verify password for '" + username + "'");
			getButton(OK).setEnabled(false);
		} else if (! password.equals(verifyPassword)) {
			description.setText("Enter the same password in New password and Verify password");
			getButton(OK).setEnabled(false);
		} else {
			description.setText("Select OK to complete password change");
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
			MessageDialog.openError(getParentShell(), "Password Error", "Passwords did not match, please re-enter.");
			return;
		}
		
		super.okPressed();
	}
	
}
