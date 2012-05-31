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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.AddServiceStartCaldecottAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CaldecottTunnelInfoDialog;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CaldecottTunnelWizard;
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
import org.eclipse.ui.progress.UIJob;

public class CaldecottUIHelper {

	private final CloudFoundryServer cloudServer;

	public CaldecottUIHelper(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	public void openCaldecottTunnelWizard() {

		UIJob uiJob = new UIJob("Show Caldecott Tunnels") {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				Shell shell = getShell();

				CaldecottTunnelWizard wizard = new CaldecottTunnelWizard(cloudServer);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				if (dialog.open() == Window.OK) {

					Set<CaldecottTunnelDescriptor> descriptorsToRemove = wizard.getDescriptorsToRemove();
					if (descriptorsToRemove != null) {
						for (CaldecottTunnelDescriptor descriptor : descriptorsToRemove) {
							try {
								new CaldecottTunnelHandler(cloudServer).stopAndDeleteCaldecottTunnel(
										descriptor.getServiceName(), monitor);
							}
							catch (CoreException e) {
								CloudFoundryPlugin.logError(e);
							}
						}
					}
				}
				return Status.OK_STATUS;
			}

		};
		uiJob.schedule();
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	public void displayCaldecottTunnels(List<String> srcNames) {

		if (srcNames != null && !srcNames.isEmpty()) {
			final List<String> serviceNames = srcNames;
			UIJob job = new UIJob("Display Caldecott Info...") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					CaldecottTunnelInfoDialog dialogue = new CaldecottTunnelInfoDialog(getShell(), cloudServer,
							serviceNames);
					dialogue.open();
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}

	}

	/**
	 * Returns a list of applicable Caldecott Actions given the selection, or
	 * empty list if not actions are applicable.
	 * @param selection
	 * @param editorPage
	 * @return non-null list of actions. May be empty.
	 */
	public List<IAction> getCaldecottActions(IStructuredSelection selection,
			final CloudFoundryApplicationsEditorPage editorPage) {
		Collection<String> selectedServices = ModifyServicesForApplicationAction.getServiceNames(selection);
		List<IAction> actions = new ArrayList<IAction>();
		final CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		if (selectedServices != null && !selectedServices.isEmpty()) {
			final List<String> servicesWithTunnels = new ArrayList<String>();
			final List<String> servicesToAdd = getServicesWithNoTunnel(selectedServices, handler, servicesWithTunnels);

			if (!servicesToAdd.isEmpty()) {
				actions.add(new AddServiceStartCaldecottAction(servicesToAdd, cloudServer.getBehaviour(), editorPage,
						"Open Caldecott Tunnel"));
			}
			else if (!servicesWithTunnels.isEmpty()) {

				actions.add(new DisconnectCaldecottTunnelAction(editorPage, handler, servicesWithTunnels));
				IAction showCaldecottTunnelInfo = new Action("Show Caldecott Tunnel info...",
						CloudFoundryImages.CONNECT) {
					public void run() {
						displayCaldecottTunnels(servicesWithTunnels);
					}
				};
				actions.add(showCaldecottTunnelInfo);
			}
		}
		return actions;
	}

	public List<String> getServicesWithNoTunnel(Collection<String> selectedServices, CaldecottTunnelHandler handler,
			List<String> servicesWithTunnels) {
		List<String> filteredInServices = new ArrayList<String>();
		for (String serviceName : selectedServices) {
			if (!handler.hasCaldecottTunnel(serviceName)) {
				filteredInServices.add(serviceName);
			}
			else {
				servicesWithTunnels.add(serviceName);
			}
		}
		return filteredInServices;
	}

	static class DisconnectCaldecottTunnelAction extends CloudFoundryEditorAction {

		static final String ACTION_NAME = "Disconnect Caldecott Tunnel";

		private final List<String> servicesWithTunnels;

		private final CaldecottTunnelHandler handler;

		public DisconnectCaldecottTunnelAction(CloudFoundryApplicationsEditorPage editorPage,
				CaldecottTunnelHandler handler, List<String> servicesWithTunnels) {
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
					CloudFoundryPlugin.logError("Failed to close Caldecott tunnel for service: " + serviceName, e);
				}
			}

			return Status.OK_STATUS;
		}

	}

}
