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

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * Unlike {@link SingleConsoleStream} the application log console stream creates
 * streams during client callbacks, and as there may be new log types available
 * in any callback, the application log manages all the streams internally
 * rather than letting {@link CloudFoundryConsole} manage the streams.
 *
 */
public class ApplicationLogConsoleStream extends ConsoleStream implements ApplicationLogListener {


	private Map<LogContentType, ConsoleStream> logStreams = new HashMap<LogContentType, ConsoleStream>();

	private CloudFoundryServer cloudServer;

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

	public synchronized void initialiseStream(MessageConsole console, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) throws CoreException {

		if (appModule == null || cloudServer == null) {
			throw CloudErrorUtil.toCoreException(Messages.ERROR_FAILED_INITIALISE_APPLICATION_LOG_STREAM);
		}
		this.console = console;
		this.appModule = appModule;
		this.cloudServer = cloudServer;
		CloudFoundryServerBehaviour behaviour = cloudServer.getBehaviour();
		behaviour.addApplicationLogListener(appModule.getDeployedApplicationName(), this);

	}

	@Override
	public synchronized boolean isActive() {
		return !logStreams.isEmpty();
	}

	@Override
	protected IOConsoleOutputStream getActiveOutputStream() {
		// Cannot write to log console stream directly, as the log console
		// stream is a collection of streams, and there is never any one
		// "active" stream.
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

			stream = swtColour > -1 ? new SingleConsoleStream(new UILogConfig(swtColour)) : null;
			if (stream != null) {
				try {
					stream.initialiseStream(console, appModule, cloudServer);
					logStreams.put(type, stream);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
			}
		}
		return stream;
	}

	
	public void onMessage(ApplicationLog appLog) {
		CloudLog log = getCloudlog(appLog, null, null);
		if (log != null) {
			try {
				write(log);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
	}

	public synchronized void write(CloudLog log) throws CoreException {
		if (log == null) {
			return;
		}
		ConsoleStream stream = getStream(log);

		if (stream != null) {
			stream.write(log);
		}
	}

	public void onComplete() {
		// Nothing for now
	}

	public void onError(Throwable exception) {
		CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_APPLICATION_LOG,
				appModule != null ? appModule.getDeployedApplicationName() : Messages.UNKNOWN_APPLICATION,
				exception.getMessage()), exception);
	}

}
