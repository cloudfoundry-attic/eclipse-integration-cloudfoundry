/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Streams file content to the Cloud Foundry console. It continues to check for
 * new content indefinitely, until the Cloud Foundry manager decided to
 * terminate any further streaming (e.g., application is deleted or stopped, or
 * enough errors have been encountered)
 */
public class FileConsoleStream extends CloudFoundryConsoleStream {

	protected int tailingOffset = 0;

	private final String path;

	private static final int MAX_COUNT = 40;

	private int attemptsRemaining;

	private static final IContentType FILE_CONTENT_TYPE = new FileStreamContentType();

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
	public FileConsoleStream(String path, int swtColour, CloudFoundryServer server, String appName, int instanceIndex) {
		super(server, swtColour, appName, instanceIndex);
		this.path = path;
		attemptsRemaining = getMaximumErrorCount();
	}

	/**
	 * 
	 * @return true if still active, stream open and is able to stream content.
	 * False otherwise
	 */
	public synchronized boolean isActive() {
		return attemptsRemaining > 0 && super.isActive();
	}

	/**
	 * 
	 * @return basic chaining API.
	 */
	public synchronized List<ICloudFoundryConsoleStream> getNextContent() {
		return null;
	}

	protected String getFilePath() {
		return path;
	}

	protected int getMaximumErrorCount() {
		return MAX_COUNT;
	}

	protected String getContent(IProgressMonitor monitor) throws CoreException {

		if (!isActive()) {
			return null;
		}

		try {
			String content = getContentFromFile(monitor);

			// Note that if no error was thrown, reset the error count. The
			// stream should only terminate if N number of errors are met in
			// a row.
			attemptsRemaining = getMaximumErrorCount();
			return content;
		}
		catch (CoreException ce) {
			CloudFoundryException cfe = ce.getCause() instanceof CloudFoundryException ? (CloudFoundryException) ce
					.getCause() : null;

			// Do not log error if is is due to range not satisfied, or file is
			// not
			// found for instance
			if (cfe != null
					&& (CloudErrorUtil.isRequestedFileRangeNotSatisfiable(cfe) || CloudErrorUtil
							.isFileNotFoundForInstance(cfe))) {
				// These types of errors are "valid" meaning they don't indicate
				// a problem. Return null to let the caller know that there is
				// no further content at the moment, but to keep the stream
				// alive.
				return null;
			}

			return handleErrorCount(ce);
		}
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
			String content = server.getBehaviour().getFile(appName, instanceIndex, path, tailingOffset, monitor);
			if (content != null) {
				tailingOffset += content.length();
			}
			return content;
		}
		catch (CloudFoundryException cfex) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(cfex));
		}
	}

	/**
	 * Handling error has two options:
	 * 
	 * <p/>
	 * 
	 * 1. Retry on error. If a message should be displayed due to the error,
	 * that message is returned to be streamed to the console.
	 * 
	 * <p/>
	 * 
	 * 2. Maximum errors reached. Exception is thrown if there is a message
	 * associated with the last error that needs to be displayed to the user.
	 * Otherwise, the stream deactives itself after maximum errors are reached.
	 * <p/>
	 * 
	 * If the error is encountered when the stream is no longer active, nothing
	 * happens and null is returned.
	 * 
	 * @return a message to be streamed to the console based on the current.
	 * error. Or null if no message should be streamed.
	 */
	protected String handleErrorCount(CoreException ce) throws CoreException {

		if (!isActive()) {
			return null;
		}

		if (isFatalError(ce)) {
			// NOTE: closing within the stream appears to throw exceptions. To
			// "deactivate" the stream, set
			// attempts to zero instead
			// close();
			attemptsRemaining = 0;
			throw ce;
		}

		// If error count maximum has been reached, display the error and close
		// stream
		String message = null;
		if (adjustErrorCount()) {
			message = reachedMaximumErrors(ce);
			if (message != null) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(message, ce));
			}
		}

		// Otherwise see if there is a message on retry that should be sent to
		// the console
		if (message == null) {
			message = getMessageOnRetry(ce, attemptsRemaining);
		}

		return message;
	}

	/**
	 * Return a message based on the current number of consecutive attempts made
	 * to fetch streaming content from the file due to errors. If the message is
	 * not meant to be an error, return it, and it will be displayed in the same
	 * format as the file content. </p> If the message should be displayed as an
	 * error to the user, throw a {@link CoreException}. This will not stop the
	 * streaming process, but simply allow the content manager to handle the
	 * error (e.g. display in std error console output).
	 * @param ce error that will result in a further attempt to stream content
	 * from the file.
	 * @param attemptsRemaining how many consecutive attempts still remain
	 * before any further streaming attempts are stopped.
	 * @return optional message related to the attempt and associated error, or
	 * null.
	 * @throws CoreException if the error requires the content manager to handle
	 * the error. Otherwise return a message or null.
	 */
	protected String getMessageOnRetry(CoreException ce, int attemptsRemaining) throws CoreException {
		return null;
	}

	/**
	 * Gets invoked when maximum errors have been reached. Return an error
	 * message that gets displayed in the console.
	 * @param ce
	 * @return error message that gets displayed in the console when maximum
	 * errors are reached. return Null if nothing should be displayed if maximum
	 * errors are reached @
	 */
	protected String reachedMaximumErrors(CoreException ce) {
		return "Taking too long to fetch file contents";
	}

	/**
	 * 
	 * @param ce
	 * @return true if error is fatal and streaming should stop. False otherwise
	 */
	protected boolean isFatalError(CoreException ce) {
		return false;
	}

	/**
	 * 
	 * @return true if maximum number of errors reached. False otherwise
	 */
	protected boolean adjustErrorCount() {
		if (attemptsRemaining > 0) {
			attemptsRemaining--;
		}
		return attemptsRemaining == 0;
	}

	public IContentType getContentType() {
		return FILE_CONTENT_TYPE;
	}
}
