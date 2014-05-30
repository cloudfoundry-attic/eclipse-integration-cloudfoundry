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

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;

class StdLogFileConsoleStream extends FileConsoleStream {

	// public static final String MAXIMUM_ERROR =
	// "Unable to fetch contents for: {0}. The application may no longer be responsive.";

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
		return null;

//		return NLS.bind(MAXIMUM_ERROR, getFilePath());
	}
}