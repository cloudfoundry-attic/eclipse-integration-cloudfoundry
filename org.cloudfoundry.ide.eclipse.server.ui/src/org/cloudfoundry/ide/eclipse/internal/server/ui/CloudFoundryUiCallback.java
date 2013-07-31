/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.RepublishModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContents;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.CaldecottUIHelper;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardProviderDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryCredentialsWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.DeleteServicesWizard;
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
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IModule;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryUiCallback extends CloudFoundryCallback {

	@Override
	public void applicationStarted(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {
		// RUn this in the UI thread as it may be invoked by a worker thread
		UIJob job = new UIJob("Updating Cloud Foundry console") {

			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {
				for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
					ConsoleContents content = ConsoleContents.getStandardLogContent(server,
							cloudModule.getApplication(), i);
					ConsoleManager.getInstance().startConsole(server, content, cloudModule.getApplication(), i, i == 0,
							true);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();

	}

	public static String getStagingInitialContent(CloudApplication cloudModule, CloudFoundryServer cloudServer) {
		StringBuffer initialContent = new StringBuffer();
		initialContent.append("Staging application ");
		initialContent.append(cloudModule.getName());
		initialContent.append(' ');
		initialContent.append("in server ");
		initialContent.append(cloudServer.getDeploymentName());
		initialContent.append('\n');
		initialContent.append("Please wait while staging completes...");
		initialContent.append('\n');
		return initialContent.toString();
	}

	@Override
	public void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// Only show staging for v2 servers
		// String stagingLogURL = cloudModule.getStartingInfo() != null ?
		// cloudModule.getStartingInfo().getStagingFile()
		// : null;
		//
		// if (stagingLogURL != null) {
		// for (int i = 0; i < cloudModule.getApplication().getInstances(); i++)
		// {
		// if (server.getBehaviour().supportsSpaces()) {
		// String initialContent =
		// ConsoleContent.getStagingInitialContent(cloudModule.getApplication(),
		// server);
		// ConsoleContent consoleContent = ConsoleContent.getConsoleContent(
		// Arrays.asList(new FileContent(stagingLogURL, true, server, true)),
		// initialContent);
		//
		// ConsoleManager.getInstance().startConsole(server, consoleContent,
		// cloudModule.getApplication(), i,
		// i == 0);
		// }
		// }
		// }

	}

	@Override
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		applicationStopped(cloudModule, cloudServer);
	}

	public void applicationStopped(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
			ConsoleManager.getInstance().stopConsole(cloudServer.getServer(), cloudModule.getApplication(), i);
		}
	}

	public void displayCaldecottTunnelConnections(CloudFoundryServer cloudServer,
			List<CaldecottTunnelDescriptor> descriptors) {

		if (descriptors != null && !descriptors.isEmpty()) {
			List<String> serviceNames = new ArrayList<String>();

			for (CaldecottTunnelDescriptor descriptor : descriptors) {
				serviceNames.add(descriptor.getServiceName());
			}
			new CaldecottUIHelper(cloudServer).displayCaldecottTunnels(serviceNames);
		}
	}

	@Override
	public void applicationStopping(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {

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
			final CloudFoundryApplicationModule appModule, final IProgressMonitor monitor) {

		DeploymentDescriptor descriptor = null;
		CloudApplication existingApp = appModule.getApplication();
		if (existingApp != null) {
			descriptor = new DeploymentDescriptor();
			descriptor.applicationInfo = new ApplicationInfo(existingApp.getName());
			descriptor.deploymentInfo = new DeploymentInfo();
			descriptor.deploymentInfo.setUris(existingApp.getUris());
			descriptor.deploymentMode = ApplicationAction.START;

			// FIXNS_STANDALONE: uncomment when CF client supports staging
			// descriptor.staging = getStaging(appModule);

			DeploymentInfo lastDeploymentInfo = appModule.getLastDeploymentInfo();
			if (lastDeploymentInfo != null) {
				descriptor.deploymentInfo.setServices(lastDeploymentInfo.getServices());
			}
		}
		else {
			IModule module = appModule.getLocalModule();

			RepublishModule repModule = CloudFoundryPlugin.getModuleCache().getData(server.getServerOriginal())
					.untagForAutomaticRepublish(module);

			// First check if the module is set for automatic republish. This is
			// different than a
			// start or update restart, as the latter two require the module to
			// already be published.
			// Automatic republish are for modules that are configured for
			// deployment but failed to
			// publish the first time. In this case it is unnecessary to open
			// the
			// deployment wizard
			// as all the configuration is already available in an existing
			// descriptor
			if (repModule != null) {
				descriptor = repModule.getDeploymentDescriptor();
			}

			if (!isValidDescriptor(descriptor, module)) {
				final DeploymentDescriptor[] depDescriptors = new DeploymentDescriptor[1];
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						ApplicationWizardProviderDelegate providerDelegate = ApplicationWizardRegistry
								.getWizardProvider(appModule.getLocalModule());
						if (providerDelegate == null) {
							CloudFoundryPlugin.logError("Failed to open application wizard for: "
									+ appModule.getApplicationId() + " when attempting to push application to "
									+ server.getServer().getName()
									+ ". No application provider found that corresponds to the application type: "
									+ appModule.getLocalModule().getModuleType().getId());
							return;
						}

						CloudFoundryApplicationWizard wizard = new CloudFoundryApplicationWizard(server, appModule,
								providerDelegate);
						WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
								.getShell(), wizard);
						int status = dialog.open();
						if (status == Dialog.OK) {
							DeploymentDescriptor descriptorToUpdate = new DeploymentDescriptor();
							descriptorToUpdate.applicationInfo = wizard.getApplicationInfo();
							descriptorToUpdate.deploymentInfo = wizard.getDeploymentInfo();
							descriptorToUpdate.deploymentMode = wizard.getDeploymentMode();
							descriptorToUpdate.applicationPlan = wizard.getApplicationPlan();

							descriptorToUpdate.staging = wizard.getStaging();
							// First add any new services to the server
							final List<CloudService> addedServices = wizard.getCreatedCloudServices();

							if (addedServices != null && !addedServices.isEmpty()) {
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

							// Now set any selected services, which may
							// include
							// past
							// deployed services
							List<String> selectedServices = wizard.getSelectedServicesForBinding();

							descriptorToUpdate.deploymentInfo.setServices(selectedServices);
							depDescriptors[0] = descriptorToUpdate;
						}
					}
				});
				descriptor = depDescriptors[0];
			}
		}

		if (descriptor == null || descriptor.deploymentInfo == null) {
			throw new OperationCanceledException();
		}
		return descriptor;
	}

	protected boolean isValidDescriptor(DeploymentDescriptor descriptor, IModule module) {
		if (descriptor == null) {
			return false;
		}
		IApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(module);
		if (delegate != null) {
			return delegate.isValidDescriptor(descriptor);
		}
		return false;
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
