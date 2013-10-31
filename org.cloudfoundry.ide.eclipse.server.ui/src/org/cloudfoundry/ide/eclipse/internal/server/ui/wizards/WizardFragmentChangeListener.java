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
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

public class WizardFragmentChangeListener extends WizardChangeListener {

	private final IWizardHandle wizardHandle;

	public WizardFragmentChangeListener(IWizardHandle wizardHandle) {
		this.wizardHandle = wizardHandle;
	}

	protected void update() {
		wizardHandle.update();
	}

	protected void setWizardError(String message) {
		wizardHandle.setMessage(message, DialogPage.ERROR);

	}

	protected void setWizardInformation(String message) {
		wizardHandle.setMessage(message, DialogPage.INFORMATION);

	}

	protected void setWizardMessage(String message) {
		wizardHandle.setMessage(message, DialogPage.NONE);

	}

}
