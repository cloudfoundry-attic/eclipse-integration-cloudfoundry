/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.server.ui.internal.console.ConsoleManagerRegistry;
import org.cloudfoundry.ide.eclipse.server.ui.internal.console.StandardLogContentType;
import org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel.CaldecottUIHelper;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryCredentialsWizard;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.DeleteServicesWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryUiCallback extends CloudFoundryCallback {

	@Override
	public void applicationStarted(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {

	}

	@Override
	public void startApplicationConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {
		if (cloudModule == null || cloudModule.getApplication() == null) {
			CloudFoundryPlugin
					.logError("No application content to display to the console while starting application in the Cloud Foundry server."); //$NON-NLS-1$
			return;
		}
		if (showIndex < 0) {
			showIndex = 0;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, cloudModule.getDeploymentInfo().getInstances()*100);
		
		subMonitor.subTask(NLS.bind(Messages.CloudFoundryUiCallback_STARTING_CONSOLE, cloudModule.getDeployedApplicationName()));

		for (int i = 0; i < cloudModule.getDeploymentInfo().getInstances(); i++) {
			// Do not clear the console as pre application start information may
			// have been already sent to the console
			// output
			boolean shouldClearConsole = false;
			
			ConsoleManagerRegistry.getConsoleManager(cloudServer)
					.startConsole(cloudServer, StandardLogContentType.APPLICATION_LOG, cloudModule, i, i == showIndex,
							shouldClearConsole, subMonitor.newChild(100));
		}
	}

	@Override
	public void showCloudFoundryLogs(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {
		ConsoleManagerRegistry.getConsoleManager(cloudServer).showCloudFoundryLogs(cloudServer, cloudModule, showIndex,
				false, monitor);
	}

	@Override
	public void printToConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			String message, boolean clearConsole, boolean isError) {
		ConsoleManagerRegistry.getConsoleManager(cloudServer).writeToStandardConsole(message, cloudServer, cloudModule,
				0, clearConsole, isError);
	}

	@Override
	public void trace(CloudLog log, boolean clear) {
		ConsoleManagerRegistry.getInstance().trace(log, clear);
	}

	@Override
	public void showTraceView(boolean showTrace) {
		if (showTrace) {
			ConsoleManagerRegistry.getInstance().setTraceConsoleVisible();
		}
	}

	@Override
	public void applicationStarting(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {

		// Only show the starting info for the first instance that is shown.
		// Not necessary to show staging
		// for instances that are not shown in the console.
		// FIXNS: Streaming of staging logs no longer works using CF client for
		// CF 1.6.0. Disabling til future
		// if (cloudModule.getStartingInfo() != null &&
		// cloudModule.getStartingInfo().getStagingFile() != null
		// && cloudModule.getApplication().getInstances() > 0) {
		//
		// boolean clearConsole = false;
		// StagingLogConsoleContent stagingContent = new
		// StagingLogConsoleContent(cloudModule.getStartingInfo(),
		// server);
		// ConsoleManager.getInstance().startConsole(server, new
		// ConsoleContents(stagingContent),
		// cloudModule.getApplication(), 0, true, clearConsole);
		//
		// }

	}

	@Override
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		stopApplicationConsole(cloudModule, cloudServer);
	}

	public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		if (cloudModule == null) {
			return;
		}

		// If application is not deployed (i.e., there are no application
		// instances), still stop the console as
		// a console may have been created for the application even if it failed
		// to deploy or start.
		int totalInstances = cloudModule.isDeployed() ? cloudModule.getApplication().getInstances() : 0;
		int instance = 0;
		do {
			ConsoleManagerRegistry.getConsoleManager(cloudServer).stopConsole(cloudServer.getServer(), cloudModule,
					instance);
			++instance;
		} while (instance < totalInstances);

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
	public void disconnecting(CloudFoundryServer cloudServer) {
		ConsoleManagerRegistry.getConsoleManager(cloudServer).stopConsoles();
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
	public DeploymentConfiguration prepareForDeployment(CloudFoundryServer server,
			CloudFoundryApplicationModule module, IProgressMonitor monitor) throws CoreException {
		return new ApplicationDeploymentUIHandler().prepareForDeployment(server, module, monitor);
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

	@Override
	public void handleError(final IStatus status) {

		if (status != null && status.getSeverity() == IStatus.ERROR) {

			UIJob job = new UIJob(Messages.CloudFoundryUiCallback_JOB_CF_ERROR) {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					Shell shell = CloudUiUtil.getShell();
					if (shell != null) {
						new MessageDialog(shell, Messages.CloudFoundryUiCallback_ERROR_CALLBACK_TITLE, null,
								status.getMessage(), MessageDialog.ERROR, new String[] { Messages.COMMONTXT_OK }, 0)
								.open();
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();

		}
	}

}
