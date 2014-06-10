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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudApplicationUrlPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.ApplicationWizardDelegate;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryDeploymentWizardPage;

public class StandaloneDeploymentWizardPage extends
		CloudFoundryDeploymentWizardPage {

	public StandaloneDeploymentWizardPage(CloudFoundryServer server,
			CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor,
			ApplicationUrlLookupService urlLookup,
			ApplicationWizardDelegate delegate) {
		super(server, module, descriptor, urlLookup, delegate);
	}

	@Override
	protected CloudApplicationUrlPart createUrlPart(
			ApplicationUrlLookupService urlLookup) {
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
