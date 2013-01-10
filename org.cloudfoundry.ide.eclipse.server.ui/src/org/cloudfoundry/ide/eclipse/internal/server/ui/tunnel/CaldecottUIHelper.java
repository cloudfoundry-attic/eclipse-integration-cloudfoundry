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
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CaldecottTunnelWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

		UIJob uiJob = new UIJob("Show Tunnels") {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				Shell shell = getShell();

				CaldecottTunnelWizard wizard = new CaldecottTunnelWizard(cloudServer);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				if (dialog.open() == Window.OK) {

					Set<CaldecottTunnelDescriptor> descriptorsToRemove = wizard.getDescriptorsToRemove();
					if (descriptorsToRemove != null) {
						for (CaldecottTunnelDescriptor descriptor : descriptorsToRemove) {
							try {
								new TunnelBehaviour(cloudServer).stopAndDeleteCaldecottTunnel(
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
			UIJob job = new UIJob("Display Tunnel Information") {

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

	public List<String> getServicesWithNoTunnel(Collection<String> selectedServices, TunnelBehaviour handler,
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



}
