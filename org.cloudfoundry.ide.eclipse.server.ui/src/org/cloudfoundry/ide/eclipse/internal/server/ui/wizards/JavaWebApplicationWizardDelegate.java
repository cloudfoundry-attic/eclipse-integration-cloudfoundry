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
