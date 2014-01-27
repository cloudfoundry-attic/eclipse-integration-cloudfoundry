/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.Map;

import org.eclipse.jface.wizard.Wizard;

/**
 * Prompts the user for missing variable values for both command options and
 * environment variables.
 * 
 */
public class SetValueVariablesWizard extends Wizard {

	private final Map<String, String> optionVariables;

	private final Map<String, String> envVariables;

	public SetValueVariablesWizard(Map<String, String> variableToValue, Map<String, String> environmentVariables) {
		this.optionVariables = variableToValue;
		this.envVariables = environmentVariables;
		setWindowTitle("Set Variable Values");
	}

	public void addPages() {
		SetValueVariablesWizardPage page = new SetValueVariablesWizardPage();
		addPage(page);
	}

	public Map<String, String> getOptionVariables() {
		return optionVariables;
	}

	public Map<String, String> getEnvironmentVariables() {
		return envVariables;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
