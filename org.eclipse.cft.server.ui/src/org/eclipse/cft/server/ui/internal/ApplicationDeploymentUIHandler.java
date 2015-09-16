/*******************************************************************************
 * Copyright (c) 2013, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.ManifestParser;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentConfiguration;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardDelegate;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardRegistry;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Prepares an application for deployment. Application deployments are defined
 * by a deployment info {@link ApplicationDeploymentInfo}. A check will be
 * performed to see if there already exists a valid deployment info for the
 * application about to be deployed.
 * <p/>
 * If the deployment descriptor is not valid, the handler will prompt the user
 * for missing information via a deployment wizard.
 * <p/>
 * If a user was prompted by a wizard, an option also exists to overwrite the
 * existing manifest file with the new deployment info changes.
 * 
 */
public class ApplicationDeploymentUIHandler {

	/**
	 * Obtain a valid deployment info containing enough information to push an
	 * application, start it if necessary (starting an application is optional),
	 * and also optionally bind services to the application
	 * @param server where application should be pushed to.
	 * @param appModule pertaining to the application that needs to be pushed.
	 * @param monitor
	 * @throws CoreException if invalid deployment info.
	 * @throws OperationCanceledException if user canceled deployment.
	 * @return {@link DeploymentConfiguration} local deployment configuration
	 * for the app, or null if app should be deployed with default
	 * configuration.
	 */
	public DeploymentConfiguration prepareForDeployment(final CloudFoundryServer server,
			final CloudFoundryApplicationModule appModule, final IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {

		// Validate the existing deployment info. Do NOT save or make changes to
		// the deployment info prior to this stage
		// (for example, saving a working copy of the deployment info with
		// default values), unless it was done so for a module that is being
		// republished, as if the deployment info is valid,
		// the wizard will not open. We want the deployment wizard to ALWAYS
		// open when deploying an application for the first time (i.e the app
		// will not have
		// a deployment info set), even if we have to populate that deployment
		// info with default values or values from the manifest file. The latter
		// should only occur AFTER the handler decides to open the wizard.
		if (!appModule.validateDeploymentInfo().isOK()) {

			// Any application that can be pushed to a CF server
			// MUST have a delegate
			// which knows how to configure that application. If no
			// delegate is found,
			// the application is not currently deployable to a CF
			// server. In that case, a delegate
			// may be required to be registered for that application
			// type.
			final ApplicationWizardDelegate providerDelegate = ApplicationWizardRegistry.getWizardProvider(appModule
					.getLocalModule());

			if (providerDelegate == null) {
				throw CloudErrorUtil.toCoreException("Failed to open application deployment wizard for: " //$NON-NLS-1$
						+ appModule.getDeployedApplicationName()
						+ " when attempting to push application to " //$NON-NLS-1$
						+ server.getServer().getName()
						+ ". No application provider found that corresponds to the application type: " //$NON-NLS-1$
						+ appModule.getLocalModule().getModuleType().getId());
			}

			// Now parse the manifest file, if it exists, and load into a
			// deployment info working copy.
			// Do NOT save the working copy yet, as a user may cancel the
			// operation from the wizard.
			// THe working copy should only be saved by the wizard if a user
			// clicks "OK".
			DeploymentInfoWorkingCopy workingCopy = null;
			try {
				workingCopy = new ManifestParser(appModule, server).load(monitor);
			}
			catch (Throwable ce) {
				// Some failure occurred reading the manifest file. Proceed
				// anyway, to allow the user to manually enter deployment
				// values.
				CloudFoundryPlugin.logError(ce);
			}

			// A working copy of the deployment descriptor is needed in order to
			// prepopulate the application deployment wizard.
			if (workingCopy == null) {
				workingCopy = appModule.resolveDeploymentInfoWorkingCopy(monitor);
			}

			// Get the old working copy in case during the deployment wizard,
			// the app name changes
			// Apps are looked up by app name in the manifest, therefore if the
			// app name changed,
			// the old entry in the manifest
			ApplicationDeploymentInfo oldInfo = workingCopy.copy();

			final boolean[] cancelled = { false };
			final boolean[] writeToManifest = { false };
			final IStatus[] status = { Status.OK_STATUS };
			final DeploymentInfoWorkingCopy finWorkingCopy = workingCopy;
			final DeploymentConfiguration[] configuration = new DeploymentConfiguration[1];

			// Update the lookup
			ApplicationUrlLookupService.update(server, monitor);
			final List<CloudService> addedServices = new ArrayList<CloudService>();

			Display.getDefault().syncExec(new Runnable() {
				public void run() {

					CloudFoundryApplicationWizard wizard = new CloudFoundryApplicationWizard(server, appModule,
							finWorkingCopy, providerDelegate);

					try {
						WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
								.getShell(), wizard);
						int dialogueStatus = dialog.open();

						if (dialogueStatus == Dialog.OK) {

							// First add any new services to the server
							List<CloudService> services = wizard.getCloudServicesToCreate();
							if (services != null) {
								addedServices.addAll(services);
							}
							writeToManifest[0] = wizard.persistManifestChanges();
							configuration[0] = wizard.getDeploymentConfiguration();
						}
						else {
							cancelled[0] = true;
						}
					}
					catch (Throwable t) {
						// Any error in the wizard should result in the module
						// being deleted (i.e. cancelled)
						cancelled[0] = true;
						status[0] = CloudFoundryPlugin.getErrorStatus(t);
					}
				}
			});

			if (cancelled[0]) {
				if (!status[0].isOK()) {
					CloudFoundryPlugin.logError("Failed to deploy application due to: " + status[0].getMessage(), //$NON-NLS-1$
							status[0].getException());
				}
				throw new OperationCanceledException();
			}
			else {

				if (!addedServices.isEmpty()) {
					try {
						server.getBehaviour().operations().createServices(addedServices.toArray(new CloudService[0]))
								.run(monitor);
					}
					catch (CoreException e) {
						// Do not let service creation errors
						// stop the application deployment
						CloudFoundryPlugin.logError(e);
					}
				}

				if (status[0].isOK()) {
					status[0] = appModule.validateDeploymentInfo();
				}
				if (!status[0].isOK()) {
					throw new CoreException(status[0]);
				}
				else if (writeToManifest[0]) {

					try {
						new ManifestParser(appModule, server).write(monitor, oldInfo);
					}
					catch (Throwable ce) {
						// Do not let this error propagate, as failing to write
						// to the manifest should not stop the app's deployment
						CloudFoundryPlugin.logError(ce);
					}
				}

				return configuration[0];
			}
		}
		return null;
	}
}
