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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * Prompts a user for application deployment information. Any information set by
 * the user is set in the application module's deployment descriptor.
 * <p/>
 * To avoid setting deployment values in the application module if a user
 * cancels the operation, it is up to the caller to ensure that 1. the
 * application module has a deployment descriptor available to edit and 2. if
 * operation is cancelled, the values in the module are restored.
 */
public class CloudFoundryApplicationWizard extends Wizard {

	protected final CloudFoundryApplicationModule module;

	protected final CloudFoundryServer server;

	protected IApplicationWizardDelegate wizardDelegate;

	protected final ApplicationWizardDescriptor applicationDescriptor;

	protected DeploymentInfoWorkingCopy workingCopy;

	/**
	 * @param server must not be null
	 * @param module must not be null.
	 * @param workingCopy a working copy that should be edited by the wizard. If
	 * a user clicks "OK", the working copy will be saved into its corresponding
	 * app module. Must not be null.
	 * @param wizard delegate that provides wizard pages for the application
	 * module. If null, default Java web wizard delegate will be used.
	 */
	public CloudFoundryApplicationWizard(CloudFoundryServer server, CloudFoundryApplicationModule module,
			DeploymentInfoWorkingCopy workingCopy, IApplicationWizardDelegate wizardDelegate) {
		Assert.isNotNull(server);
		Assert.isNotNull(module);
		Assert.isNotNull(workingCopy);
		this.server = server;
		this.module = module;
		this.wizardDelegate = wizardDelegate;

		this.workingCopy = workingCopy;
		applicationDescriptor = new ApplicationWizardDescriptor(this.workingCopy);
		
		// By default applications are started after being pushed to the server
		applicationDescriptor.setApplicationStartMode(ApplicationAction.START);
		setNeedsProgressMonitor(true);
		setWindowTitle("Application");
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = super.canFinish();
		if (canFinish && wizardDelegate instanceof ApplicationWizardDelegate) {
			canFinish = ((ApplicationWizardDelegate) wizardDelegate).getApplicationDelegate()
					.validateDeploymentInfo(applicationDescriptor.getDeploymentInfo()).isOK();
		}

		return canFinish;
	}

	@Override
	public void addPages() {

		// if a wizard provider exists, see if it contributes pages to the
		// wizard
		List<IWizardPage> applicationDeploymentPages = null;

		if (wizardDelegate == null) {
			// Use the default Java Web pages
			wizardDelegate = ApplicationWizardRegistry.getDefaultJavaWebWizardDelegate();
		}

		applicationDeploymentPages = wizardDelegate.getWizardPages(applicationDescriptor, server, module);

		if (applicationDeploymentPages != null && !applicationDeploymentPages.isEmpty()) {
			for (IWizardPage updatedPage : applicationDeploymentPages) {
				addPage(updatedPage);
			}
		}
		else {

			String moduleID = module != null && module.getModuleType() != null ? module.getModuleType().getId()
					: "Unknown module type.";

			CloudFoundryPlugin
					.logError("No application deployment wizard pages found for application type: "
							+ moduleID
							+ ". Unable to complete application deployment. Check that the application type is registered in the Cloud Foundry application framework.");
		}

	}

	/**
	 * @return newly created services. The services may not necessarily be bound
	 * to the application. To see the actual list of services to be bound,
	 * obtain the deployment descriptor: {@link #getDeploymentDescriptor()}
	 */
	public List<CloudService> getCloudServicesToCreate() {
		return applicationDescriptor.getCloudServicesToCreate();
	}

	public boolean persistManifestChanges() {
		return applicationDescriptor.shouldPersistDeploymentInfo();
	}

	public DeploymentConfiguration getDeploymentConfiguration() {
		if (applicationDescriptor.getApplicationStartMode() != null) {
			return new DeploymentConfiguration(applicationDescriptor.getApplicationStartMode());
		}
		return null;
	}

	@Override
	public boolean performFinish() {
		workingCopy.save();
		return true;
	}

}
