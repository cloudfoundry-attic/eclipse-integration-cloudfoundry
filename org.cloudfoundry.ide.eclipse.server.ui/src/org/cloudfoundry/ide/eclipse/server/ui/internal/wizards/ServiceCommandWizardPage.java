/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel.AddCommandDisplayPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ServiceCommandWizardPage extends CloudFoundryAwareWizardPage {

	private AddCommandDisplayPart displayPart;

	private IStatus partStatus;

	protected ServiceCommandWizardPage() {
		super(Messages.ServiceCommandWizardPage_TEXT_CMD_PAGE, Messages.ServiceCommandWizardPage_TEXT_CMD_PAGE_BODY, Messages.ServiceCommandWizardPage_TEXT_CMD_PAGE_DESCRIP, null);
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
