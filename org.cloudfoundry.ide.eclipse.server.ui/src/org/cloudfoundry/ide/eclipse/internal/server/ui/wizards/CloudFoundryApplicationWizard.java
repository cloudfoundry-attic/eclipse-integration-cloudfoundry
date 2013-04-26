/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
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

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationPlan;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationFramework;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRuntime;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 * @author Nieraj Singh
 */

public class CloudFoundryApplicationWizard extends Wizard {

	private final ApplicationModule module;

	private final CloudFoundryServer server;

	private final ApplicationWizardProviderDelegate provider;

	private ApplicationWizardDescriptor applicationDescriptor;

	private List<ApplicationFramework> frameworks;

	private List<ApplicationRuntime> runtimes;

	private List<ApplicationPlan> v2ApplicationPlans;

	public CloudFoundryApplicationWizard(CloudFoundryServer server, ApplicationModule module,
			ApplicationWizardProviderDelegate provider) {
		Assert.isNotNull(server);
		Assert.isNotNull(module);
		this.server = server;
		this.module = module;
		this.provider = provider;

		v2ApplicationPlans = server != null ? server.getBehaviour().getApplicationPlans() : null;

		applicationDescriptor = isCCNGServer() && v2ApplicationPlans != null && !v2ApplicationPlans.isEmpty() ? new CCNGV2ApplicationWizardDescriptor()
				: new ApplicationWizardDescriptor();

		loadApplicationOptions();
		setWindowTitle("Application");
	}

	public boolean isCCNGServer() {
		return server.getBehaviour().supportsSpaces();
	}

	public List<ApplicationFramework> getFrameworks() {
		return frameworks;
	}

	public List<ApplicationRuntime> getRuntimes() {
		return runtimes;
	}

	public List<ApplicationPlan> getV2ApplicationPlans() {
		return v2ApplicationPlans;
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = super.canFinish();
		if (canFinish) {
			IApplicationWizardDelegate wizardDelegate = provider.getWizardDelegate();
			if (wizardDelegate instanceof AbstractApplicationWizardDelegate) {
				canFinish = ((AbstractApplicationWizardDelegate) wizardDelegate).isValid(applicationDescriptor);
			}
		}

		return canFinish;
	}

	protected ApplicationFramework getApplicationFramework(IApplicationDelegate delegate) throws CoreException {
		ApplicationFramework framework = delegate.getFramework(module.getLocalModule());

		if (framework == null) {
			String error = "Failed to push the application to the Cloud Foundry server because the application framework could not be resolved for: "
					+ server.getServerId();
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(error));

		}
		else {
			return framework;
		}

	}

	protected void loadApplicationOptions() {

		if (provider != null) {
			IApplicationDelegate delegate = provider.getApplicationDelegate();

			try {

				frameworks = delegate.getSupportedFrameworks();
				runtimes = delegate.getRuntimes(server);

				ApplicationFramework defaultFramework = getApplicationFramework(delegate);

				if (defaultFramework != null && runtimes != null && !runtimes.isEmpty()) {
					// Set a staging using the default framework and the first
					// runtime encountered. These values can then be changed by
					// the user
					applicationDescriptor.setStaging(defaultFramework, runtimes.get(0));
				}
				else {
					CloudFoundryPlugin
							.logError("Unable to resolve a default framework and runtime for application when publishing to a Cloud Foundry server: "
									+ module.getApplicationId()
									+ " of type: "
									+ module.getLocalModule().getModuleType().getId());
				}

			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
	}

	@Override
	public void addPages() {

		IApplicationWizardDelegate wizardDelegate = provider.getWizardDelegate();
		// if a wizard provider exists, see if it contributes pages to the
		// wizard
		List<IWizardPage> updatePages = null;

		if (wizardDelegate != null) {

			// Pass a copy to avoid the provider from modifying the original
			// list of pages
			updatePages = wizardDelegate.getWizardPages(applicationDescriptor, server, module);

		}

		if (updatePages == null || updatePages.isEmpty()) {
			// Use the default Java Web pages
			updatePages = new JavaWebApplicationWizardDelegate().getWizardPages(applicationDescriptor, server, module);
		}

		for (IWizardPage updatedPage : updatePages) {
			addPage(updatedPage);
		}
	}

	public ApplicationInfo getApplicationInfo() {
		return applicationDescriptor.getApplicationInfo();
	}

	public ApplicationPlan getApplicationPlan() {
		return (applicationDescriptor instanceof CCNGV2ApplicationWizardDescriptor) ? ((CCNGV2ApplicationWizardDescriptor) applicationDescriptor)
				.getApplicationPlan() : null;
	}

	public DeploymentInfo getDeploymentInfo() {
		return applicationDescriptor.getDeploymentInfo();
	}

	public ApplicationAction getDeploymentMode() {
		return applicationDescriptor.getStartDeploymentMode();
	}

	public Staging getStaging() {
		return applicationDescriptor.getStaging();
	}

	/**
	 * @return
	 */
	public List<CloudService> getCreatedCloudServices() {
		return applicationDescriptor.getCreatedCloudServices();
	}

	/**
	 * May be empty if nothing selected, but never null
	 * @return
	 */
	public List<String> getSelectedServicesForBinding() {
		return applicationDescriptor.getSelectedServicesForBinding();
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
