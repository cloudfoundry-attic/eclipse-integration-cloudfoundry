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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.CloudFoundryProperties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * 
 * Update restart action invoked in Server's view context menu (used in Servers
 * view context menu extension point)
 * 
 */
public class UpdateRestartModuleAction extends AbstractCloudFoundryServerAction {
	
	private IModule[] selectedModule = null; 

	protected String getJobName() {
		return "Update and restarting module";
	}

	protected String getFailureMessage() {
		return "Unable to update and restart module";
	}

	protected void doRun(CloudFoundryServer server, CloudFoundryApplicationModule appModule, IAction action) {
		final CloudFoundryServer cloudServer = server;
		Job job = new Job(getJobName()) {

			protected IStatus run(IProgressMonitor monitor) {

				try {
					// FIXNS: Disabled debug for CF 1.5.0 until v2 supports
					// debug
					// if
					// (CloudFoundryProperties.isApplicationRunningInDebugMode.testProperty(modules,
					// getCloudFoundryServer())) {
					// cloudServer.getBehaviour().updateRestartDebugModule(modules,
					// getIncrementalPublish(), monitor);
					// }
					// else {
					// }

					cloudServer.getBehaviour().getUpdateRestartOperation(selectedModule).run(monitor);

				}
				catch (CoreException e) {
					CloudFoundryPlugin.getDefault().getLog()
							.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, getFailureMessage(), e));
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	protected boolean getIncrementalPublish() {
		return CloudFoundryPlugin.getDefault().getIncrementalPublish();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	protected void serverSelectionChanged(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule,
			IAction action) {

		if (cloudServer != null && (cloudServer.getServer().getServerState() == IServer.STATE_STARTED)) {
			if (appModule != null) {
				int state = appModule.getState();
				// Do not enable the action if the associated module project is
				// not accessible, as users shoudln't
				// be able to modify files within Eclipse if the project is not
				// accessible (i.e it is not open and writable in the workspace)
				selectedModule = new IModule[] { appModule.getLocalModule() };
				if (state == IServer.STATE_STARTED
						&& !appModule.isExternal()
						&& CloudFoundryProperties.isModuleProjectAccessible.testProperty(
								selectedModule, cloudServer)) {
					action.setEnabled(true);
					return;
				}
			}
		}
		action.setEnabled(false);

	}

}