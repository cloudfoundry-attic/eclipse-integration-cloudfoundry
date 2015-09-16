/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal.console;

import java.util.List;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.core.runtime.CoreException;

/**
 * Application Log console that manages loggregator streams for a deployed
 * application. This console should only be created and used for applications
 * that are already published, as it initialises loggregator support which
 * requires the application to exist in the Cloud server.
 * 
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
class ApplicationLogConsole extends CloudFoundryConsole {

	public ApplicationLogConsole(ConsoleConfig config) {
		super(config);
	}

	public synchronized void writeApplicationLogs(List<ApplicationLog> logs) {
		if (logs != null) {
			for (ApplicationLog log : logs) {
				writeApplicationLog(log);
			}
		}
	}

	/**
	 * Writes a loggregator application log to a corresponding console stream.
	 * This is different from {@link #writeToStream(CloudLog)} in the sense that
	 * the latter writes a local log and does not handle special cases for
	 * loggregator.
	 * @param log
	 */
	protected synchronized void writeApplicationLog(ApplicationLog log) {
		if (log == null) {
			return;
		}
		try {
			// Write to the application console stream directly, as the
			// Application log stream does
			// additional processing on the raw application log that may not be performed
			// by the base CloudFoundryConsole
			ConsoleStream stream = getStream(StandardLogContentType.APPLICATION_LOG);
			if (stream instanceof ApplicationLogConsoleStream) {
				((ApplicationLogConsoleStream) stream).write(log);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}

	}
}
