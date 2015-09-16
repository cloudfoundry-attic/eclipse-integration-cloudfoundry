/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.console.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;

/**
 * All the active streams for a given application instance that contribute
 * content to the same application console.
 * 
 *
 */
class ActiveStreams {

	private Map<IContentType, List<IConsoleJob>> activeStreams = new HashMap<IContentType, List<IConsoleJob>>();

	public ActiveStreams() {
	}

	public synchronized void close() {
		activeStreams.clear();
	}

	public synchronized void startTailing(List<IConsoleJob> jobs) {
		if (jobs == null) {
			return;
		}

		for (IConsoleJob job : jobs) {
			startTailing(job);
		}
	}

	public synchronized boolean contains(IConsoleJob job) {
		if (job == null) {
			return false;
		}

		List<IConsoleJob> jobsPerType = activeStreams.get(job.getContentType());

		return jobsPerType != null && jobsPerType.contains(job);
	}

	public synchronized IConsoleJob getFirst(IContentType contentType) {

		List<IConsoleJob> errorJobs = activeStreams.get(contentType);
		if (errorJobs != null && errorJobs.size() > 0) {
			return errorJobs.get(0);
		}
		return null;
	}

	/**
	 * Adds a console job, but does not schedule it.
	 * @param job to add
	 */
	public synchronized void addJob(IConsoleJob job) {

		IContentType contentType = job.getContentType();

		List<IConsoleJob> streamJobs = activeStreams.get(contentType);
		if (streamJobs == null) {
			streamJobs = new ArrayList<IConsoleJob>();
			activeStreams.put(contentType, streamJobs);
		}

		streamJobs.add(job);

	}

	/**
	 * 
	 * @param job to add. Will add, initialise and schedule the job.
	 */
	protected void startTailing(IConsoleJob job) {
		addJob(job);
		((Job) job).schedule();
	}

}