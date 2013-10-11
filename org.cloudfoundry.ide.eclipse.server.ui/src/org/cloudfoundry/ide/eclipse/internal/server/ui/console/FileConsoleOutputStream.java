/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.springframework.http.HttpStatus;

/**
 * Streams content from a remote file to the CF console. It self-terminates if
 * too many errors are encountered in a row while attempting to obtain content
 * from the file.
 * 
 */
public class FileConsoleOutputStream extends ConsoleOutputStream {

	private final String path;

	protected int offset = 0;

	private final int swtColour;

	private final String appName;

	private final int instanceIndex;

	private String id;

	private static final int MAX_COUNT = 50;

	private int errorCount = MAX_COUNT;

	/**
	 * 
	 * @param path file path to be fetched from the server.
	 * @param isError true if the output should be sent to the console error
	 * output stream.
	 * @param server
	 * @param swtColour use -1 to use default console colour
	 */
	public FileConsoleOutputStream(IOConsoleOutputStream outStream, String path, CloudFoundryServer server,
			int swtColour, String appName, int instanceIndex) {
		super(outStream, server);
		this.path = path;
		this.swtColour = swtColour;
		this.appName = appName;
		this.instanceIndex = instanceIndex;

		this.id = appName + " - " + instanceIndex + " - " + path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleOutputStream#getStreamColour()
	 */
	protected int getStreamColour() {
		return swtColour;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleOutputStream
	 * #getContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected String getContent(IProgressMonitor monitor) throws CoreException {

		try {
			String content = getContentFromFile(monitor);
			// Note that if no error was thrown, reset the error count. The
			// stream should only terminate if N number of errors are met in
			// a row.
			errorCount = MAX_COUNT;
			return content;
		}
		catch (CoreException ce) {
			CloudFoundryException cfe = ce.getCause() instanceof CloudFoundryException ? (CloudFoundryException) ce
					.getCause() : null;

			// Do not log error if is is due to range not satisfied, or file is
			// not
			// found for instance
			if (cfe != null
					&& (HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(cfe.getStatusCode()) || CloudErrorUtil
							.isFileNotFoundForInstance(cfe))) {
				// These types of errors are "valid" meaning they don't indicate
				// a problem. Return null to let the caller know that there is
				// no further content at the moment, but to keep the stream
				// alive.
				return null;
			}

			// If stream is still active, handle the error.
			if (!shouldCloseStream() && adjustErrorCount()) {
				requestStreamClose(true);
				String actualErrorMessage = ce.getMessage() != null ? ce.getMessage()
						: "Unknown error while obtaining content for " + path;
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(
						"Too many failures trying to stream content from " + path + ". Closing stream due to: "
								+ actualErrorMessage, ce));
			}
		}

		return null;
	}

	/**
	 * Get content from the file from the client starting from the current
	 * offset.
	 * @param monitor
	 * @return content from the file starting from the current offset. It may be
	 * null if there is no more content available.
	 * @throws CoreException if client or server error occurred while fetching
	 * content for the file.
	 */
	protected String getContentFromFile(IProgressMonitor monitor) throws CoreException {
		try {
			String content = server.getBehaviour().getFile(appName, instanceIndex, path, offset, monitor);
			if (content != null) {
				offset += content.length();
			}
			return content;
		}
		catch (CloudFoundryException cfex) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(cfex));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleOutputStream#getID()
	 */
	public String getID() {
		return id;
	}

	/**
	 * 
	 * @return true if errors have reached a limit after adjusting the current
	 * count. False otherwise
	 */
	protected boolean adjustErrorCount() {
		// Otherwise an error occurred
		if (errorCount > 0) {
			errorCount--;
		}
		return errorCount == 0;
	}
}