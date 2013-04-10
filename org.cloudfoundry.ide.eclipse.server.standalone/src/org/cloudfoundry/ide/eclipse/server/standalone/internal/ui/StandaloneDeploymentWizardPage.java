/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentInfoValidator;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.application.JavaStartCommand;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.ui.StartCommandPartFactory.ICommandChangeListener;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.ui.StartCommandPartFactory.StartCommandEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public class StandaloneDeploymentWizardPage extends
		CloudFoundryDeploymentWizardPage implements ICommandChangeListener {

	public StandaloneDeploymentWizardPage(
			CloudFoundryServer server, ApplicationModule module,
			ApplicationWizardDescriptor descriptor) {
		super(server, module, descriptor);
	}

	protected StandaloneStartCommandPart standalonePart;

	@Override
	protected void createAreas(Composite parent) {
		createURLArea(parent);

		createMemoryArea(parent);

		createCCNGPlanArea(parent);

		IProject project = module.getLocalModule().getProject();

		standalonePart = new StandaloneStartCommandPart(new JavaStartCommand(),
				this, project);
		standalonePart.createPart(parent);

		createStartOrDebugOptions(parent);

	}

	@Override
	protected void createStartOrDebugOptions(Composite parent) {
		super.createStartOrDebugOptions(parent);

		if (isServerDebugModeAllowed()) {
			// Change the indentation for standalone if debug option is present
			// Use default indentation
			GridData buttonData = new GridData(SWT.FILL, SWT.FILL, false, false);
			regularStartOnDeploymentButton.setLayoutData(buttonData);

			// Also add two columns
			GridLayoutFactory.fillDefaults().numColumns(2)
					.applyTo(runDebugOptions);
		}
	}

	@Override
	protected void setURL() {
		String url = urlText != null && !urlText.isDisposed() ? urlText
				.getText() : null;
		if (ValueValidationUtil.isEmpty(url)) {
			// Set an empty list if URL is empty as it can cause problems when
			// deploying a standalone application
			List<String> urls = new ArrayList<String>();
			descriptor.getDeploymentInfo().setUris(urls);
		}
	}

	@Override
	protected void update(boolean updateButtons) {
		canFinish = false;

		String startCommand = standalonePart.getStandaloneStartCommand();

		// Perform basic validation on the URL and start command
		DeploymentInfoValidator validator = new DeploymentInfoValidator(
				urlText.getText(), startCommand, true);

		IStatus status = validator.isValid();

		if (status.getSeverity() == IStatus.OK) {

			// Check if there is additional validation on the start command

			if (!standalonePart.isStartCommandValid()) {
				setErrorMessage("Invalid start command entered.");
			} else {

				// A Staging must exist in order to set the start command
				setErrorMessage(null);
				canFinish = true;
			}
		} else {
			setErrorMessage(status.getMessage() != null ? status.getMessage()
					: "Invalid URL value entered.");
		}

		// Set the start command whether valid or not, as the contents of the
		// descriptor are used to validate whether the wizard can complete or
		// not
		Staging staging = descriptor.getStaging();
		staging.setCommand(startCommand);

		if (updateButtons) {
			getWizard().getContainer().updateButtons();
		}
	}

	@Override
	public void updateUrl() {
		// Do nothing, as updating URL with the application name is not
		// applicable for Java standalone
	}

	public void handleEvent(StartCommandEvent event) {
		if (event.equals(StartCommandEvent.UPDATE)) {
			update(true);
		}
	}

}
