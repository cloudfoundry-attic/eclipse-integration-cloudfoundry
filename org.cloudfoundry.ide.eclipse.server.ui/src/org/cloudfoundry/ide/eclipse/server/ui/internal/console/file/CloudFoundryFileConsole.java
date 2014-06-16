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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console.file;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
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
class CloudFoundryFileConsole extends JobChangeAdapter {

	static final String ATTRIBUTE_SERVER = "org.cloudfoundry.ide.eclipse.server.Server";

	static final String ATTRIBUTE_APP = "org.cloudfoundry.ide.eclipse.server.CloudApp";

	static final String ATTRIBUTE_INSTANCE = "org.cloudfoundry.ide.eclipse.server.CloudInstance";

	static final String CONSOLE_TYPE = "org.cloudfoundry.ide.eclipse.server.appcloud";

	private final CloudFoundryApplicationModule app;

	private ActiveStreams activeStreams;

	/** How frequently to check for log changes; defaults to 3 seconds */
	private static long POLL_INTERVAL = 3000;

	private final MessageConsole console;

	public CloudFoundryFileConsole(CloudFoundryApplicationModule app, MessageConsole console) {
		this.app = app;
		this.console = console;
	}

	protected ActiveStreams getActiveStreams() {
		if (activeStreams == null) {
			activeStreams = new ActiveStreams();
		}
		return activeStreams;
	}

	/**
	 * Starts stream jobs and creates new output streams to the console for each
	 * file listed in the console contents.
	 * @param contents to stream to the console
	 */
	public synchronized void startTailing(List<ICloudFoundryConsoleStream> consoleContents) {
		// Add any new streams to the list of active streams
		// External requests to start tailing should always active a new active
		// stream, if not already active
		if (consoleContents == null) {
			return;
		}
		List<IConsoleJob> jobs = getJobs(consoleContents);
		getActiveStreams().startTailing(jobs);
	}

	/**
	 * Stops any further streaming of file content. Stream jobs are terminated.
	 */
	public synchronized void stop() {
		getActiveStreams().close();
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

	/**
	 * Synchronously writes to Std Error. This is run in the same thread where
	 * it is invoked, therefore use with caution as to not send a large volume
	 * of text.
	 * @param message
	 * @param monitor
	 */
	public void synchWriteToStdError(String message) {
		writeToStdStream(message, StdContentType.STD_ERROR);
	}

	/**
	 * Synchronously writes to Std Out. This is run in the same thread where it
	 * is invoked, therefore use with caution as to not send a large volume of
	 * text.
	 * @param message
	 * @param monitor
	 */
	public void synchWriteToStdOut(String message) {
		writeToStdStream(message, StdContentType.STD_OUT);
	}

	protected void writeToStdStream(String message, StdContentType type) {
		IConsoleJob job = getStdStreamJob(type);
		if (job != null) {
			try {
				job.write(message);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
	}

	protected List<IConsoleJob> getJobs(List<ICloudFoundryConsoleStream> consoleContents) {
		if (consoleContents == null) {
			return null;
		}
		List<IConsoleJob> jobs = new ArrayList<IConsoleJob>();
		for (ICloudFoundryConsoleStream stream : consoleContents) {
			IConsoleJob job = createConsoleJob(stream);
			if (job != null) {
				jobs.add(job);
			}
		}
		return jobs;
	}

	protected IConsoleJob createConsoleJob(ICloudFoundryConsoleStream stream) {
		IOConsoleOutputStream outputStream = console.newOutputStream();
		IConsoleJob job = null;
		if (outputStream != null) {
			job = new ConsoleStreamJob(app.getDeployedApplicationName() + " - " + stream.getContentType().getId(),
					stream);
			stream.initialiseStream(outputStream);
		}
		return job;
	}

	protected IConsoleJob getStdStreamJob(StdContentType contentType) {
		if (contentType == null) {
			return null;
		}

		IConsoleJob job = getActiveStreams().getFirst(contentType);

		// Lazily create the std streaming jobs, only when needed, as to not
		// have running jobs that are not used. Only one job
		// per std out content should exist per tailing session
		if (job == null) {

			LocalConsoleStream stdContent = null;
			switch (contentType) {
			case STD_ERROR:
				stdContent = new LocalStdErrorConsoleStream();
				break;
			case STD_OUT:
				stdContent = new LocalStdOutConsoleStream();
				break;
			}
			if (stdContent != null) {
				IOConsoleOutputStream outputStream = console.newOutputStream();
				if (outputStream != null) {
					stdContent.initialiseStream(outputStream);

					job = new StdConsoleStreamJob(stdContent);
					getActiveStreams().addJob(job);
				}
			}
		}
		return job;

	}

	/**
	 * 
	 * NOTE: The job API may be accessed by multiple threads. To avoid
	 * deadlocks, the API of the job should not be synchronized. Instead,
	 * synchronization should occur around the content stream writer itself,
	 * {@link ICloudFoundryConsoleStream}.
	 *
	 */
	class ConsoleStreamJob extends Job implements IConsoleJob {

		protected final ICloudFoundryConsoleStream content;

		public ConsoleStreamJob(String name, ICloudFoundryConsoleStream content) {
			super(name);
			this.content = content;
			setSystem(true);
		}

		protected boolean isActive() {
			return activeStreams.contains(this) && content.isActive();
		}

		@Override
		protected synchronized IStatus run(IProgressMonitor monitor) {

			if (!isActive()) {
				content.close();
			}
			else {

				try {
					content.write(monitor);
				}
				catch (CoreException e) {
					String errorMessage = e.getMessage();

					if (errorMessage != null) {

						// Be sure error message is written in the current job,
						// not the separate
						// error job, as to avoid race condition (i.e.having the
						// error message appear in the console if the current
						// job is stopped.). Also ensure each error message
						// appears in a new line.
						errorMessage = '\n' + errorMessage;

						synchWriteToStdError(errorMessage);
					}
				}

				// re-schedule even if an error is thrown, as the stream may
				// want to attempt again regardless of error.
				if (isActive()) {
					schedule(POLL_INTERVAL);
				}

				// Fetch next ordered content that should follow the current
				// one, even if error occurred, or stream has closed, as errors
				// in one content may
				// still schedule additional contents for other files
				if (content instanceof FileConsoleStream) {
					List<ICloudFoundryConsoleStream> nextContent = ((FileConsoleStream) content).getNextContent();

					if (nextContent != null) {

						List<IConsoleJob> jobs = getJobs(nextContent);
						activeStreams.startTailing(jobs);
					}
				}
			}
			return Status.OK_STATUS;

		}

		public synchronized void write(String message) throws CoreException {
			if (isActive()) {
				content.write(message);
			}
		}

		public IContentType getContentType() {
			return content.getContentType();
		}
	}
}
