/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelServiceCommandStore;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.ModifyServicesForApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.AddServiceStartCaldecottAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

public class TunnelActionProvider {

	private final CloudFoundryServer cloudServer;

	public static final String DISABLED_V2_TOOLTIP_MESSAGE = "Disabled for this version of Cloud Foundry Integration for Eclipse";

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

		CloudService selectedService = null;
		// Only show external tools launch actions for ONE selection
		if (services != null && services.size() == 1) {
			selectedService = services.get(0);
		}

		if (selectedService != null) {
			// See if there is an existing tunnel that is open
			CaldecottTunnelDescriptor descriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
					cloudServer, selectedService.getName());

			IAction dataToolsAction = DataToolsTunnelAction.getAction(editorPage, selectedService, descriptor,
					cloudServer);

			// Add connection to Eclipse data tools, if one exists for the given
			// service
			if (dataToolsAction != null) {
				actions.add(dataToolsAction);
			}

			try {
				List<ServiceCommand> commands = TunnelServiceCommandStore.getCurrentStore().getCommandsForService(
						selectedService, true);
				if (commands != null && !commands.isEmpty()) {

					for (ServiceCommand command : commands) {
						actions.add(descriptor != null ? new LaunchTunnelCommandAction(editorPage, descriptor, command,
								cloudServer) : new LaunchTunnelCommandAction(editorPage, selectedService, command,
								cloudServer));
					}
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError("Failed to load external tool launch commands for services.", e);
			}
		}

		actions.add(new CommandDefinitionActions(editorPage, selectedService));

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
		public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {

			return new ModifyEditorOperation() {

				@Override
				protected void performOperation(IProgressMonitor monitor) throws CoreException {
					for (String serviceName : servicesWithTunnels) {
						try {
							handler.stopAndDeleteCaldecottTunnel(serviceName, monitor);
						}
						catch (CoreException e) {
							CloudFoundryPlugin.logError("Failed to close tunnel for service: " + serviceName, e);
						}
					}
				}
			};
		}
	}
}
