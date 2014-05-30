/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.OrgsAndSpacesWizard;
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
		return "Cloning server to selected space";
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
			job.setSystem(true);
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
