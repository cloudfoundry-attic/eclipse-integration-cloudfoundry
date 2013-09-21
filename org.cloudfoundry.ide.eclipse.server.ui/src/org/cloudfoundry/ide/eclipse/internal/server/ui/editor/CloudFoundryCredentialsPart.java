/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.lang.reflect.InvocationTargetException;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryConstants;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryURLNavigation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.RegisterAccountWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
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

	public static final int UNVALIDATED_FILLED = 1000;

	public static final int VALIDATED = 1002;

	private static final String DEFAULT_DESCRIPTION = "Register or log in to {0} account.";

	private static final String VALID_ACCOUNT_MESSAGE = "Account information is valid. Click 'Next' to chose a cloud space, or 'Finish' to use the default space.";

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

	private CloudSpaceChangeHandler spaceChangeHandler;

	private IRunnableContext runnableContext;

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, CloudSpaceChangeHandler spaceChangeHandler,
			IPartChangeListener changeListener, WizardPage wizardPage) {
		this(cfServer, spaceChangeHandler, changeListener);

		if (wizardPage != null) {
			wizardPage.setTitle(NLS.bind("{0} Account", service));
			wizardPage.setDescription(NLS.bind(DEFAULT_DESCRIPTION, service));
			ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
			if (banner != null) {
				wizardPage.setImageDescriptor(banner);
			}
			runnableContext = wizardPage.getWizard() != null && wizardPage.getWizard().getContainer() != null ? wizardPage
					.getWizard().getContainer() : null;
		}
	}

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, CloudSpaceChangeHandler spaceChangeHandler,
			IPartChangeListener changeListener, final IWizardHandle wizardHandle) {
		this(cfServer, spaceChangeHandler, changeListener);
		if (wizardHandle != null) {
			wizardHandle.setTitle(NLS.bind("{0} Account", service));
			wizardHandle.setDescription(NLS.bind(DEFAULT_DESCRIPTION, service));
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

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, CloudSpaceChangeHandler spaceChangeHandler,
			IPartChangeListener changeListener) {

		this.cfServer = cfServer;
		this.serverTypeId = cfServer.getServer().getServerType().getId();
		this.service = CloudFoundryBrandingExtensionPoint.getServiceName(serverTypeId);
		this.spaceChangeHandler = spaceChangeHandler;

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

	public PartChangeEvent getValidationEvent(boolean validateAgainstServer) {

		IStatus localValidation = validateLocally();

		String userName = cfServer.getUsername();
		String password = cfServer.getPassword();
		String url = cfServer.getUrl();

		String message = localValidation.getMessage();
		String errorMsg = null;

		int validationType = PartChangeEvent.NONE;
		if (localValidation.isOK()) {

			if (validateAgainstServer) {
				errorMsg = CloudUiUtil.validateCredentials(cfServer, userName, password, url, true, runnableContext);

				// No credential errors, so now do a orgs and spaces lookup for
				// the newly validated credentials.
				if (errorMsg == null) {

					try {
						CloudSpacesDescriptor descriptor = spaceChangeHandler.getUpdatedDescriptor(url, userName,
								password, runnableContext);
						if (descriptor == null) {
							errorMsg = "Failed to resolve organizations and spaces for the given credentials. Please contact Cloud Foundry support.";
						}
						else {
							validationType = VALIDATED;
						}
					}
					catch (CoreException e) {
						errorMsg = "Failed to resolve organization and spaces "
								+ (e.getMessage() != null ? " due to " + e.getMessage()
										: ". Unknown error occurred while requesting list of spaces from the server")
								+ ". Please contact Cloud Foundry support.";
					}
				}
			}
			else {
				// If no validation request is made, check that there is a
				// spaces descriptor set and matches the current credentials.
				// This means the credentials were already validated in a
				// previous update.

				if (!spaceChangeHandler.matchesCurrentDescriptor(url, userName, password)) {
					spaceChangeHandler.clearSetDescriptor();
					message = "Please validate your credentials.";
					validationType = UNVALIDATED_FILLED;
				}
				else {
					validationType = VALIDATED;
				}
			}
		}

		// If not an actual server validation error, treat anything else as an
		// Info status, including local errors, as to display missing
		// value messages as info rather than error messages
		int statusType = IStatus.INFO;
		if (errorMsg != null) {
			message = errorMsg;
			statusType = IStatus.ERROR;
		}
		else if (validationType == VALIDATED) {
			message = VALID_ACCOUNT_MESSAGE;
		}

		IStatus eventStatus = CloudFoundryPlugin.getStatus(message, statusType);

		return new PartChangeEvent(null, eventStatus, this, validationType);

	}

	protected IStatus validateLocally() {

		String userName = cfServer.getUsername();
		String password = cfServer.getPassword();
		String url = cfServer.getUrl();
		String message = null;

		boolean valuesFilled = false;

		if (userName == null || userName.trim().length() == 0) {
			message = "Enter an email address.";
		}
		else if (password == null || password.trim().length() == 0) {
			message = "Enter a password.";
		}
		else if (url == null || url.trim().length() == 0) {
			message = NLS.bind("Select a {0} URL.", service);
		}
		else {
			valuesFilled = true;
			message = NLS.bind(DEFAULT_DESCRIPTION, service);
		}

		int statusType = valuesFilled ? IStatus.OK : IStatus.ERROR;

		return CloudFoundryPlugin.getStatus(message, statusType);
	}

	public void validate() {
		PartChangeEvent validationEvent = getValidationEvent(true);
		notifyChange(validationEvent);
	}

	/**
	 * 
	 * @param validateCredentials true if credentials should be validated, which
	 * would require a network I/O request sent to the server. False if only
	 * local validation should be performed (e.g. check for malformed URL)
	 */
	public void updateUI(boolean validateAgainstServer) {

		String url = cfServer.getUrl();

		PartChangeEvent validationEvent = getValidationEvent(validateAgainstServer);
		boolean valuesFilled = validationEvent.getType() == UNVALIDATED_FILLED
				|| validationEvent.getType() == VALIDATED;

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
