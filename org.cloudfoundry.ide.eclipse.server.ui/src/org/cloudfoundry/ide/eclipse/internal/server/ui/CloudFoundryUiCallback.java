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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.List;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CaldecottUIHelper;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryCredentialsWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.DeleteServicesWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryUiCallback extends CloudFoundryCallback {

	@Override
	public void applicationStarted(CloudFoundryServer server, ApplicationModule cloudModule) {
		for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
			ConsoleManager.getInstance().startConsole(server, cloudModule.getApplication(), i, i == 0);
		}
	}

	@Override
	public void deleteApplication(ApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
			ConsoleManager.getInstance().stopConsole(cloudServer.getServer(), cloudModule.getApplication(), i);
		}
	}

	public void displayCaldecottTunnelConnections(CloudFoundryServer cloudServer) {
		new CaldecottUIHelper(cloudServer).displayCaldecottTunnelConnections();
	}

	@Override
	public void applicationStopping(CloudFoundryServer server, ApplicationModule cloudModule) {
		// CloudApplication application = cloudModule.getApplication();
		// if (application != null) {
		// consoleManager.stopConsole(application);
		// }
	}

	@Override
	public void disconnecting(CloudFoundryServer server) {
		ConsoleManager.getInstance().stopConsoles();
	}

	@Override
	public void getCredentials(final CloudFoundryServer server) {
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				CloudFoundryCredentialsWizard wizard = new CloudFoundryCredentialsWizard(server);
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
						.getShell(), wizard);
				dialog.open();
			}
		});

		if (server.getUsername() == null || server.getUsername().length() == 0 || server.getPassword() == null
				|| server.getPassword().length() == 0 || server.getUrl() == null || server.getUrl().length() == 0) {
			throw new OperationCanceledException();
		}
	}

	@Override
	public DeploymentDescriptor prepareForDeployment(final CloudFoundryServer server,
			final ApplicationModule appModule, final IProgressMonitor monitor) {
		final DeploymentDescriptor descriptor = new DeploymentDescriptor();
		CloudApplication existingApp = appModule.getApplication();
		if (existingApp != null) {
			descriptor.applicationInfo = new ApplicationInfo(existingApp.getName());
			descriptor.deploymentInfo = new DeploymentInfo();
			descriptor.deploymentInfo.setUris(existingApp.getUris());
			descriptor.deploymentMode = ApplicationAction.START;

			DeploymentInfo lastDeploymentInfo = appModule.getLastDeploymentInfo();
			if (lastDeploymentInfo != null) {
				descriptor.deploymentInfo.setServices(lastDeploymentInfo.getServices());
			}
		}
		else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					CloudFoundryApplicationWizard wizard = new CloudFoundryApplicationWizard(server, appModule);
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
							.getShell(), wizard);
					int status = dialog.open();
					if (status == Dialog.OK) {
						descriptor.applicationInfo = wizard.getApplicationInfo();
						descriptor.deploymentInfo = wizard.getDeploymentInfo();
						descriptor.deploymentMode = wizard.getDeploymentMode();

						// First add any new services to the server
						final List<CloudService> addedServices = wizard.getAddedCloudServices();

						if (!addedServices.isEmpty()) {
							IProgressMonitor subMonitor = new SubProgressMonitor(monitor, addedServices.size());
							try {
								server.getBehaviour().createService(addedServices.toArray(new CloudService[0]),
										subMonitor);
							}
							catch (CoreException e) {
								CloudFoundryPlugin.logError(e);
							}
							finally {
								subMonitor.done();
							}
						}

						// Now set any selected services, which may include past
						// deployed services
						List<String> selectedServices = wizard.getSelectedCloudServicesID();

						descriptor.deploymentInfo.setServices(selectedServices);
					}
				}
			});
		}
		if (descriptor.deploymentInfo == null) {
			throw new OperationCanceledException();
		}
		return descriptor;
	}

	@Override
	public void deleteServices(final List<String> services, final CloudFoundryServer cloudServer) {
		if (services == null || services.isEmpty()) {
			return;
		}

		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				DeleteServicesWizard wizard = new DeleteServicesWizard(cloudServer, services);
				WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
				dialog.open();
			}

		});
	}

}
