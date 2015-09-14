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

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.MessageConsole;

/**
 * Cloud Foundry console manages various separate streams based on console
 * content type (e.g., STDOUT, STDERROR).
 * <p/>
 * The console will create and initialise streams from
 * {@link ConsoleStreamRegistry} for different console content types, allowing
 * adopters to user their own streams for common {@link LogContentType} like
 * {@link StandardLogContentType#STD_ERROR} or
 * {@link StandardLogContentType#STD_OUT}
 *
 */
public class CloudFoundryConsole {
	static final String ATTRIBUTE_SERVER = "org.cloudfoundry.ide.eclipse.server.Server"; //$NON-NLS-1$ 

	static final String ATTRIBUTE_APP = "org.cloudfoundry.ide.eclipse.server.CloudApp"; //$NON-NLS-1$ 

	static final String ATTRIBUTE_INSTANCE = "org.cloudfoundry.ide.eclipse.server.CloudInstance"; //$NON-NLS-1$ 

	static final String CONSOLE_TYPE = "org.cloudfoundry.ide.eclipse.server.appcloud"; //$NON-NLS-1$ 

	private Map<LogContentType, ConsoleStream> activeStreams = new HashMap<LogContentType, ConsoleStream>();

	private final ConsoleConfig config;

	public CloudFoundryConsole(ConsoleConfig config) {
		this.config = config;
	}

	public MessageConsole getConsole() {
		return config.getMessageConsole();
	}

	/**
	 * Initialises a stream based on the given log content type and prepares it
	 * for tailing
	 * @param contents to stream to the console
	 */
	public synchronized void startTailing(LogContentType type) {
		// initialise the stream for tailing
		try {
			getStream(type);
		}
		catch (CoreException ce) {
			CloudFoundryPlugin.logError(ce);
		}
	}

	/**
	 * Gets the stream for the given content type. This will request the console stream
	 * registry  for a stream provider for the given content type. If the
	 * stream was already created, it will return the existing cached stream. To
	 * clear the cache, the streams must fist be stopped through {@link #stop()}
	 * See {@link ConsoleStreamRegistry}
	 * @param type
	 * @return non-null stream for the given log content type.
	 * @throws CoreException if stream failed to initialise for the given type
	 */
	protected synchronized ConsoleStream getStream(LogContentType type) throws CoreException {

		if (type == null) {
			return null;
		}

		ConsoleStream stream = activeStreams.get(type);

		if (stream == null) {
			stream = ConsoleStreamRegistry.getInstance().getStream(type);

			if (stream == null) {
				throw CloudErrorUtil.toCoreException(NLS.bind(Messages.ERROR_NO_CONSOLE_STREAM_FOUND, type));
			}
			stream.initialiseStream(config);
			activeStreams.put(type, stream);
		}

		return stream;
	}

	/**
	 * Stops any further streaming of file content.
	 */
	public synchronized void stop() {
		for (Entry<LogContentType, ConsoleStream> entry : activeStreams.entrySet()) {
			entry.getValue().close();
		}
		activeStreams.clear();
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

	public synchronized void writeToStream(CloudLog log) {
		if (log == null) {
			return;
		}
		try {
			ConsoleStream stream = getStream(log.getLogType());
			stream.write(log);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

}
