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

import org.cloudfoundry.ide.eclipse.server.ui.internal.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Handles status in a wizard, including display the status message and
 * refreshing the wizard UI state.
 * 
 */
public abstract class WizardStatusHandler implements IPartChangeListener {

	public void handleChange(PartChangeEvent event) {
		IStatus status = event != null ? event.getStatus() : null;
		handleChange(status);
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
