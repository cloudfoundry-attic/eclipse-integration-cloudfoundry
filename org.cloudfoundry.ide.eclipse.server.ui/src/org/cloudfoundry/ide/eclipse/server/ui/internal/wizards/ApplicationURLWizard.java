/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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

import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudApplicationUrlPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
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

		protected void performWhenPageVisible() {

			// Refresh the application URL (since the URL host tends to the the
			// application name, if the application name has changed
			// make sure it gets updated in the UI. Also fetch the list of
			// domains
			// ONCE per session.
			// Run all URL refresh and fetch in the same UI Job to ensure that
			// domain updates occur first before the UI is refreshed.
			if (!refreshedDomains) {
				refreshApplicationUrlDomains();
			}
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
		protected void domainsRefreshed() {
			urlPart.refreshDomains();
			urlPart.setUrl(initialUrl);
		}

		public void handleChange(PartChangeEvent event) {
			if (event.getSource() == CloudUIEvent.APPLICATION_URL_CHANGED) {
				editedUrl = event.getData() instanceof String ? (String) event.getData() : null;
			}

			super.handleChange(event);
		}
	}
}
