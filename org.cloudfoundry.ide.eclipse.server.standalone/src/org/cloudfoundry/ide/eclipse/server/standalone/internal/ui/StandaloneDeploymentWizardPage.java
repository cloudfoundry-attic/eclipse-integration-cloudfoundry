/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudHostDomainUrlPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.application.JavaStartCommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class StandaloneDeploymentWizardPage extends
		CloudFoundryDeploymentWizardPage {

	public StandaloneDeploymentWizardPage(CloudFoundryServer server,
			CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor,
			CloudApplicationUrlLookup urlLookup,
			ApplicationWizardDelegate delegate) {
		super(server, module, descriptor, urlLookup, delegate);
	}

	protected StandaloneStartCommandPart standalonePart;

	@Override
	protected void createAreas(Composite parent) {

		Composite topComposite = new Composite(parent, SWT.NONE);
		GridLayout topLayout = new GridLayout(2, false);
		topComposite.setLayout(topLayout);
		topComposite
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		createURLArea(topComposite);

		createMemoryArea(topComposite);

		IProject project = module.getLocalModule().getProject();

		standalonePart = new StandaloneStartCommandPart(new JavaStartCommand(),
				project);
		standalonePart.addPartChangeListener(this);

		standalonePart.createPart(topComposite);

		createStartOrDebugOptions(topComposite);
	}

	@Override
	protected void createStartOrDebugOptions(Composite parent) {
		super.createStartOrDebugOptions(parent);

		// TODO: Enable when debug is supported again post CF 1.5.0
		// if (isServerDebugModeAllowed()) {
		// regularStartOnDeploymentButton.setText("Start application:");
		// GridData buttonData = new GridData(SWT.FILL, SWT.FILL, false, false);
		// regularStartOnDeploymentButton.setLayoutData(buttonData);
		//
		// // Also add two columns
		// GridLayoutFactory.fillDefaults().numColumns(2)
		// .applyTo(runDebugOptions);
		// }
	}

	@Override
	protected CloudHostDomainUrlPart createUrlPart(
			CloudApplicationUrlLookup urlLookup) {
		return new StandaloneAppUrlPart(urlLookup);
	}

	@Override
	protected void setUrlInDescriptor(String url) {

		if (ValueValidationUtil.isEmpty(url)) {
			// Set an empty list if URL is empty as it can cause problems when
			// deploying a standalone application
			List<String> urls = new ArrayList<String>();

			descriptor.getDeploymentInfo().setUris(urls);
			return;
		}
		super.setUrlInDescriptor(url);
	}

	@Override
	public void updateUrlInUI() {
		// Do nothing, as updating URL with the application name is not
		// applicable for Java standalone
	}

	public void handleChange(PartChangeEvent event) {
		if (event.getSource() == standalonePart) {
			String startCommand = event.getData() instanceof String ? (String) event
					.getData() : null;
			descriptor.setStartCommand(startCommand);
		}

		super.handleChange(event);
	}

}
