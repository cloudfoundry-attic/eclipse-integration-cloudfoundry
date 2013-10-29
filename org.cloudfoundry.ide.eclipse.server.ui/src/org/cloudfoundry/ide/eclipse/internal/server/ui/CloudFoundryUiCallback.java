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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContents;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.PreApplicationStartConsoleContent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.StagingLogConsoleContent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.CaldecottUIHelper;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryCredentialsWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.DeleteServicesWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
	public void applicationStarted(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {
		for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
			// Do not clear the console as pre application start information may
			// have been already sent to the console
			// output
			boolean shouldClearConsole = false;
			ConsoleContents content = ConsoleContents.getStandardLogContent(server, cloudModule.getApplication(), i);
			ConsoleManager.getInstance().startConsole(server, content, cloudModule.getApplication(), i, i == 0,
					shouldClearConsole);
		}

	}

	public void applicationAboutToStart(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// Not necessary to show staging
		// for instances that are not shown in the console, so just show the
		// first instance.
		if (cloudModule.getApplication().getInstances() > 0) {

			boolean clearConsole = true;
			PreApplicationStartConsoleContent content = new PreApplicationStartConsoleContent();
			ConsoleManager.getInstance().startConsole(server, new ConsoleContents(content),
					cloudModule.getApplication(), 0, true, clearConsole);

		}
	}

	@Override
	public void applicationStarting(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {

		// Only show the starting info for the first instance that is shown.
		// Not necessary to show staging
		// for instances that are not shown in the console.
		if (cloudModule.getStartingInfo() != null && cloudModule.getStartingInfo().getStagingFile() != null
				&& cloudModule.getApplication().getInstances() > 0) {

			boolean clearConsole = false;
			StagingLogConsoleContent stagingContent = new StagingLogConsoleContent(cloudModule.getStartingInfo(),
					server);
			ConsoleManager.getInstance().startConsole(server, new ConsoleContents(stagingContent),
					cloudModule.getApplication(), 0, true, clearConsole);

		}

	}

	@Override
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		stopApplicationConsole(cloudModule, cloudServer);
	}

	public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		if (cloudModule == null || cloudModule.getApplication() == null) {
			return;
		}
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
	public void prepareForDeployment(final CloudFoundryServer server, final CloudFoundryApplicationModule appModule,
			final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		new ApplicationDeploymentUIHandler().prepareForDeployment(server, appModule, monitor);
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
