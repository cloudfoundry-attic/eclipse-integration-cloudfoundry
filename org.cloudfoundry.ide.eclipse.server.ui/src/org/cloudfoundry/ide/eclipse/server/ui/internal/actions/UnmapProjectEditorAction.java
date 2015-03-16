/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.wst.server.core.IModule;

public class UnmapProjectEditorAction extends Action {
	private final CloudFoundryApplicationsEditorPage editorPage;

	private final IModule module;

	public UnmapProjectEditorAction(CloudFoundryApplicationsEditorPage editorPage, IModule module) {

		setText(Messages.UnmapProjectEditorAction_ACTION_LABEL);

		this.editorPage = editorPage;
		this.module = module;
	}

	@Override
	public void run() {
		final CloudFoundryServer cloudServer = editorPage.getCloudServer();
		final CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
		if (appModule != null && editorPage.getSite() != null && editorPage.getSite().getShell() != null) {

			Job job = new Job("Module Command") //$NON-NLS-1$
			{
				protected IStatus run(IProgressMonitor monitor) {
					try {
						new UnmapProjectOperation(appModule, editorPage.getCloudServer()).run(monitor);
					}
					catch (CoreException e) {
						CloudFoundryPlugin.logError(e);
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}

	}
}
