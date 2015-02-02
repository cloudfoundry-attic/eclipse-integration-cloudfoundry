/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 *     Steven Hung, IBM - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Logger;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.ServicesHandler;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.ServiceToApplicationsBindingWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.progress.UIJob;

public class ServiceToApplicationsBindingAction extends Action {

	private final CloudFoundryServerBehaviour serverBehaviour;

	private final ServicesHandler servicesHandler;

	private final CloudFoundryApplicationsEditorPage editorPage;

	public ServiceToApplicationsBindingAction(IStructuredSelection selection,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {

		this.serverBehaviour = serverBehaviour;
		this.editorPage = editorPage;

		setText(Messages.MANAGE_SERVICES_TO_APPLICATIONS_ACTION);
		servicesHandler = new ServicesHandler(selection);
	}

	@Override
	public void run() {
		UIJob uiJob = new UIJob(Messages.MANAGE_SERVICES_TO_APPLICATIONS_TITLE) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					if (serverBehaviour != null) {
						ServiceToApplicationsBindingWizard wizard = new ServiceToApplicationsBindingWizard(
								servicesHandler, serverBehaviour.getCloudFoundryServer(), editorPage);
						WizardDialog dialog = new WizardDialog(editorPage.getSite().getShell(), wizard);
						dialog.open();
					}
				}
				catch (CoreException e) {
					if (Logger.ERROR) {
						Logger.println(Logger.ERROR_LEVEL, this, "runInUIThread", "Error launching wizard", e); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				return Status.OK_STATUS;
			}

		};
		uiJob.setSystem(true);
		uiJob.setPriority(Job.INTERACTIVE);
		uiJob.schedule();
	}

}
