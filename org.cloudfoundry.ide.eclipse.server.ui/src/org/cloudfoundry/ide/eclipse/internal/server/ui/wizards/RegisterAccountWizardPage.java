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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * @author Steffen Pingel
 */
public class RegisterAccountWizardPage extends WizardPage {

	private CloudFoundryServer cloudServer;

	private Text emailText;

	private Text passwordText;

	private Text verifyPasswordText;

	protected RegisterAccountWizardPage(CloudFoundryServer cloudServer) {
		super("registerAccount");
		this.cloudServer = cloudServer;
		setTitle("Register Account");
		setDescription(NLS.bind("Sign-up for an account at {0}.", cloudServer.getUrl()));
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label emailLabel = new Label(composite, SWT.NONE);
		emailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		emailLabel.setText("Email:");

		emailText = new Text(composite, SWT.BORDER);
		emailText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		emailText.setEditable(true);
		emailText.setFocus();
		if (cloudServer.getUsername() != null) {
			emailText.setText(cloudServer.getUsername());
		}
		emailText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		passwordLabel.setText("Password:");

		passwordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordText.setEditable(true);
		if (cloudServer.getPassword() != null) {
			passwordText.setText(cloudServer.getPassword());
		}
		passwordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		Label verifyPasswordLabel = new Label(composite, SWT.NONE);
		verifyPasswordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		verifyPasswordLabel.setText("Verify Password:");

		verifyPasswordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		verifyPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		verifyPasswordText.setEditable(true);
//		if (cloudServer.getPassword() != null) {
//			verifyPasswordText.setText(cloudServer.getPassword());
//		}
		verifyPasswordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		update();
		setControl(composite);
	}

	public String getEmail() {
		return emailText.getText();
	}

	public String getPassword() {
		return passwordText.getText();
	}

	private void update() {
		String errorMessage = null;
		String message = null;
		if (emailText.getText().length() == 0) {
			message = "Enter an email address.";
		}
		else if (passwordText.getText().length() == 0) {
			message = "Enter a password.";
		}
		else if (verifyPasswordText.getText().length() == 0) {
			message = "Enter password for verification.";
		}
		else if (!passwordText.getText().equals(verifyPasswordText.getText())) {
			errorMessage = "Passwords do not match.";
		}
		setMessage(message);
		setErrorMessage(errorMessage);
		setPageComplete(message == null && errorMessage == null);
	}

}
