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

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Handles changes to CF server credentials (e.g. changes to user name or
 * password) performed through a wizard.
 * 
 */
public abstract class WizardChangeListener implements IPartChangeListener {

	public void handleChange(PartChangeEvent event) {
		handleChange(event.getStatus());
	}

	public void handleChange(IStatus status) {
		if (status == null) {
			status = Status.OK_STATUS;
		}
		setWizardError(null);

		if (status.getSeverity() == IStatus.INFO) {
			setWizardInformation(status.getMessage());
		}
		else if (status.getSeverity() == IStatus.ERROR) {
			setWizardError(status.getMessage());
		}
		else if (status.getSeverity() == IStatus.OK && !Status.OK_STATUS.getMessage().equals(status.getMessage())) {
			// Do not display "OK" messages.
			setWizardMessage(status.getMessage());
		}

		update();
	}

	/**
	 * Update the wizard state and buttons
	 */
	abstract protected void update();

	abstract protected void setWizardError(String message);

	abstract protected void setWizardInformation(String message);

	abstract protected void setWizardMessage(String message);

}
