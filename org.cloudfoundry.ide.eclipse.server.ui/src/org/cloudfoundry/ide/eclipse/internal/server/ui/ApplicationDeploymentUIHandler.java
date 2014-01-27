/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.RepublishModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ManifestParser;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;

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
	 */
	public void prepareForDeployment(final CloudFoundryServer server, final CloudFoundryApplicationModule appModule,
			final IProgressMonitor monitor) throws CoreException, OperationCanceledException {

		// First check if the module is set for automatic republish (i.e. a
		// prior publish for the application
		// failed, but the deployment info is available through the republish
		// module

		IModule module = appModule.getLocalModule();

		RepublishModule repModule = CloudFoundryPlugin.getModuleCache().getData(server.getServerOriginal())
				.untagForAutomaticRepublish(module);

		if (repModule != null) {
			ApplicationDeploymentInfo republishDeploymentInfo = repModule.getDeploymentInfo();
			if (republishDeploymentInfo != null) {
				DeploymentInfoWorkingCopy copy = appModule.getDeploymentInfoWorkingCopy();
				copy.setInfo(republishDeploymentInfo);
				copy.save();
			}
		}

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
				throw CloudErrorUtil.toCoreException("Failed to open application deployment wizard for: "
						+ appModule.getDeployedApplicationName() + " when attempting to push application to "
						+ server.getServer().getName()
						+ ". No application provider found that corresponds to the application type: "
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
				workingCopy = new ManifestParser(appModule, server).load();
			}
			catch (Throwable ce) {
				// Some failure occurred reading the manifest file. Proceed
				// anyway, to allow the user to manually enter deployment
				// values.
				CloudFoundryPlugin.logError(ce);
			}
			
			// Get the old working copy in case during the deployment wizard, the app name changes
			// Apps are looked up by app name in the manifest, therefore if the app name changed,
			// the old entry in the manifest 
			ApplicationDeploymentInfo oldInfo = workingCopy != null ? workingCopy.copy() : null;

			final boolean[] cancelled = { false };
			final boolean[] writeToManifest = { false };
			final IStatus status[] = { Status.OK_STATUS };
			final DeploymentInfoWorkingCopy finWorkingCopy = workingCopy;

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
							final List<CloudService> addedServices = wizard.getCloudServicesToCreate();
							writeToManifest[0] = wizard.persistManifestChanges();

							if (addedServices != null && !addedServices.isEmpty()) {
								IProgressMonitor subMonitor = new SubProgressMonitor(monitor, addedServices.size());
								try {
									server.getBehaviour().createService(addedServices.toArray(new CloudService[0]),
											subMonitor);
								}
								catch (CoreException e) {
									CloudFoundryPlugin.log(e);
								}
								finally {
									subMonitor.done();
								}
							}
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
					CloudFoundryPlugin.logError("Failed to deploy application due to: " + status[0].getMessage(),
							status[0].getException());
				}
				throw new OperationCanceledException();
			}
			else {
				if (status[0].isOK()) {
					status[0] = appModule.validateDeploymentInfo();
				}
				if (!status[0].isOK()) {
					throw new CoreException(status[0]);
				}
				else if (writeToManifest[0]) {

					IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
					try {
						new ManifestParser(appModule, server).write(subMonitor, oldInfo);
					}
					catch (Throwable ce) {
						// Do not let this error propagate, as failing to write
						// to the manifest should not stop the app's deployment
						CloudFoundryPlugin.logError(ce);
					}
					finally {
						subMonitor.done();
					}
				}
			}
		}
	}
}
