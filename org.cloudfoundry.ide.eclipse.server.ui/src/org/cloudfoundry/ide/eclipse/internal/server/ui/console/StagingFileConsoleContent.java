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

import java.util.ArrayList;
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
public class StagingFileConsoleContent extends FileConsoleContent {

	public static final String STAGING_LOG = "logs/staging_task.log";

	public static final String STD_OUT_LOG = "logs/stdout.log";

	public static final String STD_ERROR_LOG = "logs/stderr.log";

	private boolean receivedStagingContent = false;

	private boolean requestNextContent = false;

	public StagingFileConsoleContent(CloudFoundryServer cloudServer, String appName, int instanceIndex) {
		super(STAGING_LOG, SWT.COLOR_DARK_GREEN, cloudServer, appName, instanceIndex);
	}

	@Override
	public List<IConsoleContent> getNextContent() {
		// Process request for next content ONCE only after having received some
		// staging content.
		// that way staging information will appear first before any other log
		// content
		if (receivedStagingContent && !requestNextContent) {
			requestNextContent = true;
			List<IConsoleContent> content = new ArrayList<IConsoleContent>();
			content.add(new StdLogFileConsoleContent(STD_ERROR_LOG, SWT.COLOR_RED, server, appName, instanceIndex));
			content.add(new StdLogFileConsoleContent(STD_OUT_LOG, -1, server, appName, instanceIndex));
			return content;
		}
		return null;
	}

	protected String getContentFromFile(IProgressMonitor monitor) throws CoreException {
		String content = super.getContentFromFile(monitor);
		if (content != null) {
			receivedStagingContent = true;
		}
		return content;
	}

	protected String reachedMaximumErrors(CoreException ce) {
		// Schedule next logs after too many failed attempts to fetch staging
		// logs
		receivedStagingContent = true;
		return super.reachedMaximumErrors(ce);
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudfoundry.ide.eclipse.internal.server.ui.console.FileConsoleContent#getMessageOnRetry(org.eclipse.core.runtime.CoreException, int)
	 */
	@Override
	protected String getMessageOnRetry(CoreException ce, int currentErrorCount) {
		// If no staging content has been received so far, application is still starting
		if (!receivedStagingContent && currentErrorCount > 0 && currentErrorCount % 3 == 0) {
			return "Waiting for application to start...";
		}
		return null;
	}

	@Override
	protected String getMaximumErrorMessage() {
		return "Taking too long to fetch staging log content from : " + STAGING_LOG;
	}

	static class StdLogFileConsoleContent extends FileConsoleContent {

		public StdLogFileConsoleContent(String path, int swtColour, CloudFoundryServer server, String appName,
				int instanceIndex) {
			super(path, swtColour, server, appName, instanceIndex);
		}

		protected int getMaximumErrorCount() {
			return 5;
		}

		protected String getMaximumErrorMessage() {
			return "Taking too long to fetch log content from : " + getFilePath()
					+ ". The application may not be running correctly.";
		}
	}

}
