/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
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
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * This schedules various jobs for streaming content to the Eclipse console for
 * a particular application instance deployed to a Cloud Foundry server. There
 * is one Cloud Foundry console per application instance. Some jobs scheduled
 * are for tailing content for remote log files, others may be for local
 * standard out and error content that is written from local content (e.g. a CF
 * Eclipse component that wishes to write something to the console related to
 * the deployed application instance).
 * <p/>
 * The tailing console jobs for remote log files work on the principle of a
 * self-scheduling,which continues to schedule itself to check for new content
 * in remote files whose content are streamed to the console.
 * <p/>
 * The console job is passed a console content , which contains a list of files,
 * each wrapped around a streaming abstraction, that need to be polled during
 * the active life of the job. The console job itself does not know how to
 * stream the file contents, and it delegates to the file streaming abstraction
 * to actually write to the console. The role of the console job is to decided
 * when to schedule itself, when to stop, and manage the console output streams.
 * 
 * <p/>
 * The job continues to run (i.e. schedule itself) after some interval until it
 * is told to stop by the CF server controller, most likely under these
 * conditions:
 * <p/>
 * 1. The application is stopped
 * <p/>
 * 2. The application is deleted
 * <p/>
 * 3. The application is restarted
 * <p/>
 * 4. A streaming content deactivates itself, most likely because it received
 * too many errors on a certain number of retries (this is controlled by the
 * content abstraction)
 * <p/>
 * In all these cases, the output stream to the console is closed, and further
 * polling for new content is terminated. An explicit starting of the tailing
 * operation will start the job again, and create new output streams to the
 * console.
 * 
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
class CloudFoundryConsole extends JobChangeAdapter {

	static final String ATTRIBUTE_SERVER = "org.cloudfoundry.ide.eclipse.server.Server";

	static final String ATTRIBUTE_APP = "org.cloudfoundry.ide.eclipse.server.CloudApp";

	static final String ATTRIBUTE_INSTANCE = "org.cloudfoundry.ide.eclipse.server.CloudInstance";

	static final String CONSOLE_TYPE = "org.cloudfoundry.ide.eclipse.server.appcloud";

	private final CloudFoundryApplicationModule app;

	private Map<IContentType, List<IConsoleJob>> activeStreams = new HashMap<IContentType, List<IConsoleJob>>();

	/** How frequently to check for log changes; defaults to 3 seconds */
	private long sampleInterval = 3000;

	private final MessageConsole console;

	public CloudFoundryConsole(CloudFoundryApplicationModule app, MessageConsole console) {
		this.app = app;
		this.console = console;
	}

	protected StdConsoleStreamJob getStdStreamJob(StdContentType contentType) {

		StdConsoleStreamJob job = null;
		List<IConsoleJob> errorJobs = activeStreams.get(contentType);
		if (errorJobs != null && errorJobs.size() > 0 && errorJobs.get(0) instanceof StdConsoleStreamJob) {
			job = (StdConsoleStreamJob) errorJobs.get(0);
		}

		// Lazily create the std streaming jobs, only when needed, as to not
		// have running jobs that are not used. Only one job
		// per std out content should exist per tailing session
		if (job == null && contentType != null) {
			StdConsoleContent stdContent = null;
			switch (contentType) {
			case STD_ERROR:
				stdContent = new AppStdErrorConsoleContent();
				break;
			case STD_OUT:
				stdContent = new AppStdOutConsoleContent();
				break;
			}
			if (stdContent != null) {
				job = new StdConsoleStreamJob(stdContent);
				startTailing(job);
			}
		}
		return job;

	}

	/**
	 * Starts stream jobs and creates new output streams to the console for each
	 * file listed in the console contents.
	 * @param contents to stream to the console
	 */
	public synchronized void startTailing(List<IConsoleContent> consoleContents) {

		// Add any new streams to the list of active streams
		if (consoleContents != null) {

			for (IConsoleContent content : consoleContents) {
				if (content != null) {
					startTailing(new ConsoleStreamJob(app.getDeployedApplicationName() + " - "
							+ content.getConsoleType().getId(), content));
				}
			}
		}
	}

	protected synchronized void startTailing(IConsoleJob job) {
		if (job == null || job.getConsoleContent() == null) {
			return;
		}

		IContentType contentType = job.getConsoleContent().getConsoleType();

		IOConsoleOutputStream stream = console.newOutputStream();
		if (stream != null) {

			List<IConsoleJob> streamJobs = activeStreams.get(contentType);
			if (streamJobs == null) {
				streamJobs = new ArrayList<CloudFoundryConsole.IConsoleJob>();
				activeStreams.put(contentType, streamJobs);
			}

			IConsoleContent content = job.getConsoleContent();
			content.initialiseStream(stream);

			streamJobs.add(job);
			((Job) job).schedule();
		}

	}

	protected synchronized void remove(IContentType type, IConsoleJob toRemove) {
		List<IConsoleJob> jobsPerType = activeStreams.get(type);
		if (jobsPerType != null) {
			jobsPerType.remove(toRemove);
		}

		if (toRemove != null) {
			toRemove.close();
		}
	}

	/**
	 * Stops any further streaming of file content. Stream jobs are terminated.
	 */
	public synchronized void stop() {

		for (Entry<IContentType, List<IConsoleJob>> entry : activeStreams.entrySet()) {
			List<IConsoleJob> jobs = entry.getValue();
			if (jobs != null) {
				for (IConsoleJob job : jobs) {
					job.close();
				}
			}
		}

		activeStreams.clear();
	}

	static String getConsoleName(CloudFoundryApplicationModule app) {
		CloudApplication cloudApp = app.getApplication();
		String name = (cloudApp != null && cloudApp.getUris() != null && cloudApp.getUris().size() > 0) ? cloudApp
				.getUris().get(0) : app.getDeployedApplicationName();
		return name;
	}

	public MessageConsole getConsole() {
		return console;
	}

	public void writeToStdError(String message) {
		StdConsoleStreamJob stdJob = getStdStreamJob(StdContentType.STD_ERROR);
		if (stdJob != null) {
			stdJob.write(message);
		}
	}

	public void writeToStdOut(String message) {
		StdConsoleStreamJob stdJob = getStdStreamJob(StdContentType.STD_OUT);
		if (stdJob != null) {
			stdJob.write(message);
		}
	}

	class ConsoleStreamJob extends Job implements IConsoleJob {

		protected final IConsoleContent content;

		public ConsoleStreamJob(String name, IConsoleContent content) {
			super(name);
			this.content = content;
			setSystem(true);
		}

		@Override
		protected synchronized IStatus run(IProgressMonitor monitor) {
			if (!content.isActive()) {
				remove(content.getConsoleType(), this);
			}
			else {

				try {
					content.write(monitor);
					schedule(sampleInterval);
				}
				catch (CoreException e) {
					String errorMessage = e.getMessage();
					if (errorMessage != null) {
						writeToStdError(errorMessage);
					}
				}
				// Fetch next ordered content that should follow the current
				// one, even if error occurred, as errors in one content may
				// still schedule additional contents for other files

				if (content instanceof FileConsoleContent) {
					List<IConsoleContent> nextContent = ((FileConsoleContent) content).getNextContent();
					if (nextContent != null) {
						startTailing(nextContent);
					}
				}
			}
			return Status.OK_STATUS;
		}

		protected void streamToConsole(IProgressMonitor monitor) throws CoreException {
			content.write(monitor);
		}

		public synchronized void close() {
			content.close();
		}

		public synchronized IConsoleContent getConsoleContent() {
			return content;
		}

	}

	class StdConsoleStreamJob extends Job implements IConsoleJob {

		private String toStream;

		private StdConsoleContent content;

		public StdConsoleStreamJob(StdConsoleContent content) {
			super(content.getConsoleType().getId());
			this.content = content;
		}

		protected synchronized IStatus run(IProgressMonitor monitor) {

			if (toStream != null) {
				try {
					content.write(toStream, monitor);
					toStream = null;
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(
							"Failed to write message to Cloud Foundry console due to - " + e.getMessage(), e);
				}
			}

			return Status.OK_STATUS;
		}

		public synchronized void write(String message) {
			this.toStream = message;
			if (this.toStream != null) {
				schedule();
			}
		}

		public synchronized void close() {
			content.close();
		}

		public synchronized IConsoleContent getConsoleContent() {
			return content;
		}

	}

	interface IConsoleJob {
		public void close();

		public IConsoleContent getConsoleContent();

	}

}
