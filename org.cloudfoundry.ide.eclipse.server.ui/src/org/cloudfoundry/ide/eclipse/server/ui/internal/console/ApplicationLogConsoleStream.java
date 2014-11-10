/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * Unlike {@link SingleConsoleStream} the application log console stream manages
 * various log streams, one for each type of log content (e.g. STDOUT,
 * STDERROR,..) received by a loggregator listener callback that is registered
 * for the given application. Since the loggregator listener is an asynchronous
 * callback, the manager has to keep track on whether each stream is still
 * available before attempting to send content to that stream whenever the
 * callback is performed.
 * 
 * <p/>
 * Closing the manager closes all active streams, as well as cancels any further
 * loggregator callbacks.
 * 
 *
 */
public class ApplicationLogConsoleStream extends ConsoleStream {

	private Map<LogContentType, ConsoleStream> logStreams = new HashMap<LogContentType, ConsoleStream>();

	private CloudFoundryApplicationModule appModule;

	public ApplicationLogConsoleStream() {

	}

	public synchronized void close() {
		if (logStreams != null) {
			for (Entry<LogContentType, ConsoleStream> entry : logStreams.entrySet()) {
				entry.getValue().close();
			}
			logStreams.clear();
		}

	}

	public synchronized void initialiseStream(MessageConsole console, CloudFoundryApplicationModule appModule)
			throws CoreException {

		if (appModule == null) {
			throw CloudErrorUtil.toCoreException(Messages.ERROR_FAILED_INITIALISE_APPLICATION_LOG_STREAM);
		}
		this.console = console;
		this.appModule = appModule;

	}

	@Override
	public synchronized boolean isActive() {
		return true;
	}

	@Override
	protected IOConsoleOutputStream getActiveOutputStream(CloudLog log) {

		ConsoleStream consoleStream = getStream(log);
		if (consoleStream != null) {
			return consoleStream.getActiveOutputStream(log);
		}
		return null;
	}

	public static CloudLog getCloudlog(ApplicationLog appLog, CloudFoundryApplicationModule appModule,
			CloudFoundryServer server) {
		if (appLog == null) {
			return null;
		}
		org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType type = appLog.getMessageType();
		LogContentType contentType = StandardLogContentType.APPLICATION_LOG_UNKNOWN;
		if (type != null) {
			switch (type) {
			case STDERR:
				contentType = StandardLogContentType.APPLICATION_LOG_STS_ERROR;
				break;
			case STDOUT:
				contentType = StandardLogContentType.APPLICATION_LOG_STD_OUT;
				break;
			}
		}

		return new CloudLog(format(appLog.getMessage()), contentType, server, appModule);

	}

	protected static String format(String message) {
		if (message.contains("\n") || message.contains("\r")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return message;
		}
		return message + '\n';
	}

	protected synchronized ConsoleStream getStream(CloudLog log) {
		LogContentType type = log.getLogType();
		ConsoleStream stream = logStreams.get(type);
		if (stream == null) {
			int swtColour = -1;
			if (StandardLogContentType.APPLICATION_LOG_STS_ERROR.equals(type)) {
				swtColour = SWT.COLOR_RED;
			}
			else if (StandardLogContentType.APPLICATION_LOG_STD_OUT.equals(type)) {
				swtColour = SWT.COLOR_DARK_GREEN;
			}
			else if (StandardLogContentType.APPLICATION_LOG_UNKNOWN.equals(type)) {
				swtColour = SWT.COLOR_BLACK;
			}

			if (swtColour > -1) {
				try {
					stream = new SingleConsoleStream(new UILogConfig(swtColour));
					stream.initialiseStream(console, appModule);
					logStreams.put(type, stream);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
			}
		}
		return stream;
	}

}
