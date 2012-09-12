/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.io.IOException;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.springframework.http.HttpStatus;

/**
 * Gets console content from the stderror and stdout log files for a given
 * application. The content of both logs is obtained by getting a range of bytes
 * from the remote log files, as opposed to the full log file. Consequently,
 * offsets for each log file are kept locally to keep track of what has already
 * be printed to the console, and only new content is fetched based on the
 * offsets.
 * 
 */
public class ConsoleContent {

	protected final IOConsoleOutputStream stdError;

	protected final IOConsoleOutputStream stdOut;

	protected final CloudFoundryServer cloudServer;

	protected final CloudApplication app;

	protected final int instanceIndex;

	protected int stderrOffset = 0;

	protected String stderrPath = "logs/stderr.log";

	protected int stdoutOffset = 0;

	protected String stdoutPath = "logs/stdout.log";

	protected final MessageConsole console;

	public ConsoleContent(CloudFoundryServer cloudServer, MessageConsole console, CloudApplication app,
			int instanceIndex) {
		this.stdOut = console.newOutputStream();
		this.stdError = console.newOutputStream();
		this.cloudServer = cloudServer;
		this.app = app;
		this.instanceIndex = instanceIndex;
		this.console = console;

		if (stdError != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					stdError.setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				}
			});
		}
	}

	protected IOConsoleOutputStream getStdErrorStream() {
		return stdError;
	}

	protected IOConsoleOutputStream getStdOutStream() {
		return stdOut;
	}

	public void reset() {
		stderrOffset = 0;
		stdoutOffset = 0;
		console.clearConsole();
	}

	/**
	 * Gets the latest changes to the app's log files and sends those to the
	 * console output. This is more efficient as only changes are fetched from
	 * the server, as opposed to the full log file
	 * 
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	public Result getFileContent(IProgressMonitor monitor) throws CoreException {
		String errorContent = getStdErrorContent(monitor);
		String outContent = getStdOurContent(monitor);
		return new Result(errorContent, outContent, stderrOffset, stdoutOffset);
	}

	protected String getStdErrorContent(IProgressMonitor monitor) throws CoreException {
		String content = getContent(stdError, stderrPath, stderrOffset, monitor);
		if (content != null) {
			stderrOffset += content.length();
		}
		return content;
	}

	protected String getStdOurContent(IProgressMonitor monitor) throws CoreException {
		String content = getContent(stdOut, stdoutPath, stdoutOffset, monitor);
		if (content != null) {
			stdoutOffset += content.length();
		}
		return content;
	}

	protected String getAndWriteContentFromServer(IOConsoleOutputStream stream, String path, int offset,
			IProgressMonitor monitor) throws CoreException, IOException {
		String content = cloudServer.getBehaviour().getFile(app.getName(), instanceIndex, path, offset, monitor);
		if (stream != null && content != null && content.length() > 0) {
			stream.write(content);
		}
		return content;
	}

	protected String getContent(IOConsoleOutputStream stream, String path, int offset, IProgressMonitor monitor)
			throws CoreException {
		String content = null;
		try {
			content = getAndWriteContentFromServer(stream, path, offset, monitor);
		}
		catch (CoreException e) {
			Throwable t = e.getCause();
			// Ignore errors due to specified start position being past the
			// content length (i.e there is no new content). Otherwise rethrow
			// error
			if (t == null || !(t instanceof CloudFoundryException)
					|| !HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(((CloudFoundryException) t).getStatusCode())) {
				throw e;
			}
		}
		catch (IOException ioe) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(ioe));
		}
		return content;
	}

	public static class Result {

		private final String errorContent;

		private final String outContent;

		private final int errorEndingPosition;

		private final int outEndingPosition;

		public Result(String errorContent, String outContent, int errorEndingPosition, int outEndingPosition) {
			this.errorContent = errorContent;
			this.outContent = outContent;
			this.errorEndingPosition = errorEndingPosition;
			this.outEndingPosition = outEndingPosition;
		}

		public String getErrorContent() {
			return errorContent;
		}

		public String getOutContent() {
			return outContent;
		}

		public int getErrorEndingPosition() {
			return errorEndingPosition;
		}

		public int getOutEndingPosition() {
			return outEndingPosition;
		}

	}

}
