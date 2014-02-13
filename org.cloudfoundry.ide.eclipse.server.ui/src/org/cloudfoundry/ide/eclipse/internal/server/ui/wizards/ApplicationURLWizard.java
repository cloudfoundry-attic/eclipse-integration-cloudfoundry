/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudApplicationUrlPart;
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

		ImageDescriptor imageDescriptor = CloudFoundryImages.getWizardBanner(serverTypeId);
		// Use the cached version if possible.
		ApplicationUrlLookupService urlLookup = ApplicationUrlLookupService.getCurrentLookup(cloudServer);
		urlPage = createPage(imageDescriptor, urlLookup);
		urlPage.setWizard(this);
		addPage(urlPage);
	}

	public String getUrl() {
		return editedUrl;
	}

	protected ApplicationURLWizardPage createPage(ImageDescriptor imageDescriptor, ApplicationUrlLookupService urlLookup) {
		CloudApplicationUrlPart urlPart = new CloudApplicationUrlPart(urlLookup);
		return new ApplicationURLWizardPage(imageDescriptor, urlLookup, urlPart);
	}

	class ApplicationURLWizardPage extends AbstractURLWizardPage {

		private final CloudApplicationUrlPart urlPart;

		protected ApplicationURLWizardPage(ImageDescriptor titleImage, ApplicationUrlLookupService urlLookup,
				CloudApplicationUrlPart urlPart) {
			super("Application URL Page", title, titleImage, urlLookup);
			setDescription("Add or edit application URL.");
			this.urlPart = urlPart;
		}

		public void createControl(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

			urlPart.createPart(composite);
			urlPart.addPartChangeListener(this);

			setControl(composite);
		}

		@Override
		protected void postDomainsRefreshedOperation() {
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
