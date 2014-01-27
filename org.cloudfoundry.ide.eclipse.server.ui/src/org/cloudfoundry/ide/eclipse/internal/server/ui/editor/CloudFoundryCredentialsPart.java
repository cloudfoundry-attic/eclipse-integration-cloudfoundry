/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.lang.reflect.InvocationTargetException;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryConstants;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsValidationStatics;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryURLNavigation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerValidator;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerWizardValidator.ValidationStatus;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.RegisterAccountWizard;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

/**
 * @author Andy Clement
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Terry Denney
 * @author Nieraj Singh
 */
public class CloudFoundryCredentialsPart extends UIPart {

	private CloudFoundryServer cfServer;

	private Text emailText;

	private TabFolder folder;

	private Text passwordText;

	private String serverTypeId;

	private String service;

	private CloudUrlWidget urlWidget;

	private Button validateButton;

	private Button registerAccountButton;

	private Button cfSignupButton;

	private ServerValidator validator;

	private IRunnableContext runnableContext;

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, ServerValidator validator,
			IPartChangeListener changeListener, WizardPage wizardPage) {
		this(cfServer, validator, changeListener);

		if (wizardPage != null) {
			wizardPage.setTitle(NLS.bind("{0} Account", service));
			wizardPage.setDescription(NLS.bind(ServerCredentialsValidationStatics.DEFAULT_DESCRIPTION, service));
			ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
			if (banner != null) {
				wizardPage.setImageDescriptor(banner);
			}
			runnableContext = wizardPage.getWizard() != null && wizardPage.getWizard().getContainer() != null ? wizardPage
					.getWizard().getContainer() : null;
		}
	}

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, ServerValidator validator,
			IPartChangeListener changeListener, final IWizardHandle wizardHandle) {
		this(cfServer, validator, changeListener);
		if (wizardHandle != null) {
			wizardHandle.setTitle(NLS.bind("{0} Account", service));
			wizardHandle.setDescription(NLS.bind(ServerCredentialsValidationStatics.DEFAULT_DESCRIPTION, service));
			ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
			if (banner != null) {
				wizardHandle.setImageDescriptor(banner);
			}

			runnableContext = new IRunnableContext() {
				public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
						throws InvocationTargetException, InterruptedException {
					wizardHandle.run(fork, cancelable, runnable);
				}
			};
		}
	}

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, ServerValidator validator,
			IPartChangeListener changeListener) {

		this.cfServer = cfServer;
		this.serverTypeId = cfServer.getServer().getServerType().getId();
		this.service = CloudFoundryBrandingExtensionPoint.getServiceName(serverTypeId);
		this.validator = validator;

		if (changeListener != null) {
			addPartChangeListener(changeListener);
		}

		runnableContext = PlatformUI.getWorkbench().getProgressService();
	}

	public Control createPart(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		folder = new TabFolder(composite, SWT.NONE);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateUI(false);
			}
		});

		try {
			createExistingUserComposite(folder);
			updateUI(false);
		}
		catch (Throwable e1) {
			CloudFoundryPlugin.logError(e1);
		}

		return composite;

	}

	public void setServer(CloudFoundryServer server) {
		this.cfServer = server;
	}

	private void createExistingUserComposite(TabFolder folder) {
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite topComposite = new Composite(composite, SWT.NONE);
		topComposite.setLayout(new GridLayout(2, false));
		topComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label emailLabel = new Label(topComposite, SWT.NONE);
		emailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		emailLabel.setText("Email:");

		emailText = new Text(topComposite, SWT.BORDER);
		emailText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		emailText.setEditable(true);
		emailText.setFocus();
		if (cfServer.getUsername() != null) {
			emailText.setText(cfServer.getUsername());
		}

		emailText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				cfServer.setUsername(emailText.getText());
				updateUI(false);
			}
		});

		Label passwordLabel = new Label(topComposite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		passwordLabel.setText("Password:");

		passwordText = new Text(topComposite, SWT.PASSWORD | SWT.BORDER);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordText.setEditable(true);
		if (cfServer.getPassword() != null) {
			passwordText.setText(cfServer.getPassword());
		}

		passwordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				cfServer.setPassword(passwordText.getText());

				updateUI(false);
			}
		});

		urlWidget = new CloudUrlWidget(cfServer) {

			@Override
			protected void setUpdatedSelectionInServer() {

				super.setUpdatedSelectionInServer();

				updateUI(false);
			}

		};

		urlWidget.createControls(topComposite);

		String url = urlWidget.getURLSelection();
		if (url != null) {
			cfServer.setUrl(CloudUiUtil.getUrlFromDisplayText(url));
		}

		final Composite validateComposite = new Composite(composite, SWT.NONE);
		validateComposite.setLayout(new GridLayout(3, false));
		validateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		validateButton = new Button(validateComposite, SWT.PUSH);
		validateButton.setText("Validate Account");
		validateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				updateUI(true);

			}
		});

		registerAccountButton = new Button(validateComposite, SWT.PUSH);
		registerAccountButton.setText("Register Account...");
		registerAccountButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				RegisterAccountWizard wizard = new RegisterAccountWizard(cfServer);
				WizardDialog dialog = new WizardDialog(validateComposite.getShell(), wizard);
				if (dialog.open() == Window.OK) {
					if (wizard.getEmail() != null) {
						emailText.setText(wizard.getEmail());
					}
					if (wizard.getPassword() != null) {
						passwordText.setText(wizard.getPassword());
					}
				}
			}
		});

		cfSignupButton = new Button(validateComposite, SWT.PUSH);
		cfSignupButton.setText(CloudFoundryConstants.PUBLIC_CF_SERVER_SIGNUP_LABEL);
		cfSignupButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				CloudFoundryURLNavigation.CF_SIGNUP_URL.navigateExternal();
			}
		});

		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("Account Information");
		item.setControl(composite);
	}

	protected PartChangeEvent validateAndGetEvent(boolean validateAgainstServer) {
		ValidationStatus status = validator.validate(validateAgainstServer, runnableContext);
		PartChangeEvent validationEvent = new PartChangeEvent(null, status.getStatus(), this,
				status.getValidationType());
		return validationEvent;
	}

	/**
	 * 
	 * @param validateCredentials true if credentials should be validated, which
	 * would require a network I/O request sent to the server. False if only
	 * local validation should be performed (e.g. check for malformed URL)
	 */
	public void updateUI(boolean validateAgainstServer) {

		String url = cfServer.getUrl();

		PartChangeEvent validationEvent = validateAndGetEvent(validateAgainstServer);
		boolean valuesFilled = validationEvent.getType() == ServerCredentialsValidationStatics.EVENT_CREDENTIALS_FILLED
				|| validationEvent.getType() == ServerCredentialsValidationStatics.EVENT_SPACE_VALID;

		if (CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(url)) {
			cfSignupButton.setVisible(true);
		}
		else {
			cfSignupButton.setVisible(false);
		}

		// If the credentials have changed and do not match those used to
		// previously
		// set a space descriptor, clear the space descriptor

		validateButton.setEnabled(valuesFilled);

		registerAccountButton.setEnabled(CloudFoundryBrandingExtensionPoint.supportsRegistration(serverTypeId, url));

		notifyChange(validationEvent);

	}
}
