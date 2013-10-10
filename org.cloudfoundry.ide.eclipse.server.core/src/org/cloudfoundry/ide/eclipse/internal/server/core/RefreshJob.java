/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;

/**
 * Refresh job for refreshing local server status to correspond to the actual
 * server status
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 */
public class RefreshJob extends Job {

	private static final long DEFAULT_INTERVAL = 60 * 1000;

	private long interval;

	private final CloudFoundryServer server;

	public RefreshJob(CloudFoundryServer server) {
		super("Refresh Server Job");
		setSystem(true);
		this.server = server;
		this.interval = DEFAULT_INTERVAL;
	}

	public synchronized void reschedule(long interval) {
		// schedule, if not already running or scheduled
		this.interval = interval;
		cancel();
		if (interval > 0) {
			schedule(interval);
		}
	}

	/**
	 * Initially runs the job without additional delay (aside from the job
	 * scheduler delays), and subsequent runs after the first run are executed
	 * after the specified interval.
	 * @param interval
	 */
	public synchronized void runAndReschedule(long interval) {
		this.interval = interval;
		cancel();
		schedule();
	}

	@Override
	protected synchronized IStatus run(IProgressMonitor monitor) {
		if (interval > 0) {
			try {
				server.getBehaviour().refreshModules(monitor);

				if (server.getServer().getServerState() == IServer.STATE_STARTED) {
					schedule(interval);
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.getDefault().getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, "Refresh of server failed", e));
			}
		}

		return Status.OK_STATUS;
	}
}
