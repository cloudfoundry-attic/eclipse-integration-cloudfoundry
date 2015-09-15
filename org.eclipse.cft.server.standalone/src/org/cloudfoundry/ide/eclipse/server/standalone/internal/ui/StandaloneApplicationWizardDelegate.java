/*******************************************************************************
 * Copyright (c) 2013, 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.ApplicationWizardDelegate;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryApplicationEnvVarWizardPage;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryApplicationServicesWizardPage;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryApplicationWizardPage;
import org.eclipse.jface.wizard.IWizardPage;

public class StandaloneApplicationWizardDelegate extends
		ApplicationWizardDelegate {

	public StandaloneApplicationWizardDelegate() {
	}

	public List<IWizardPage> getWizardPages(
			ApplicationWizardDescriptor descriptor,
			CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule applicationModule) {
		List<IWizardPage> defaultPages = new ArrayList<IWizardPage>();

		ApplicationUrlLookupService urllookup = ApplicationUrlLookupService
				.getCurrentLookup(cloudServer);

		StandaloneDeploymentWizardPage deploymentPage = new StandaloneDeploymentWizardPage(
				cloudServer, applicationModule, descriptor, urllookup, this);

		CloudFoundryApplicationWizardPage applicationNamePage = new CloudFoundryApplicationWizardPage(
				cloudServer, applicationModule, descriptor);

		defaultPages.add(applicationNamePage);

		defaultPages.add(deploymentPage);

		CloudFoundryApplicationServicesWizardPage servicesPage = new CloudFoundryApplicationServicesWizardPage(
				cloudServer, applicationModule, descriptor);

		defaultPages.add(servicesPage);

		defaultPages.add(new CloudFoundryApplicationEnvVarWizardPage(
				cloudServer, descriptor.getDeploymentInfo()));
		return defaultPages;

	}
}
