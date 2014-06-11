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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import java.util.List;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.eclipse.ui.console.MessageConsole;

/**
 * 
 * 
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
class ApplicationLogConsole extends CloudFoundryConsole {

	public ApplicationLogConsole(MessageConsole console) {
		super(console);
	}

	/**
	 * Synchronously writes to Std Error. This is run in the same thread where
	 * it is invoked, therefore use with caution as to not send a large volume
	 * of text.
	 * @param message
	 * @param monitor
	 */
	public void writeToStdError(String message) {
		writeToStream(message, StandardLogContentType.STD_ERROR);
	}

	/**
	 * Synchronously writes to Std Out. This is run in the same thread where it
	 * is invoked, therefore use with caution as to not send a large volume of
	 * text.
	 * @param message
	 * @param monitor
	 */
	public void writeToStdOut(String message) {
		writeToStream(message, StandardLogContentType.STD_OUT);
	}

	protected synchronized void writeToStream(String message, LogContentType type) {
		if (message != null) {
			writeToStream(new CloudLog(message, type));
		}
	}

	public synchronized void writeApplicationLogs(List<ApplicationLog> logs, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		if (logs != null) {
			for (ApplicationLog log : logs) {
				CloudLog cloudLog = ApplicationLogConsoleStream.getCloudlog(log, appModule, cloudServer);
				if (cloudLog != null) {
					writeToStream(cloudLog);
				}
			}
		}
	}

}
