/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.io.File;
import java.util.List;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.wizard.Wizard;


/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryApplicationWizard extends Wizard {

	private CloudFoundryApplicationWizardPage applicationPage;

	private CloudFoundryDeploymentWizardPage deploymentPage;

	private CloudFoundryApplicationServicesWizardPage servicesPage;

	private final ApplicationModule module;

	private final CloudFoundryServer server;

	public CloudFoundryApplicationWizard(CloudFoundryServer server, ApplicationModule module) {
		Assert.isNotNull(server);
		Assert.isNotNull(module);
		this.server = server;
		this.module = module;
		setWindowTitle("Application");
	}

	@Override
	public void addPages() {
		deploymentPage = new CloudFoundryDeploymentWizardPage(server, module, this);
		if (module.getLocalModule() != null) {
			applicationPage = new CloudFoundryApplicationWizardPage(server, deploymentPage, module);
			addPage(applicationPage);
		}
		addPage(deploymentPage);
		servicesPage = new CloudFoundryApplicationServicesWizardPage(server, module);
		addPage(servicesPage);

	}

	public ApplicationInfo getApplicationInfo() {
		return (applicationPage != null) ? applicationPage.getApplicationInfo() : null;
	}

	public DeploymentInfo getDeploymentInfo() {
		return deploymentPage.getDeploymentInfo();
	}

	public ApplicationAction getDeploymentMode() {
		return deploymentPage.getDeploymentMode();
	}

	public File getWarFile() {
		return (applicationPage != null) ? applicationPage.getWarFile() : null;
	}

	/**
	 * May be empty if nothing added, but never null
	 * @return
	 */
	public List<CloudService> getAddedCloudServices() {
		return servicesPage.getAddedServices();
	}

	/**
	 * May be empty if nothing selected, but never null
	 * @return
	 */
	public List<String> getSelectedCloudServicesID() {
		return servicesPage.getSelectedServicesID();
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
