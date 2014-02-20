/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
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
		listener = new WizardFragmentChangeListener(wizardHandle);
		spacesPart = new CloudSpacesSelectionPart(validator.getSpaceDelegate(), listener, cloudServer, wizardHandle);
		spacesPart.createPart(composite);
		return composite;
	}

	public boolean isComplete() {
		return validator != null && validator.areCredentialsFilled();
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
