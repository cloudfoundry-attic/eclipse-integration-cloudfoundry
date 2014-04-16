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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * Default delegate for any application that uses standard Java web deployment
 * properties for Cloud Foundry. For example, default Java web applications
 * require at least one mapped application URL, therefore the wizard delegate
 * will provide wizard pages that require that at least one mapped application
 * URL is set by the user in the UI.
 * 
 */
public class JavaWebApplicationWizardDelegate extends ApplicationWizardDelegate {

	public List<IWizardPage> getWizardPages(ApplicationWizardDescriptor applicationDescriptor,
			CloudFoundryServer cloudServer, CloudFoundryApplicationModule applicationModule) {
		List<IWizardPage> defaultPages = new ArrayList<IWizardPage>();

		CloudFoundryDeploymentWizardPage deploymentPage = new CloudFoundryDeploymentWizardPage(cloudServer,
				applicationModule, applicationDescriptor, ApplicationUrlLookupService.getCurrentLookup(cloudServer), this);

		CloudFoundryApplicationWizardPage applicationNamePage = new CloudFoundryApplicationWizardPage(cloudServer,
				deploymentPage, applicationModule, applicationDescriptor);

		defaultPages.add(applicationNamePage);

		defaultPages.add(deploymentPage);

		CloudFoundryApplicationServicesWizardPage servicesPage = new CloudFoundryApplicationServicesWizardPage(
				cloudServer, applicationModule, applicationDescriptor);

		defaultPages.add(servicesPage);

		defaultPages.add(new CloudFoundryApplicationEnvVarWizardPage(cloudServer, applicationDescriptor
				.getDeploymentInfo()));
		return defaultPages;
	}

}
