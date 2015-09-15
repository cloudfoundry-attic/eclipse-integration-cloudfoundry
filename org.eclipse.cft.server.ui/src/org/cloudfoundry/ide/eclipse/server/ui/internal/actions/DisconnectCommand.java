/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public class DisconnectCommand extends BaseCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		final CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer.loadAdapter(CloudFoundryServer.class, null);
		Job disconnectJob = new Job(Messages.DisconnectCommand_JOB_DISCONN_SERVER) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					cloudServer.getBehaviour().disconnect(monitor);
				}
				catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				catch (CoreException e) {
//					Trace.trace(Trace.STRING_SEVERE, "Error calling disconnect() ", e);
					return new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, NLS.bind(
							"Failed to disconnect from server: {0}", e.getMessage())); //$NON-NLS-1$
				}
				return Status.OK_STATUS;
			}
		};
		disconnectJob.schedule();		
		return null;
	}

	
}
