/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

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