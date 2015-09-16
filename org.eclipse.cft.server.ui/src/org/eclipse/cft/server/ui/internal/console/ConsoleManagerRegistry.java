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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.cft.server.ui.internal.console.file.FileConsoleManager;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

/**
 * 
 * Registry that loads console managers for different servers. For example,
 * certain servers may use log file streaming whereas others application log
 * callbacks for loggregator. Based on the server, the registry will load the
 * appropriate console manager.
 * <p/>
 * In addition, the registry also manages a common trace console that is
 * applicable to any server.
 */
public class ConsoleManagerRegistry {

	public static final String CLOUD_FOUNDRY_TRACE_CONSOLE_NAME = "Cloud Foundry Trace"; //$NON-NLS-1$ 

	static final String TRACE_CONSOLE_ID = "org.cloudfoundry.ide.eclipse.server.trace"; //$NON-NLS-1$ 

	private static ConsoleManagerRegistry registry;

	private CloudFoundryConsole traceConsole;

	private IConsoleManager consoleManager;

	/**
	 * Loggregator-supporting console manager
	 */
	private CloudConsoleManager appConsoleManager = new ApplicationLogConsoleManager();

	/**
	 * Log file streaming console manager.
	 */
	private CloudConsoleManager fileConsoleManager = new FileConsoleManager();

	private final IConsoleListener listener = new IConsoleListener() {

		public void consolesAdded(IConsole[] consoles) {
			// ignore

		}

		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (TRACE_CONSOLE_ID.equals(console.getType()) && (traceConsole != null)) {
					traceConsole.stop();
					traceConsole = null;
				}
			}
		}
	};

	public ConsoleManagerRegistry() {
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.addConsoleListener(listener);
	}

	public static ConsoleManagerRegistry getInstance() {
		if (registry == null) {
			registry = new ConsoleManagerRegistry();
		}
		return registry;
	}

	public static CloudConsoleManager getConsoleManager(CloudFoundryServer cloudServer) {
		return getInstance().getCloudConsoleManager(cloudServer);
	}

	/*
	 * INSTANCE METHODS
	 */
	/**
	 * Returns a console manager appropriate for the given server. If the server
	 * uses log file streaming , then a file console manager is returned.
	 * <p/>
	 * Otherwise, if the server uses loggregator or log callbacks, or it's not
	 * possible to determine the logging mechanism of the server, by default an
	 * application log console manager (i.e. a console manager that uses
	 * callbacks to obtain application logs) is returned.
	 * @param cloudServer
	 * @return non-null console manager based on the server type.
	 */
	public CloudConsoleManager getCloudConsoleManager(CloudFoundryServer cloudServer) {
		if (usesLogFileStreaming(cloudServer)) {
			return getFileConsoleManager();
		}
		else {
			return appConsoleManager;
		}
	}

	/**
	 * Convenience method for adopters to obtain a file console manager IFF the
	 * adopters are sure their servers use log file streaming.
	 * <p/>
	 * Otherwise, callers should ONLY use
	 * {@link #getCloudConsoleManager(CloudFoundryServer)} to obtain the
	 * appropriate console manager for their server.
	 * @return Log file console manager.
	 */
	public CloudConsoleManager getFileConsoleManager() {
		return fileConsoleManager;
	}

	protected boolean usesLogFileStreaming(CloudFoundryServer cloudServer) {
		return false;
	}

	/**
	 * Makes the general Cloud Foundry trace console visible in the console
	 * view.
	 */
	public void setTraceConsoleVisible() {
		CloudFoundryConsole console = getTraceConsoleStream();
		if (console != null) {
			consoleManager.showConsoleView(console.getConsole());
		}
	}

	/**
	 * Sends a trace message to a Cloud Foundry trace console.
	 * 
	 * @param log if null, nothing is written to trace console.
	 * @param clear whether trace console should be cleared prior to displaying
	 * the trace message.
	 */
	public void trace(CloudLog log, boolean clear) {
		if (log == null) {
			return;
		}
		try {
			CloudFoundryConsole console = getTraceConsoleStream();

			if (console != null) {
				// Do not make trace visible as another console may be visible
				// while
				// tracing is occuring.
				console.writeToStream(log);
			}
		}
		catch (Throwable e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	protected synchronized CloudFoundryConsole getTraceConsoleStream() {

		if (traceConsole == null) {
			MessageConsole messageConsole = null;
			for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
				if (console instanceof MessageConsole && console.getName().equals(CLOUD_FOUNDRY_TRACE_CONSOLE_NAME)) {
					messageConsole = (MessageConsole) console;
				}
			}
			if (messageConsole == null) {
				messageConsole = new MessageConsole(CLOUD_FOUNDRY_TRACE_CONSOLE_NAME, TRACE_CONSOLE_ID, null, true);
			}
			traceConsole = new CloudFoundryConsole(new ConsoleConfig(messageConsole, null, null));

			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });

		}

		return traceConsole;
	}

}
