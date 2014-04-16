/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
