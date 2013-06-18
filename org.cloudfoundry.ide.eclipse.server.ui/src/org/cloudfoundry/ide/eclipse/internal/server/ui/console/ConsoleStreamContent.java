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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.FileContent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * Gets console content from the stderror and stdout log files for a given
 * application. The content of both logs is obtained by getting a range of bytes
 * from the remote log files, as opposed to the full log file. Consequently,
 * offsets for each log file are kept locally to keep track of what has already
 * be printed to the console, and only new content is fetched based on the
 * offsets.
 * 
 */
public class ConsoleStreamContent {

	protected final IOConsoleOutputStream stdError;

	protected final IOConsoleOutputStream stdOut;

	protected final List<FileContentHandler> standardOutputContent = new ArrayList<FileContentHandler>();

	protected final List<FileContentHandler> errorOutputContent = new ArrayList<FileContentHandler>();

	protected final MessageConsole console;

	protected final ConsoleContent content;

	public ConsoleStreamContent(ConsoleContent content, MessageConsole console, CloudApplication app, int instanceIndex) {
		this.stdOut = console.newOutputStream();
		this.stdError = console.newOutputStream();
		this.content = content;
		this.console = console;

		if (stdError != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					stdError.setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				}
			});
		}

		List<FileContent> fileContents = content.getFileContents();

		// Regardless of how many contents there are, there are only two streams
		// available
		// to send the content to the console: either use the error stream, or
		// the standard out stream.
		for (FileContent fileContent : fileContents) {
			if (fileContent.isError()) {
				errorOutputContent.add(getFileContentHandler(fileContent, getStdErrorStream(), app.getName(),
						instanceIndex));
			}
			else {
				standardOutputContent.add(getFileContentHandler(fileContent, getStdOutStream(), app.getName(),
						instanceIndex));
			}
		}
	}

	public void displayInitialContent() {
		// Show initial content
		String initialContent = content.getInitialContent();
		if (initialContent != null && stdOut != null) {

			try {
				stdOut.write(initialContent);
			}
			catch (IOException ioe) {
				CloudFoundryPlugin.logError(ioe);
			}
			// No need to close it, as the output stream is managed by the
			// console itself.
		}
	}

	protected FileContentHandler getFileContentHandler(FileContent content, OutputStream outStream, String appName,
			int instanceIndex) {
		return new FileContentHandler(content, outStream, appName, instanceIndex);
	}

	protected IOConsoleOutputStream getStdErrorStream() {
		return stdError;
	}

	protected IOConsoleOutputStream getStdOutStream() {
		return stdOut;
	}

	public void reset() {
		resetFileContent(standardOutputContent);
		resetFileContent(errorOutputContent);
		console.clearConsole();
	}

	protected void resetFileContent(List<FileContentHandler> contents) {
		for (FileContentHandler contentUI : contents) {
			contentUI.reset();
		}
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

		String errorContent = getContent(errorOutputContent, monitor);
		int totalErrorCount = getTotalOffset(errorOutputContent);
		int totalStdCount = getTotalOffset(standardOutputContent);
		String outContent = getContent(standardOutputContent, monitor);
		return new Result(errorContent, outContent, totalErrorCount, totalStdCount);
	}

	protected String getContent(List<FileContentHandler> content, IProgressMonitor monitor) throws CoreException {
		StringBuffer results = new StringBuffer();
		for (FileContentHandler ct : content) {
			String result = ct.getContent(monitor);
			if (result != null) {
				// If there already is previous content, seperate the new
				// content with a newline
				if (results.length() > 0) {
					results.append("\n");
				}
				results.append(result);
			}
		}

		return results.length() > 0 ? results.toString() : null;
	}

	protected int getTotalOffset(List<FileContentHandler> content) {
		int count = 0;
		for (FileContentHandler ct : content) {
			count += ct.getOffset();
		}
		return count;
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
