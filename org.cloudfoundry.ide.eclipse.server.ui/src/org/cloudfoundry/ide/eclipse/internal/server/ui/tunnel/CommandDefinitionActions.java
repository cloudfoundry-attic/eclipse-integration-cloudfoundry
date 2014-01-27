/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelServiceCommandStore;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.TunnelCommandDefinitionWizard;
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
		setText("Command Definitions...");
		setImageDescriptor(CloudFoundryImages.TUNNEL_EXTERNAL_TOOLS);
		
		/**
		 * FIXNS: Disabled for CF 1.5.0 until tunnel support at client level are updated.
		 */
		setEnabled(false);
		setToolTipText(TunnelActionProvider.DISABLED_V2_TOOLTIP_MESSAGE);
	}

	protected String getJobName() {
		return "Command Definitions";
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
		UIJob job = new UIJob("Command Definitions") {

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