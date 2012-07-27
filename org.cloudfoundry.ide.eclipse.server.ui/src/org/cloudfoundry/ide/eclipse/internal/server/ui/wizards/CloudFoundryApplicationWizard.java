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

import java.util.List;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.client.lib.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneRuntimeType;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.wizard.Wizard;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
@SuppressWarnings("restriction")
public class CloudFoundryApplicationWizard extends Wizard {

	private AbstractCloudFoundryApplicationWizardPage applicationPage;

	private CloudFoundryDeploymentWizardPage deploymentPage;

	private CloudFoundryApplicationServicesWizardPage servicesPage;

	private final ApplicationModule module;

	private StandaloneDescriptor descriptor;

	private final CloudFoundryServer server;

	public CloudFoundryApplicationWizard(CloudFoundryServer server, ApplicationModule module) {
		Assert.isNotNull(server);
		Assert.isNotNull(module);
		this.server = server;
		this.module = module;
		setWindowTitle("Application");
	}

	/**
	 * Always non-null, although the application may not be a standalone
	 * application.
	 * @return
	 */
	public StandaloneDescriptor getStandaloneDescriptor() {
		if (descriptor == null) {
			descriptor = new StandaloneDescriptor(module);
		}
		return descriptor;
	}

	/**
	 * 
	 * @return true if the application is a standalone application with a
	 * supported runtime. False otherwise.
	 */
	public boolean isStandaloneApplication() {
		return getStandaloneDescriptor().isSupportedStandalone();
	}

	@Override
	public void addPages() {
		deploymentPage = new CloudFoundryDeploymentWizardPage(server, module, this);
		if (module.getLocalModule() != null) {
			applicationPage = isStandaloneApplication() ? new StandaloneApplicationWizardPage(server, deploymentPage,
					module) : new CloudFoundryApplicationWizardPage(server, deploymentPage, module);
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

	public Staging getStaging() {
		if (isStandaloneApplication()) {
			StandaloneDescriptor standaloneDescriptor = getStandaloneDescriptor();
			StandaloneRuntimeType runtimeType = standaloneDescriptor.getRuntimeType();
			String command = deploymentPage.getStandaloneStartCommand();
			Staging staging = new Staging(DeploymentConstants.STANDALONE_FRAMEWORK);
			staging.setCommand(command);
			staging.setRuntime(runtimeType.getId());
			return staging;
		}

		return null;
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
