/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.AddCommandDisplayPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ServiceCommandWizardPage extends CloudFoundryAwareWizardPage {

	private AddCommandDisplayPart displayPart;

	private IStatus partStatus;

	protected ServiceCommandWizardPage() {
		super("Command Page", "Command Definition", "Define a command to launch on a service tunnel", null);
	}

	public void createControl(Composite parent) {

		ServiceCommandWizard wizard = (ServiceCommandWizard) getWizard();

		displayPart = wizard.getContextServiceCommand() != null ? new AddCommandDisplayPart(wizard.getService(),
				wizard.getContextServiceCommand()) : new AddCommandDisplayPart(wizard.getService(), wizard
				.getCommands().getDefaultTerminal());
		displayPart.addPartChangeListener(new IPartChangeListener() {

			public void handleChange(PartChangeEvent event) {
				if (event != null) {
					partStatus = event.getStatus();
					if (partStatus == null || partStatus.isOK()) {
						setErrorMessage(null);
						setPageComplete(true);
					}
					else {
						if (ValueValidationUtil.isEmpty(partStatus.getMessage())) {
							setErrorMessage(null);
						}
						else {
							setErrorMessage(partStatus.getMessage());
						}
						setPageComplete(false);
					}
				}
			}

		});
		Control control = displayPart.createPart(parent);
		setControl(control);
	}

	@Override
	public boolean isPageComplete() {
		return partStatus == null || partStatus.isOK();
	}

	public AddCommandDisplayPart getCommandPart() {
		return displayPart;
	}
}
