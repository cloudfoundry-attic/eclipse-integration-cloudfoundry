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

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;

public class WizardPageChangeListener extends WizardChangeListener {

	private final WizardPage wizardPage;

	public WizardPageChangeListener(WizardPage wizardPage) {
		this.wizardPage = wizardPage;
	}


	protected void update() {
		if (wizardPage.getWizard() != null && wizardPage.getWizard().getContainer() != null
				&& wizardPage.getWizard().getContainer().getCurrentPage() != null) {
			wizardPage.getWizard().getContainer().updateButtons();
		}
	}

	protected void setWizardError(String message) {
		wizardPage.setErrorMessage(message);
	}

	protected void setWizardInformation(String message) {
		wizardPage.setMessage(message, DialogPage.INFORMATION);

	}

	protected void setWizardMessage(String message) {
		wizardPage.setMessage(message, DialogPage.NONE);

	}

}
