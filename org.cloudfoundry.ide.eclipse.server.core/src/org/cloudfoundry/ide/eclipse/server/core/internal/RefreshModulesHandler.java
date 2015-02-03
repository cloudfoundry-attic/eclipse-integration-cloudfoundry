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
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Handles refresh of modules in a target Cloud Space. Fires refresh events when
 * refresh operations complete successfully
 * <p/>
 * As refreshing modules may include fetch a list of {@link CloudApplication}
 * from the target Cloud space associated with the given
 * {@link CloudFoundryServer} which may be a long-running task, module refreshes
 * is performed asynchronously as a job.
 * 
 */
public class RefreshModulesHandler {

	private BehaviourRefreshJob refreshJob;

	private final CloudFoundryServer cloudServer;

	private ICloudFoundryOperation opToRun;

	private boolean forceStop = false;

	public RefreshModulesHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		this.refreshJob = new BehaviourRefreshJob();
	}

	/**
	 * Refresh all modules in the Cloud space. Refresh is performed
	 * asynchronously as it may be long running. Once refresh is complete a
	 * {@link CloudServerEvent#EVENT_SERVER_REFRESHED} event is fired, notifying
	 * any listeners that refresh has completed.
	 */
	public synchronized void scheduleRefresh() {
		if (!forceStop && this.opToRun == null) {
			this.opToRun = cloudServer.getBehaviour().operations().refreshAll(null);
			refreshJob.schedule();
		}
	}

	public synchronized void scheduleRefresh(ICloudFoundryOperation opToRun) {
		if (!forceStop && this.opToRun == null) {
			this.opToRun = opToRun;
			refreshJob.schedule();
		}
	}

	public synchronized void stop(boolean forceStop) {
		this.forceStop = forceStop;
	}

	public void fireRefreshEvent(IModule module) {
		ServerEventHandler.getDefault().fireApplicationChanged(cloudServer, module);
	}

	private class BehaviourRefreshJob extends Job {

		public BehaviourRefreshJob() {
			super("Refresh Server Job"); //$NON-NLS-1$
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {

			try {
				opToRun.run(monitor);
			}
			catch (Throwable t) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_FAILED_MODULE_REFRESH, t.getMessage()));
			}
			finally {
				opToRun = null;
			}

			return Status.OK_STATUS;
		}
	}

}