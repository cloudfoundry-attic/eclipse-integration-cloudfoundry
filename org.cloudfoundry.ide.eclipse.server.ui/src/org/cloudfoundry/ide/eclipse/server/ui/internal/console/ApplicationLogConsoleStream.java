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
import org.cloudfoundry.client.lib.StreamingLogToken;
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
public class ApplicationLogConsoleStream extends ConsoleStream implements ApplicationLogListener {

	private Map<LogContentType, ConsoleStream> logStreams = new HashMap<LogContentType, ConsoleStream>();

	private CloudFoundryServer cloudServer;

	private CloudFoundryApplicationModule appModule;

	private StreamingLogToken loggregatorToken;

	public ApplicationLogConsoleStream() {
	}

	public synchronized void close() {
		if (logStreams != null) {
			for (Entry<LogContentType, ConsoleStream> entry : logStreams.entrySet()) {
				entry.getValue().close();
			}
			logStreams.clear();
		}
		// Also cancel any further loggregator streaming
		if (loggregatorToken != null) {
			loggregatorToken.cancel();
			loggregatorToken = null;
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

		// This token represents the loggregator connection.
		loggregatorToken = behaviour.addApplicationLogListener(appModule.getDeployedApplicationName(), this);
	}

	@Override
	public synchronized boolean isActive() {
		return loggregatorToken != null;
	}

	@Override
	protected IOConsoleOutputStream getActiveOutputStream() {
		// There is no single active outstream for the manager, as the manager
		// manages
		// various streams internally for each type of loggregator content.
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
		// Only log if the console manager is active. This is a
		// workaround
		// for cases where canceling loggregator connection is not immediate,
		// and the callback still is invoked asynchronously
		// even after all the managed console streams are closed.
		if (log == null || !isActive()) {
			return;
		}
		ConsoleStream stream = getStream(log);

		// Even if the manager is active, any individual stream may be closed,
		// therefore always check that
		// the associated stream for the given log type is active before
		// streaming content to it.
		if (stream != null && stream.isActive()) {
			stream.write(log);
		}
	}

	public void onComplete() {
		// Nothing for now
	}

	public void onError(Throwable exception) {
		// Only log errors if the stream manager is active. This prevents errors
		// to be continued to be displayed by the asynchronous loggregator
		// callback after the stream
		// manager has closed.
		if (isActive()) {
			CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_APPLICATION_LOG,
					appModule != null ? appModule.getDeployedApplicationName() : Messages.UNKNOWN_APPLICATION,
					exception.getMessage()), exception);
		}
	}
}
