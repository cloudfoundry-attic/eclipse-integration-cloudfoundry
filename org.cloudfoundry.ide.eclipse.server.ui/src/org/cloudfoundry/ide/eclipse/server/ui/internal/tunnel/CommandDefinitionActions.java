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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceInfo;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.TunnelServiceCommandStore;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.TunnelCommandDefinitionWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;

/**
 * 
 * Launches wizard to edit commands for service tunnels.
 * 
 */
class CommandDefinitionActions extends Action {

	private final CloudFoundryApplicationsEditorPage editorPage;

	private final CloudService serviceContext;

	protected CommandDefinitionActions(CloudFoundryApplicationsEditorPage editorPage, CloudService serviceContext) {
		this.editorPage = editorPage;
		this.serviceContext = serviceContext;
		setText(Messages.CommandDefinitionActions_TEXT_CMD_DEFS);
		setImageDescriptor(CloudFoundryImages.TUNNEL_EXTERNAL_TOOLS);
		
		/**
		 * FIXNS: Disabled for CF 1.5.0 until tunnel support at client level are updated.
		 */
		setEnabled(false);
		setToolTipText(TunnelActionProvider.DISABLED_V2_TOOLTIP_MESSAGE);
	}

	protected String getJobName() {
		return Messages.CommandDefinitionActions_TEXT_CMD_DEF;
	}

	public void run() {

		UIJob job = getUIJob();

		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) editorPage.getEditorSite().getService(
				IWorkbenchSiteProgressService.class);
		if (service != null) {
			service.schedule(job, 0L, true);
		}
		else {
			job.schedule();
		}

	}

	protected UIJob getUIJob() {
		UIJob job = new UIJob(Messages.CommandDefinitionActions_TEXT_CMD_DEF) {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					ITunnelServiceCommands commands = TunnelServiceCommandStore.getCurrentStore()
							.getTunnelServiceCommands();
					Shell shell = getShell();

					if (shell != null) {
						ServiceInfo serviceInfo = null;

						if (serviceContext != null) {
							String vendor = CloudUtil.getServiceVendor(serviceContext);
							if (vendor != null) {

								for (ServiceInfo info : ServiceInfo.values()) {
									if (info.name().equals(vendor)) {
										serviceInfo = info;
										break;
									}
								}
							}
						}
						TunnelCommandDefinitionWizard wizard = new TunnelCommandDefinitionWizard(commands, serviceInfo);
						WizardDialog dialog = new WizardDialog(getShell(), wizard);
						if (dialog.open() == Window.OK) {
							commands = wizard.getExternalToolLaunchCommandsServer();

							try {
								TunnelServiceCommandStore.getCurrentStore().storeServerServiceCommands(commands);
							}
							catch (CoreException e) {
								CloudFoundryPlugin.log(e);
							}
						}
					}
				}
				catch (CoreException e) {
					CloudFoundryPlugin.log(e);
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