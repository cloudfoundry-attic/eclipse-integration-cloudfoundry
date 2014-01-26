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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;

/**
 * Displays staging log information to the console. Once staging log content is
 * received, it schedules stream content for other log files (std out/error).
 * That way, staging always appears first in the console output
 */
public class StagingFileConsoleStream extends FileConsoleStream {

	public static final String STAGING_LOG = "logs/staging_task.log";

	private boolean scheduleNextContent = false;

	// FIXNS: Hack until get next content API or content manager is improved
	// such that content is only requested once to avoid
	// duplicate requests
	private boolean alreadyRequestedNextContent = false;

	private String initialContent = null;

	public StagingFileConsoleStream(CloudFoundryServer cloudServer, String appName, int instanceIndex) {
		super(STAGING_LOG, SWT.COLOR_DARK_GREEN, cloudServer, appName, instanceIndex);
	}

	@Override
	public List<ICloudFoundryConsoleStream> getNextContent() {
		// Process request for next content ONCE only after having received some
		// staging content.
		// that way staging information will appear first before any other log
		// content
		if (scheduleNextContent && !alreadyRequestedNextContent) {
			alreadyRequestedNextContent = true;

			return new StdConsoleContents().getContents(getServer(), appName, instanceIndex);
		}
		return null;
	}

	protected String getContentFromFile(IProgressMonitor monitor) throws CoreException {
		String content = super.getContentFromFile(monitor);
		if (content != null) {
			if (initialContent == null) {
				// Prepend new line if receiving first content to ensure staging
				// information always appears
				// on a new line
				content = '\n' + content;
			}
			initialContent = content;
			scheduleNextContent = true;
		}
		return content;
	}

	protected String reachedMaximumErrors(CoreException ce) {
		// Schedule next logs after too many failed attempts to fetch staging
		// logs
		scheduleNextContent = true;
		return "Failed to fetch staging contents from: " + getFilePath();
	}

	protected int getMaximumErrorCount() {
		return 60;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.console.FileConsoleContent
	 * #getMessageOnRetry(org.eclipse.core.runtime.CoreException, int)
	 */
	@Override
	protected String getMessageOnRetry(CoreException ce, int currentAttemptsRemaining) {
		// If the next log contents (std out and std error), have not yet been
		// scheduled
		// it means we are still waiting for some staging output to occur, or
		// for max errors in streaming
		// staging to hit. However, once next content is scheduled, do not show
		// any further information regarding
		// staging
		if (!scheduleNextContent) {

			// Note that the first attempt to stream is not a "retry" attempt to
			// stream, therefore
			// the first RETRY attempt will always be maximum error count - 1.
			if (currentAttemptsRemaining == getMaximumErrorCount() - 1) {
				return "Waiting for application to start...";
			}
			else if (currentAttemptsRemaining > 0) {
				// Append progress dot
				if (currentAttemptsRemaining % 20 == 0) {
					return "\n.";
				}
				else {
					return ".";
				}
			}
			else if (currentAttemptsRemaining == 0) {
				return "Still waiting for applicaiton to start...";
			}
		}
		return null;
	}

}
