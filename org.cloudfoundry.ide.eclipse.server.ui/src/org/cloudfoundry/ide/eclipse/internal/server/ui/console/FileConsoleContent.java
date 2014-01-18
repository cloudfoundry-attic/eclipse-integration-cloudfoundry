/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springframework.http.HttpStatus;

/**
 * Streams file content to the Cloud Foundry console. It continues to check for
 * new content indefinitely, until the Cloud Foundry manager decided to
 * terminate any further streaming (e.g., application is deleted or stopped, or
 * enough errors have been encountered)
 */
public class FileConsoleContent extends AbstractConsoleContent {

	protected int offset = 0;

	private String path;

	private long startingWait = -1;

	private String id;

	private static final int MAX_COUNT = 50;

	private int errorCount = MAX_COUNT;

	/**
	 * 
	 * @param path relative path of content resource, relative to the
	 * application in the remove server
	 * @param swtColour valid constants would be something like SWT.COLOR_RED.
	 * Use -1 to use default console colour.
	 * @param server must not be null. Server where contents should be fetched.
	 * @param appName must not be null
	 * @param instanceIndex must be valid and greater than -1.
	 */
	public FileConsoleContent(String path, int swtColour, CloudFoundryServer server, String appName, int instanceIndex) {
		this(path, swtColour, server, appName, instanceIndex, -1);
	}

	public FileConsoleContent(String path, int swtColour, CloudFoundryServer server, String appName, int instanceIndex,
			long startingWait) {
		super(server, swtColour, appName, instanceIndex);
		this.path = path;

		this.startingWait = startingWait;
	}

	/**
	 * 
	 * @return How long to wait before streaming starts. -1 if no waiting
	 * required.
	 */
	public long startingWait() {
		return startingWait;
	}

	public List<IConsoleContent> getNextContent() {
		return null;
	}

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
			if (!isClosed() && adjustErrorCount()) {
				close();
				// FIXNS: For CF 1.6.0, do not log the error in the console.
				// Leaving it commented in case it needs to be reenabled again
				// in the future.
				// String actualErrorMessage = ce.getMessage() != null ?
				// ce.getMessage()
				// : "Unknown error while obtaining content for " + path;
				// throw new CoreException(CloudFoundryPlugin.getErrorStatus(
				// "Too many failures trying to stream content from " + path +
				// ". Closing stream due to: "
				// + actualErrorMessage, ce));
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

	/*
	 * @Overrride
	 */
	public String toString() {
		return id;
	}
}
