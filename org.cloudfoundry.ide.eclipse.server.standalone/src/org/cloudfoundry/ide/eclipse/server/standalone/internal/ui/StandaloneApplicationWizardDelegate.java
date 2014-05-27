/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationEnvVarWizardPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationServicesWizardPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizardPage;
import org.cloudfoundry.ide.eclipse.server.core.ApplicationUrlLookupService;
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

		StandaloneDeploymentWizardPage deploymentPage = new StandaloneDeploymentWizardPage(
				cloudServer, applicationModule, descriptor,
				ApplicationUrlLookupService.getCurrentLookup(cloudServer), this);

		CloudFoundryApplicationWizardPage applicationNamePage = new CloudFoundryApplicationWizardPage(
				cloudServer, deploymentPage, applicationModule, descriptor);

		defaultPages.add(applicationNamePage);

		defaultPages.add(deploymentPage);

		CloudFoundryApplicationServicesWizardPage servicesPage = new CloudFoundryApplicationServicesWizardPage(
				cloudServer, applicationModule, descriptor);

		defaultPages.add(servicesPage);
		
		defaultPages.add(new CloudFoundryApplicationEnvVarWizardPage(cloudServer, descriptor
				.getDeploymentInfo()));
		return defaultPages;

	}
}
