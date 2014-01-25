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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;

class StdLogFileConsoleStream extends FileConsoleStream {

	public StdLogFileConsoleStream(String path, int swtColour, CloudFoundryServer server, String appName,
			int instanceIndex) {
		super(path, swtColour, server, appName, instanceIndex);
	}

	@Override
	protected int getMaximumErrorCount() {
		return 10;
	}

	protected String getMessageOnRetry(CoreException ce, int currentAttemptsRemaining) {
		return null;
	}

	@Override
	protected String reachedMaximumErrors(CoreException ce) {
		return "Unable to fetch contents for: " + getFilePath()
				+ ". The application may not be running or taking too long to start. Please refresh console manually.";
	}
}