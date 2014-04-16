/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudOperationsConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;

/**
 * Handles refresh operations across the Cloud Foundry Eclipse plugin, including
 * stopping and starting ongoing refresh operations, as well as notifying
 * listeners on a refresh event.
 * 
 */
public class RefreshHandler {

	protected long interval = -1;

	/**
	 * 
	 */

	private BehaviourRefreshJob refreshJob;

	private final CloudFoundryServer cloudServer;

	public RefreshHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		this.refreshJob = new BehaviourRefreshJob();
	}

	/**
	 * Stop a refresh operation at the next opportunity. There is no guarantee
	 * that the operation may stop immediately.
	 */
	public synchronized void stop() {
		doStart(-1);
	}

	/**
	 * Start refresh operation after the given interval.
	 * @param interval if interval > 0 operation will start. If interval <= -1,
	 * operation will stop at next available opportunity.
	 */
	public synchronized void start(long interval) {
		doStart(interval);
	}

	/**
	 * Starts a refresh operation. Implementing classes can specify a default
	 * interval time before the operation begins.
	 */
	public synchronized void start() {
		interval = CloudOperationsConstants.DEFAULT_INTERVAL;
		doStart(interval);
	}

	protected void doStart(long interval) {
		if (interval > -1) {
			refreshJob.schedule(interval);
		}
	}

	/**
	 * Notifies that a refresh has occurred. Implementing classes can determine
	 * the shape of the notification (i.e., if it fires a particular event
	 * defined by the implementing class, or another type), as well as how the
	 * firing takes places (e.g. whether a list of listeners in the implementing
	 * class are notified).
	 * @param monitor progress monitor. If invoked by the Cloud Foundry
	 * framework this should never be null.
	 */
	public synchronized void fireRefreshEvent(IProgressMonitor monitor) {
		// Do not create a Job for this, as this may be invoked many times, to
		// avoid many Jobs being scheduled.
		ServerEventHandler.getDefault().fireServerRefreshed(cloudServer);
	}

	protected void refreshFromJob(IProgressMonitor monitor) {
		if (shouldRefresh()) {
			fireRefreshEvent(monitor);
		}
	}

	protected synchronized boolean shouldRefresh() {
		return interval > -1;
	}

	/**
	 * Standard Behaviour refresh job, which refreshes the application modules
	 * through Behaviour API.
	 * 
	 */
	class BehaviourRefreshJob extends Job {

		public BehaviourRefreshJob() {
			super("Refresh Server Job");
			setSystem(true);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {

			if (RefreshHandler.this.cloudServer.getServer().getServerState() == IServer.STATE_STARTED) {
				RefreshHandler.this.refreshFromJob(monitor);
				if (interval > 0) {
					schedule(interval);
				}
			}

			return Status.OK_STATUS;

		}

	}

}