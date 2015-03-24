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
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public abstract class UpdateMappingCommand extends ModuleCommand {

	@Override
	protected void run(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		final ICloudFoundryOperation op = getCloudOperation(appModule, cloudServer);

		if (op != null) {
			Job job = new Job(NLS.bind(Messages.UPDATE_PROJECT_MAPPING, appModule.getDeployedApplicationName())) {
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
			job.schedule();
		}
		else {
			CloudFoundryPlugin.logError("No operation resolved to run in this action"); //$NON-NLS-1$
		}
	}

	abstract protected ICloudFoundryOperation getCloudOperation(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer);
}
