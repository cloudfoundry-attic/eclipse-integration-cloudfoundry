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

import org.cloudfoundry.ide.eclipse.server.ui.internal.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel.SetValueVariablesPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Prompts users for unset application options
 */
public class SetValueVariablesWizardPage extends CloudFoundryAwareWizardPage {

	private IStatus status;

	protected SetValueVariablesWizardPage() {
		super("Set Value Variables Page", "Set Values", "The following variables require values:", null);
	}

	public void createControl(Composite parent) {
		SetValueVariablesWizard wizard = (SetValueVariablesWizard) getWizard();
		Map<String, String> variableToValue = wizard.getOptionVariables();
		Map<String, String> environmentVariables = wizard.getEnvironmentVariables();
		SetValueVariablesPart part = new SetValueVariablesPart(variableToValue, environmentVariables);

		part.addPartChangeListener(new IPartChangeListener() {

			public void handleChange(PartChangeEvent event) {
				status = event.getStatus();
				if (status != null && !status.isOK()) {
					if (status.getMessage() != null && status.getMessage().length() > 0) {
						setErrorMessage(status.getMessage());
					}
					setPageComplete(false);
				}
				else {
					setErrorMessage(null);
					setPageComplete(true);
				}
			}

		});

		Control control = part.createPart(parent);
		setControl(control);
	}

	public boolean isPageComplete() {
		return status == null || status.isOK();
	}

}
