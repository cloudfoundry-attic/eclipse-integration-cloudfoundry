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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
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

public class UnmapProjectCommand extends BaseCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);

		final CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer.loadAdapter(
				CloudFoundryServer.class, null);
		final CloudFoundryApplicationModule existingApp = cloudServer.getExistingCloudModule(selectedModule);

		final ICloudFoundryOperation op = cloudServer.getBehaviour().operations()
				.unmapProject(existingApp, cloudServer);

		Job job = new Job("Unmap Project") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.run(monitor);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();

		return null;
	}

}
