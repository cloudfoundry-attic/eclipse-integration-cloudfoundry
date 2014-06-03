/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License; you may not use this file except in compliance 
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
 *     Keith Chong, IBM - Support more general branded server type IDs via org.eclipse.ui.menus
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IModule;

public class UpdateRestartModuleCommand extends BaseCommandHandler {


	private String getJobName() {
		return "Update and restarting module";
	}

	private String getFailureMessage() {
		return "Unable to update and restart module";
	}
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		String error = null;
		CloudFoundryServer cloudServer = selectedServer != null ? (CloudFoundryServer) selectedServer.loadAdapter(
				CloudFoundryServer.class, null) : null;
		CloudFoundryApplicationModule appModule = cloudServer != null && selectedModule != null ? cloudServer
				.getExistingCloudModule(selectedModule) : null;
		if (selectedServer == null) {
			error = "No Cloud Foundry server instance available to run the selected action.";
		}

		if (error == null) {
			doRun(cloudServer, appModule);
		}
		else {
			CloudFoundryPlugin.logError(error);
		}

		return null;
	}
	
	protected void doRun(CloudFoundryServer server, CloudFoundryApplicationModule appModule) {
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

					cloudServer.getBehaviour().getUpdateRestartOperation(new IModule[] {selectedModule}).run(monitor);

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


}