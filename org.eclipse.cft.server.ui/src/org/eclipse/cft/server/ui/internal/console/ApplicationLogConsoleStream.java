/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Unlike {@link SingleConsoleStream}, that is associated with only one stream
 * content type, the application log console stream is actually a collection of
 * separate streams, one for each type of loggregator content type (e.g. STDOUT,
 * STDERROR,..) , all which are received through the same loggregator listener
 * and managed by one single loggregator token. This is why there aren't
 * separate {@link ConsoleStream} for each loggregator content type, as all
 * loggregator content are received through the same callback registered in the
 * {@link CloudFoundryServerBehaviour}
 * 
 * 
 * <p/>
 * Closing the manager closes all active streams, as well as cancels any further
 * loggregator callbacks.
 * 
 *
 */
public class ApplicationLogConsoleStream extends ConsoleStream {

	/*
	 * Log content types that are specific to streaming or fetching recent logs
	 * of published applications that are currently running on the Cloud server
	 */
	protected static final LogContentType APPLICATION_LOG_STS_ERROR = new LogContentType("applicationlogstderror"); //$NON-NLS-1$

	protected static final LogContentType APPLICATION_LOG_STD_OUT = new LogContentType("applicationlogstdout"); //$NON-NLS-1$

	protected static final LogContentType APPLICATION_LOG_UNKNOWN = new LogContentType("applicationlogunknown"); //$NON-NLS-1$

	private StreamingLogToken loggregatorToken;

	private Map<LogContentType, ConsoleStream> logStreams = new HashMap<LogContentType, ConsoleStream>();

	private ConsoleConfig consoleDescriptor;

	public ApplicationLogConsoleStream() {

	}

	public synchronized void close() {
		if (logStreams != null) {
			for (Entry<LogContentType, ConsoleStream> entry : logStreams.entrySet()) {
				entry.getValue().close();
			}
			logStreams.clear();
		}
		if (loggregatorToken != null) {
			loggregatorToken.cancel();
			loggregatorToken = null;
		}
	}

	public synchronized void initialiseStream(ConsoleConfig descriptor) throws CoreException {

		if (descriptor == null) {
			throw CloudErrorUtil.toCoreException(Messages.ERROR_FAILED_INITIALISE_APPLICATION_LOG_STREAM);
		}
		this.consoleDescriptor = descriptor;

		if (loggregatorToken == null) {

			CloudFoundryServerBehaviour behaviour = consoleDescriptor.getCloudServer().getBehaviour();

			// This token represents the loggregator connection.
			loggregatorToken = behaviour.addApplicationLogListener(consoleDescriptor.getCloudApplicationModule()
					.getDeployedApplicationName(), new ApplicationLogConsoleListener());

		}
	}

	@Override
	public synchronized boolean isActive() {
		return loggregatorToken != null;
	}

	@Override
	protected IOConsoleOutputStream getOutputStream(LogContentType type) {

		ConsoleStream consoleStream = getApplicationLogStream(type);
		if (consoleStream != null && consoleStream.isActive()) {
			return consoleStream.getOutputStream(type);
		}
		return null;
	}

	protected static String format(String message) {
		if (message.contains("\n") || message.contains("\r")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return message;
		}
		return message + '\n';
	}

	/**
	 * Gets the stream associated with the given cloud log type
	 * @param log
	 * @return stream associated with the given cloud log type, or null if log
	 * type is not supported
	 */
	protected synchronized ConsoleStream getApplicationLogStream(LogContentType type) {

		if (type == null) {
			return null;
		}

		ConsoleStream stream = logStreams.get(type);
		if (stream == null) {

			int swtColour = -1;

			if (APPLICATION_LOG_STS_ERROR.equals(type)) {
				swtColour = SWT.COLOR_RED;
			}
			else if (APPLICATION_LOG_STD_OUT.equals(type)) {
				swtColour = SWT.COLOR_DARK_GREEN;
			}
			else if (APPLICATION_LOG_UNKNOWN.equals(type)) {
				swtColour = SWT.COLOR_BLACK;
			}

			if (swtColour > -1) {

				try {
					stream = new SingleConsoleStream(new UILogConfig(swtColour));
					stream.initialiseStream(consoleDescriptor);
					logStreams.put(type, stream);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
			}
		}
		return stream;
	}

	/**
	 * Listener that receives loggregator content and sends it to the
	 * appropriate stream.
	 *
	 */
	public class ApplicationLogConsoleListener implements ApplicationLogListener {

		public void onMessage(ApplicationLog appLog) {
			if (isActive()) {
				try {
					write(appLog);
				}
				catch (CoreException e) {
					onError(e);
				}
			}
		}

		public void onComplete() {
			// Nothing for now
		}

		public void onError(Throwable exception) {
			// Only log errors if the stream manager is active. This prevents
			// errors
			// to be continued to be displayed by the asynchronous loggregator
			// callback after the stream
			// manager has closed.
			if (isActive()) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_APPLICATION_LOG, consoleDescriptor
						.getCloudApplicationModule().getDeployedApplicationName(), exception.getMessage()), exception);
			}
		}
	}

	public CloudLog getCloudlog(ApplicationLog appLog) {
		if (appLog == null) {
			return null;
		}
		org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType type = appLog.getMessageType();
		LogContentType contentType = APPLICATION_LOG_UNKNOWN;
		if (type != null) {
			switch (type) {
			case STDERR:
				contentType = APPLICATION_LOG_STS_ERROR;
				break;
			case STDOUT:
				contentType = APPLICATION_LOG_STD_OUT;
				break;
			}
		}

		return new CloudLog(format(appLog.getMessage()), contentType);

	}

	/**
	 * Writes a loggregator application log to the console. The content type of
	 * the application log is resolved first and a corresponding stream is
	 * fetched or created as part of streaming the log message to the console.
	 */
	public synchronized void write(ApplicationLog appLog) throws CoreException {
		if (appLog == null) {
			return;
		}

		// Convert it to a CloudLog that contains the appropriate log content
		// type
		CloudLog log = getCloudlog(appLog);
		IOConsoleOutputStream activeOutStream = getOutputStream(log.getLogType());

		if (activeOutStream != null && log.getMessage() != null) {
			try {
				activeOutStream.write(log.getMessage());
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}
	}

}
