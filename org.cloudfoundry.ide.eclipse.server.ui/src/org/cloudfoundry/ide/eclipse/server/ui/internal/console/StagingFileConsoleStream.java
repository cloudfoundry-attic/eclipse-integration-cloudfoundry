/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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

package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
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
		return null;
	}

	protected int getMaximumErrorCount() {
		return 60;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.server.ui.internal.console.FileConsoleContent
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
				return Messages.CONSOLE_WAITING_FOR_APPLICATION_TO_START + '\n';
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
				return '\n' + Messages.CONSOLE_STILL_WAITING_FOR_APPLICAITON_TO_START;
			}
		}
		return null;
	}

}
