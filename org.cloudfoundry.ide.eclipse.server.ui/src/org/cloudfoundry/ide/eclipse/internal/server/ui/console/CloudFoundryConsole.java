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
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * 
 * The Console job for applications running on Cloud Foundry works on the
 * principle of a self-scheduling job,which continues to schedule itself to
 * check for new content in remote files whose content are streamed to the
 * console.
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
 * 4. The particular file that is streamed to the console generates repeated
 * errors above a threshold value of admisable number of errors. If a particular
 * file generates too many errors, the stream to that file is closed, and the
 * file is removed from the list of files that needs to be polled. That way
 * polling of other files are unaffected if streaming content of a particular
 * file repeatedly fails.
 * <p/>
 * In all these cases, the output stream to the console is closed, and further
 * polling for new content is terminated. An explicit starting of the tailing
 * operation will start the job again, and create new output streams to the
 * console. Closing output streams does NOT cancel or terminate the job itself.
 * The job keeps running in all cases, EXCEPT when an application is deleted.
 * See the Console Manager for further details.
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

	private final CloudApplication app;

	private List<ConsoleStream> activeStreams = new ArrayList<CloudFoundryConsole.ConsoleStream>();

	/** How frequently to check for log changes; defaults to 5 seconds */
	private long sampleInterval = 5000;

	private final MessageConsole console;

	public CloudFoundryConsole(CloudApplication app, MessageConsole console) {
		this.app = app;
		this.console = console;
	}

	/**
	 * Starts stream jobs and creates new output streams to the console for each
	 * file listed in the console contents.
	 * @param contents to stream to the console
	 */
	public synchronized void startTailing(ConsoleContents contents) {

		// Add any new streams to the list of active streams
		if (contents != null) {
			List<IConsoleContent> consoleContents = contents.getContents();
			if (consoleContents != null) {
				List<ConsoleStream> addedStreams = new ArrayList<CloudFoundryConsole.ConsoleStream>();
				for (IConsoleContent content : consoleContents) {
					IOConsoleOutputStream stream = console.newOutputStream();
					if (stream != null) {

						ICloudFoundryConsoleOutputStream outStream = content.getOutputStream(stream);
						if (outStream != null) {
							ConsoleStream consoleStream = new ConsoleStream(getConsoleName(app), outStream);
							consoleStream.addJobChangeListener(this);
							consoleStream.tailing(true);

							addedStreams.add(new ConsoleStream(getConsoleName(app), outStream));
						}
						else {
							try {
								stream.close();
							}
							catch (IOException ioe) {
								// Ignore;
							}
						}
					}
				}

				activeStreams.addAll(addedStreams);

				// Reschedule all jobs
				for (ConsoleStream stream : addedStreams) {
					stream.schedule();
				}
			}
		}

	}

	protected synchronized ConsoleStream remove(ConsoleStream toRemove) {
		List<ConsoleStream> toKeep = new ArrayList<CloudFoundryConsole.ConsoleStream>();
		ConsoleStream removed = null;
		for (ConsoleStream stream : activeStreams) {
			if (stream != toRemove) {
				toKeep.add(stream);
			}
			else {
				removed = stream;
			}
		}
		activeStreams = toKeep;
		return removed;
	}

	/**
	 * Stops any further streaming of file content. Stream jobs are terminated.
	 */
	public synchronized void stop() {

		for (ConsoleStream outStream : activeStreams) {
			outStream.close();
		}

		activeStreams.clear();
	}

	static String getConsoleName(CloudApplication app) {
		String name = (app.getUris() != null && app.getUris().size() > 0) ? app.getUris().get(0) : app.getName();
		return name;
	}

	public MessageConsole getConsole() {
		return console;
	}

	class ConsoleStream extends Job {

		private final ICloudFoundryConsoleOutputStream stream;

		/** Is the tailer currently tailing? */

		private boolean tailing = true;

		private boolean close = false;

		private final String name;

		public ConsoleStream(String name, ICloudFoundryConsoleOutputStream stream) {
			super(name);
			this.stream = stream;
			this.name = name;
			setSystem(true);

		}

		@Override
		protected synchronized IStatus run(IProgressMonitor monitor) {
			if (tailing && !close) {

				boolean remove = stream.shouldCloseStream();
				if (!remove) {
					try {
						stream.write(monitor);
					}
					catch (CoreException e) {
						remove = true;
						CloudFoundryPlugin.log(e);
					}
				}

				if (remove) {
					close();
				}
				else {
					schedule(sampleInterval);
				}

			}
			return Status.OK_STATUS;

		}

		public synchronized boolean close() {
			if (!close) {
				close = true;
				tailing(false);

				try {
					stream.close();
				}
				catch (IOException io) {
					CloudFoundryPlugin.logWarning("I/O Exception attempting to close console stream: " + name);
				}

				super.done(Status.OK_STATUS);
			}

			return close;
		}

		public synchronized void tailing(boolean tailing) {
			this.tailing = tailing;
		}
	}

	@Override
	public synchronized void done(IJobChangeEvent event) {
		Job job = event.getJob();

		if (job instanceof ConsoleStream) {
			ConsoleStream stream = (ConsoleStream) job;
			remove(stream);
		}
	}
	
	

}
