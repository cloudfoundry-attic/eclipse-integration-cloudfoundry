/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
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
								CloudFoundryPlugin.log(e);
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
