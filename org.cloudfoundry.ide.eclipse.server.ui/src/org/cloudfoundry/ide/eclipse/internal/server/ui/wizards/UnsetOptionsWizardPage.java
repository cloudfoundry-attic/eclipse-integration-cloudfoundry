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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOption;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.UnsetOptionsPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class UnsetOptionsWizardPage extends WizardPage {

	private List<CommandOption> unsetOptions;
	
	private IStatus status;

	protected UnsetOptionsWizardPage() {
		super("Unset Options Page");
		setTitle("Set Command Options");
		setDescription("The following command options require values.");
	}

	public void createControl(Composite parent) {
		IWizard wizard = getWizard();
		unsetOptions = (wizard instanceof UnsetOptionsWizard) ? ((UnsetOptionsWizard) wizard).getCommandOptions()
				: null;

		UnsetOptionsPart part = new UnsetOptionsPart(unsetOptions);

		part.addPartChangeListener(new IPartChangeListener() {

			public void handleChange(PartChangeEvent event) {
				status = event.getStatus();
				if (status != null && !status.isOK()) {
					setErrorMessage(status.getMessage());
					setPageComplete(false);
				}
				else {
					setErrorMessage(null);
					setPageComplete(true);
				}
			}

		});

		Control control = part.createControl(parent);
		setControl(control);
	}

	

	public boolean isPageComplete() {
		return status == null || status.isOK();
	}

}
