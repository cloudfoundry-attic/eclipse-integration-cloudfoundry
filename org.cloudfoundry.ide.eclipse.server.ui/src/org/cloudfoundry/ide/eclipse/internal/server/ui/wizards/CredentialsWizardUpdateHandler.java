/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryCredentialsPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

/**
 * Updates the wizard based on changes to CF server credentials (e.g. changes to
 * user name or password) when creating or attempting to connect a CF server
 * instance.
 * 
 */
public class CredentialsWizardUpdateHandler implements IPartChangeListener {

	private final IWizardHandle wizardHandle;

	private final WizardPage wizardPage;

	private boolean credentialsValidated;

	private boolean credentialsFilled;

	private IStatus lastValidation;

	public CredentialsWizardUpdateHandler(IWizardHandle wizardHandle) {
		this(wizardHandle, null);
	}

	public CredentialsWizardUpdateHandler(WizardPage page) {
		this(null, page);
	}

	private CredentialsWizardUpdateHandler(IWizardHandle wizardHandle, WizardPage wizardPage) {
		this.wizardHandle = wizardHandle;
		this.wizardPage = wizardPage;
	}

	/**
	 * True if the credentials have been validated and are correct. False
	 * otherwise.
	 * @return
	 */
	public boolean isValid() {
		return credentialsValidated;
	}

	/**
	 * True if the credential values have been entered (i.e., there are no
	 * missing values), although they may not be necessarily validated yet.
	 * @return
	 */
	public boolean credentialsFilled() {
		return credentialsFilled;
	}

	public void handleChange(PartChangeEvent event) {
		lastValidation = event.getStatus() != null ? event.getStatus() : Status.OK_STATUS;

		credentialsValidated = event.getType() == CloudFoundryCredentialsPart.VALIDATED;
		credentialsFilled = credentialsValidated || event.getType() == CloudFoundryCredentialsPart.UNVALIDATED_FILLED;

		if (lastValidation.getSeverity() == IStatus.INFO) {
			setWizardError(null);
			setWizardInformation(lastValidation.getMessage());
		}
		else if (lastValidation.getSeverity() == IStatus.ERROR) {
			setWizardError(lastValidation.getMessage());
		}
		else {
			setWizardError(null);
		}

		update();
	}

	public IStatus getLastValidationStatus() {
		return lastValidation;
	}

	/**
	 * Update the wizard state and buttons
	 */
	protected void update() {
		if (wizardHandle != null) {
			wizardHandle.update();
		}
		else if (wizardPage != null && wizardPage.getWizard() != null && wizardPage.getWizard().getContainer() != null
				&& wizardPage.getWizard().getContainer().getCurrentPage() != null) {
			wizardPage.getWizard().getContainer().updateButtons();
		}
	}

	protected void setWizardError(String message) {
		if (wizardHandle != null) {
			wizardHandle.setMessage(message, DialogPage.ERROR);
		}
		else if (wizardPage != null) {
			wizardPage.setErrorMessage(message);
		}
	}

	protected void setWizardInformation(String message) {
		if (wizardHandle != null) {
			wizardHandle.setMessage(message, DialogPage.INFORMATION);
		}
		else if (wizardPage != null) {
			wizardPage.setMessage(message, DialogPage.INFORMATION);
		}
	}

}
