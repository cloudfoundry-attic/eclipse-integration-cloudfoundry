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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
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
		setWindowTitle(Messages.SetValueVariablesWizard_TITILE_SET_VAR);
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
