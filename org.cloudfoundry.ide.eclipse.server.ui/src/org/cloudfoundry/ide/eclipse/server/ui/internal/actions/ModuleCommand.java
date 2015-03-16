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
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public abstract class ModuleCommand extends BaseCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		final CloudFoundryServer cloudServer = selectedServer != null ? (CloudFoundryServer) selectedServer
				.loadAdapter(CloudFoundryServer.class, null) : null;
		CloudFoundryApplicationModule appModule = cloudServer != null && selectedModule != null ? cloudServer
				.getExistingCloudModule(selectedModule) : null;
		if (selectedServer == null) {
			CloudFoundryPlugin.logError("No Cloud Foundry server instance available to run the selected action."); //$NON-NLS-1$
		}
		else if (appModule == null) {
			CloudFoundryPlugin.logError("No Cloud module resolved for the given selection."); //$NON-NLS-1$
		}
		else {

			final ICloudFoundryOperation op = getCloudOperation(appModule, cloudServer);

			if (op != null) {
				Job job = new Job("Module Command") //$NON-NLS-1$
				{
					protected IStatus run(IProgressMonitor monitor) {
						try {
							op.run(monitor);
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
			else {
				CloudFoundryPlugin.logError("No operation resolved to run in this action"); //$NON-NLS-1$
			}
		}
		return null;
	}

	abstract protected ICloudFoundryOperation getCloudOperation(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer);

}
