/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelServiceCommandStore;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.ModifyServicesForApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.AddServiceStartCaldecottAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ExternalToolsCommandWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;

public class TunnelActionProvider {

	private final CloudFoundryServer cloudServer;

	public TunnelActionProvider(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	/**
	 * Returns a list of applicable Caldecott Actions given the selection, or
	 * empty list if not actions are applicable.
	 * @param selection
	 * @param editorPage
	 * @return non-null list of actions. May be empty.
	 */
	public List<IAction> getTunnelActions(IStructuredSelection selection,
			final CloudFoundryApplicationsEditorPage editorPage) {
		Collection<String> selectedServices = ModifyServicesForApplicationAction.getServiceNames(selection);
		List<IAction> actions = new ArrayList<IAction>();
		final TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
		if (selectedServices != null && !selectedServices.isEmpty()) {
			final List<String> servicesWithTunnels = new ArrayList<String>();
			final CaldecottUIHelper uiHelper = new CaldecottUIHelper(cloudServer);
			final List<String> servicesToAdd = uiHelper.getServicesWithNoTunnel(selectedServices, handler,
					servicesWithTunnels);

			if (!servicesToAdd.isEmpty()) {

				if (servicesWithTunnels.isEmpty()) {
					actions.add(new AddServiceStartCaldecottAction(servicesToAdd, cloudServer.getBehaviour(),
							editorPage, "Open Tunnel"));
				}

			}

			if (!servicesWithTunnels.isEmpty()) {

				actions.add(new DisconnectCaldecottTunnelAction(editorPage, handler, servicesWithTunnels));
				IAction showCaldecottTunnelInfo = new Action("Show Tunnel Information...", CloudFoundryImages.CONNECT) {
					public void run() {
						uiHelper.displayCaldecottTunnels(servicesWithTunnels);
					}
				};
				actions.add(showCaldecottTunnelInfo);

			}
		}

		// Add the external tools actions
		// If only one service is selected, check if there are external
		// tools to launch for that service
		List<CloudService> services = ModifyServicesForApplicationAction.getServices(selection);

		// Only show external tools launch actions for ONE selection
		if (services != null && services.size() == 1) {

			CloudService selectedService = services.get(0);

			// Check if a tunnel is open, if not, open one

			try {
				List<ServiceCommand> commands = TunnelServiceCommandStore.getCurrentStore().getCommandsForService(
						selectedService.getVendor(), true);
				if (commands != null && !commands.isEmpty()) {
					CaldecottTunnelDescriptor descriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
							cloudServer, selectedService.getName());

					for (ServiceCommand command : commands) {
						actions.add(descriptor != null ? new ExecuteTunnelExternalToolAction(editorPage, descriptor,
								command, cloudServer) : new ExecuteTunnelExternalToolAction(editorPage,
								selectedService, command, cloudServer));
					}
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError("Failed to load external tool launch commands for services.", e);
			}
		}

		actions.add(new ExternalToolsAction(editorPage, cloudServer));
		return actions;
	}

	static class DisconnectCaldecottTunnelAction extends CloudFoundryEditorAction {

		static final String ACTION_NAME = "Disconnect Tunnel";

		private final List<String> servicesWithTunnels;

		private final TunnelBehaviour handler;

		public DisconnectCaldecottTunnelAction(CloudFoundryApplicationsEditorPage editorPage, TunnelBehaviour handler,
				List<String> servicesWithTunnels) {
			super(editorPage, RefreshArea.ALL);
			setText(ACTION_NAME);
			setImageDescriptor(CloudFoundryImages.DISCONNECT);
			this.servicesWithTunnels = servicesWithTunnels;
			this.handler = handler;
		}

		@Override
		public String getJobName() {
			return ACTION_NAME;
		}

		@Override
		public IStatus performAction(IProgressMonitor monitor) throws CoreException {
			for (String serviceName : servicesWithTunnels) {
				try {
					handler.stopAndDeleteCaldecottTunnel(serviceName, monitor);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError("Failed to close tunnel for service: " + serviceName, e);
				}
			}

			return Status.OK_STATUS;
		}

	}

	static class ExternalToolsAction extends AbstractEditorAction {

		private final CloudFoundryServer cloudServer;

		protected ExternalToolsAction(CloudFoundryApplicationsEditorPage editorPage, CloudFoundryServer cloudServer) {
			super(editorPage);
			this.cloudServer = cloudServer;
			setText("External Tools...");
			setImageDescriptor(CloudFoundryImages.TUNNEL_EXTERNAL_TOOLS);
		}

		@Override
		protected String getJobName() {
			return "External Tools";
		}

		@Override
		protected UIJob getUIJob() {
			UIJob job = new UIJob("External Tools") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						TunnelServiceCommands commands = TunnelServiceCommandStore.getCurrentStore()
								.getTunnelServiceCommands();
						Shell shell = getShell();

						if (shell != null) {
							ExternalToolsCommandWizard wizard = new ExternalToolsCommandWizard(commands, cloudServer);
							WizardDialog dialog = new WizardDialog(getShell(), wizard);
							if (dialog.open() == Window.OK) {
								commands = wizard.getExternalToolLaunchCommandsServer();

								try {
									TunnelServiceCommandStore.getCurrentStore().storeServerServiceCommands(commands);
								}
								catch (CoreException e) {
									CloudFoundryPlugin.logError(e);
								}
							}
						}
					}
					catch (CoreException e) {
						CloudFoundryPlugin.logError(e);
					}
					return Status.OK_STATUS;
				}

			};

			job.setSystem(true);
			return job;
		}

		protected Shell getShell() {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}

	}

	static abstract class AbstractEditorAction extends Action {

		private final CloudFoundryApplicationsEditorPage editorPage;

		protected AbstractEditorAction(CloudFoundryApplicationsEditorPage editorPage) {
			this.editorPage = editorPage;
		}

		protected abstract String getJobName();

		protected abstract UIJob getUIJob();

		public void run() {

			UIJob job = getUIJob();

			IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) editorPage.getEditorSite()
					.getService(IWorkbenchSiteProgressService.class);
			if (service != null) {
				service.schedule(job, 0L, true);
			}
			else {
				job.schedule();
			}

		}
	}

}
