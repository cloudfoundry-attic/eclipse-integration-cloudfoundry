/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.Map;

import org.eclipse.jface.wizard.Wizard;

public class UnsetOptionsWizard extends Wizard {

	private final Map<String, String> variableToValue;

	public UnsetOptionsWizard(Map<String, String> variableToValue) {
		this.variableToValue = variableToValue;
		setWindowTitle("Set Command Option Values");
	}

	public void addPages() {
		UnsetOptionsWizardPage page = new UnsetOptionsWizardPage();
		addPage(page);
	}

	public Map<String, String> getVariables() {
		return variableToValue;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
