/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.OrgsAndSpacesWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.UIJob;

/**
 * Clones an existing CF server instance, using the same credentials and server
 * URL, but prompting the user for another space
 */
public class CloneServerAction extends AbstractCloudFoundryServerAction {

	private IWorkbenchPart activePart;

	protected String getJobName() {
		return "Cloning serve to selected space";
	}

	public void doRun(final CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		final Shell shell = activePart != null && activePart.getSite() != null ? activePart.getSite().getShell()
				: CloudUiUtil.getShell();

		if (shell != null) {
			UIJob job = new UIJob(getJobName()) {

				public IStatus runInUIThread(IProgressMonitor monitor) {
					OrgsAndSpacesWizard wizard = new OrgsAndSpacesWizard(cloudServer);
					WizardDialog dialog = new WizardDialog(shell, wizard);
					dialog.open();

					return Status.OK_STATUS;
				}
			};

			job.schedule();
		}
		else {
			CloudFoundryPlugin.logError("Unable to find an active shell to open the orgs and spaces wizard.");
		}

	}

	protected void serverSelectionChanged(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			IAction action) {
		action.setEnabled(server != null);
	}

	public void setActivePart(IAction action, IWorkbenchPart activePart) {
		this.activePart = activePart;
	}

}
