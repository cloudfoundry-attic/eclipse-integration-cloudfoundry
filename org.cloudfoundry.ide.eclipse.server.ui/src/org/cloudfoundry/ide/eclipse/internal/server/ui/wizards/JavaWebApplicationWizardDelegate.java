/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.jface.wizard.IWizardPage;

public class JavaWebApplicationWizardDelegate extends AbstractApplicationWizardDelegate {

	public List<IWizardPage> getWizardPages(ApplicationWizardDescriptor applicationDescriptor,
			CloudFoundryServer cloudServer, ApplicationModule applicationModule) {
		List<IWizardPage> defaultPages = new ArrayList<IWizardPage>();

		CloudFoundryDeploymentWizardPage deploymentPage = new CloudFoundryDeploymentWizardPage(cloudServer,
				applicationModule, applicationDescriptor);

		CloudFoundryApplicationWizardPage runtimeFrameworkPage = new CloudFoundryApplicationWizardPage(cloudServer,
				deploymentPage, applicationModule, applicationDescriptor);

		defaultPages.add(runtimeFrameworkPage);

		defaultPages.add(deploymentPage);

		CloudFoundryApplicationServicesWizardPage servicesPage = new CloudFoundryApplicationServicesWizardPage(
				cloudServer, applicationModule, applicationDescriptor);

		defaultPages.add(servicesPage);
		return defaultPages;
	}
}
