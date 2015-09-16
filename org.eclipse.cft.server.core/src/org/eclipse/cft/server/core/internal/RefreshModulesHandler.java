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
package org.eclipse.cft.server.core.internal;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.client.BehaviourOperation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Handles refresh of modules in a target Cloud Space.
 * <p/>
 * As refreshing modules may include fetch a list of {@link CloudApplication}
 * from the target Cloud space associated with the given
 * {@link CloudFoundryServer} which may be a long-running task, module refreshes
 * is performed asynchronously as a job, and only one job is scheduled per
 * behaviour regardless of the number of refresh requests received
 * 
 */
public class RefreshModulesHandler {

	private BehaviourRefreshJob refreshJob;

	private final CloudFoundryServer cloudServer;

	private BehaviourOperation opToRun;

	private static final String NO_SERVER_ERROR = "Null server in refresh module handler. Unable to schedule module refresh."; //$NON-NLS-1$

	/**
	 * 
	 * @param cloudServer may be null if not resolved.
	 */
	public RefreshModulesHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		String serverName = cloudServer != null ? cloudServer.getServer().getId() : "Unknown server"; //$NON-NLS-1$

		String refreshJobLabel = NLS.bind(Messages.RefreshModulesHandler_REFRESH_JOB, serverName);

		this.refreshJob = new BehaviourRefreshJob(refreshJobLabel);
	}

	/**
	 * Refresh all modules in the Cloud space, as well as services, but does not
	 * refresh the application instances information as that is triggered
	 * individually on a module selection to avoid a slow refresh
	 */
	public synchronized void scheduleRefreshAll() {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().refreshAll(null));
		}
	}

	public synchronized boolean isScheduled() {
		return this.opToRun != null;
	}

	/**
	 * Schedules to refresh all modules, services, as well as the instances for
	 * the given module, if not null. Passing a module is optional. If not
	 * module is passed, only a server-wide module refresh is performed.
	 * @param module to refresh
	 */
	public synchronized void scheduleRefreshAll(IModule module) {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().refreshAll(module));
		}
	}

	/**
	 * Schedule an application refresh to update the application's stats and
	 * information, including its instances. Does not perform a refresh on any
	 * other application, only on the given application module.
	 * @param module to refresh
	 */
	public synchronized void schedulesRefreshApplication(IModule module) {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().refreshApplication(module));
		}
	}

	/**
	 * Schedules an application refresh after there is a deployment change (app
	 * is started, stopped, restarted, or removed). This is meant to indicate a
	 * refresh is required after a long-running operation as opposed to
	 * {@link #schedulesRefreshApplication(IModule)} which is meant for updates
	 * on an existing application
	 * @param module
	 */
	public synchronized void scheduleRefreshForDeploymentChange(IModule module) {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().refreshForDeploymentChange(module));
		}
	}

	private synchronized void scheduleRefresh(BehaviourOperation opToRun) {
		if (this.opToRun == null) {
			this.opToRun = opToRun;
			schedule();
		}
	}

	private void schedule() {
		refreshJob.setSystem(false);
		refreshJob.schedule();
	}

	private class BehaviourRefreshJob extends Job {

		public BehaviourRefreshJob(String label) {
			super(label);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			try {
				CloudFoundryServer cloudServer = null;
				IModule module = opToRun.getModule();

				try {
					cloudServer = opToRun.getBehaviour() != null ? opToRun.getBehaviour().getCloudFoundryServer()
							: null;
				}
				catch (CoreException ce) {
					CloudFoundryPlugin.logError(ce);
				}

				try {
					opToRun.run(monitor);
				}
				catch (Throwable t) {
					// Cloud server must not be null as it's the source of
					// the event
					if (cloudServer == null) {
						CloudFoundryPlugin.logError(NLS.bind(Messages.RefreshModulesHandler_EVENT_CLOUD_SERVER_NULL,
								opToRun.getClass()));
					}
					else {
						ServerEventHandler.getDefault().fireError(cloudServer, module,
								CloudFoundryPlugin.getErrorStatus(Messages.RefreshModulesHandler_REFRESH_FAILURE, t));

					}
				}
			}
			finally {
				opToRun = null;
			}

			return Status.OK_STATUS;
		}
	}

}