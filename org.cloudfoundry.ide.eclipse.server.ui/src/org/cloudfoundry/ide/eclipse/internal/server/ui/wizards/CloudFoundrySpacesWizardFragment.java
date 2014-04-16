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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsValidationStatics;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerWizardValidator;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerWizardValidator.ValidationStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

public class CloudFoundrySpacesWizardFragment extends WizardFragment {

	private final ServerWizardValidator validator;

	private final CloudFoundryServer cloudServer;

	private CloudSpacesSelectionPart spacesPart;

	private IWizardHandle wizardHandle;

	private WizardChangeListener listener;

	public CloudFoundrySpacesWizardFragment(CloudFoundryServer cloudServer, ServerWizardValidator validator) {
		this.validator = validator;

		this.cloudServer = cloudServer;
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle wizardHandle) {
		this.wizardHandle = wizardHandle;
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		listener = new WizardFragmentChangeListener(wizardHandle) {

			@Override
			public void handleChange(PartChangeEvent event) {

				// Validate if there is a space change
				if (validator != null && event.getType() == ServerCredentialsValidationStatics.EVENT_SPACE_CHANGED) {
					validator.localValidation();
				}
				super.handleChange(event);
			}
		};
		spacesPart = new CloudSpacesSelectionPart(validator.getSpaceDelegate(), listener, cloudServer, wizardHandle);
		spacesPart.createPart(composite);
		return composite;
	}

	public boolean isComplete() {
		if (validator == null) {
			return false;
		}
		ValidationStatus status = validator.getPreviousValidationStatus();
		return status != null && status.getStatus().isOK();
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	public void enter() {
		if (wizardHandle != null) {
			wizardHandle
					.setMessage(
							"Resolving list of Cloud Foundry organizations and spaces. Please wait while the operation completes.",
							DialogPage.NONE);
		}
		IRunnableContext context = wizardHandle != null ? new WizardHandleContext(wizardHandle).getRunnableContext()
				: null;
		ValidationStatus status = validator.validate(true, context);
		if (listener != null) {
			listener.handleChange(status.getStatus());
		}

		if (spacesPart != null) {
			spacesPart.setInput();
		}

		super.enter();
	}

}
