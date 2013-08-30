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

import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.SetValueVariablesPart;
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
