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

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.UnsetOptionsPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Prompts users for unset application options
 */
public class UnsetOptionsWizardPage extends WizardPage {

	private Map<String, String> variableToValue;

	private IStatus status;

	protected UnsetOptionsWizardPage() {
		super("Unset Options Page");
		setTitle("Set Command Options");
		setDescription("The following command options require values.");
	}

	public void createControl(Composite parent) {
		IWizard wizard = getWizard();
		variableToValue = (wizard instanceof UnsetOptionsWizard) ? ((UnsetOptionsWizard) wizard).getVariables() : null;

		UnsetOptionsPart part = new UnsetOptionsPart(variableToValue);

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
