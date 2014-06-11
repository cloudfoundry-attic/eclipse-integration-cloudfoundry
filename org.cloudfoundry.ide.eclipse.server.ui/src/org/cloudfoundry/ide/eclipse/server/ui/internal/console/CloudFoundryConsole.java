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

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.CloudLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.MessageConsole;

public class CloudFoundryConsole {
	static final String ATTRIBUTE_SERVER = "org.cloudfoundry.ide.eclipse.server.Server";

	static final String ATTRIBUTE_APP = "org.cloudfoundry.ide.eclipse.server.CloudApp";

	static final String ATTRIBUTE_INSTANCE = "org.cloudfoundry.ide.eclipse.server.CloudInstance";

	static final String CONSOLE_TYPE = "org.cloudfoundry.ide.eclipse.server.appcloud";

	private Map<LogContentType, ConsoleStream> activeStreams = new HashMap<LogContentType, ConsoleStream>();

	private final MessageConsole console;

	public CloudFoundryConsole(MessageConsole console) {
		this.console = console;
	}

	public MessageConsole getConsole() {
		return console;
	}

	/**
	 * Starts stream jobs and creates new output streams to the console for each
	 * file listed in the console contents.
	 * @param contents to stream to the console
	 */
	public synchronized void startTailing(LogContentType type, CloudFoundryApplicationModule appModule,
			CloudFoundryServer server) {
		ConsoleStream stream = getStream(type, appModule, server);
		if (stream == null) {
			CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_NO_CONSOLE_STREAM_FOUND, type));
		}

	}

	protected synchronized ConsoleStream getStream(LogContentType type, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {

		if (type == null) {
			return null;
		}

		ConsoleStream stream = activeStreams.get(type);

		if (stream == null) {
			stream = ConsoleStreamManager.getCurrent().getStream(type);
			if (stream != null) {
				try {
					stream.initialiseStream(getConsole(), appModule, cloudServer);
					activeStreams.put(type, stream);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
			}
		}

		return stream;
	}

	/**
	 * Stops any further streaming of file content. Stream jobs are terminated.
	 */
	public synchronized void stop() {
		for (Entry<LogContentType, ConsoleStream> entry : activeStreams.entrySet()) {
			entry.getValue().close();
		}
	}

	public synchronized void writeToStream(CloudLog log) {
		if (log == null) {
			return;
		}
		CloudFoundryServer server = (CloudFoundryServer) log.getAdapter(CloudFoundryServer.class);
		CloudFoundryApplicationModule appModule = (CloudFoundryApplicationModule) log
				.getAdapter(CloudFoundryApplicationModule.class);
		ConsoleStream stream = getStream(log.getLogType(), appModule, server);
		if (stream != null) {
			try {
				stream.write(log);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
	}

}
