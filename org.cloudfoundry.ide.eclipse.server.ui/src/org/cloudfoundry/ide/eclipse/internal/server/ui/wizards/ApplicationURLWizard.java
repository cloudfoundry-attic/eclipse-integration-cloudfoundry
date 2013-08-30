/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudHostDomainUrlPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Allows the editing or addition of an application URL based on an existing
 * list of Cloud domains.
 * 
 */
public class ApplicationURLWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private final String initialUrl;

	private String editedUrl;

	private ApplicationURLWizardPage urlPage;

	private static final String title = "Add or Edit Application URL";

	public ApplicationURLWizard(CloudFoundryServer cloudServer, String initialUrl) {
		this.cloudServer = cloudServer;
		this.initialUrl = initialUrl;
		setWindowTitle(title);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		String serverTypeId = cloudServer.getServer().getServerType().getId();

		ImageDescriptor imgDescriptor = CloudFoundryImages.getWizardBanner(serverTypeId);
		CloudApplicationUrlLookup urlLookup = new CloudApplicationUrlLookup(cloudServer);
		urlPage = new ApplicationURLWizardPage(imgDescriptor, urlLookup);
		urlPage.setWizard(this);
		addPage(urlPage);
	}

	public String getUrl() {
		return editedUrl;
	}

	class ApplicationURLWizardPage extends AbstractURLWizardPage {

		private CloudHostDomainUrlPart urlPart;

		protected ApplicationURLWizardPage(ImageDescriptor titleImage, CloudApplicationUrlLookup urlLookup) {
			super("Application URL Page", title, titleImage, urlLookup);
			setDescription("Add or edit application URL.");
		}

		public void createControl(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

			urlPart = new CloudHostDomainUrlPart(urlLookup);
			urlPart.createPart(composite);
			urlPart.addPartChangeListener(this);

			setControl(composite);
		}

		@Override
		protected void refreshURLUI() {
			urlPart.refreshDomains();
			urlPart.updateFullUrl(initialUrl);
		}

		public void handleChange(PartChangeEvent event) {
			if (event.getSource() == urlPart) {
				editedUrl = event.getData() instanceof String ? (String) event.getData() : null;
			}

			super.handleChange(event);
		}

	}

}
