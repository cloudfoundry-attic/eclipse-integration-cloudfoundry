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
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudApplicationUrlPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;

public class StandaloneDeploymentWizardPage extends
		CloudFoundryDeploymentWizardPage {

	public StandaloneDeploymentWizardPage(CloudFoundryServer server,
			CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor,
			CloudApplicationUrlLookup urlLookup,
			ApplicationWizardDelegate delegate) {
		super(server, module, descriptor, urlLookup, delegate);
	}

	@Override
	protected CloudApplicationUrlPart createUrlPart(
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
	protected void postDomainsRefreshedOperation() {
		if (urlPart == null) {
			return;
		}
		urlPart.refreshDomains();

		// Do not update the app URL after domains have been refreshed as
		// standalone does not require URL
	}

}
