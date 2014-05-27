/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 *     IBM - add external finder method for console
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.trace.ITraceType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IServer;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class ConsoleManager {

	private IConsoleManager consoleManager;

	Map<String, CloudFoundryConsole> consoleByUri;

	private CloudFoundryTraceConsole traceConsole;

	private final IConsoleListener listener = new IConsoleListener() {

		public void consolesAdded(IConsole[] consoles) {
			// ignore

		}

		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (CloudFoundryConsole.CONSOLE_TYPE.equals(console.getType())) {
					Object server = ((MessageConsole) console).getAttribute(CloudFoundryConsole.ATTRIBUTE_SERVER);
					Object app = ((MessageConsole) console).getAttribute(CloudFoundryConsole.ATTRIBUTE_APP);
					Object index = ((MessageConsole) console).getAttribute(CloudFoundryConsole.ATTRIBUTE_INSTANCE);
					if (server instanceof IServer && app instanceof CloudFoundryApplicationModule
							&& index instanceof Integer) {
						stopConsole((IServer) server, (CloudFoundryApplicationModule) app, (Integer) index);
					}
				}
				else if (CloudFoundryTraceConsole.TRACE_CONSOLE_ID.equals(console.getType())
						&& (traceConsole != null)) {
					traceConsole.close();
					traceConsole = null;
				}
			}
		}
	};

	private static ConsoleManager instance = new ConsoleManager();

	public static ConsoleManager getInstance() {
		return instance;
	}

	public ConsoleManager() {
		consoleByUri = new HashMap<String, CloudFoundryConsole>();
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.addConsoleListener(listener);
	}

	/**
	 * @param server
	 * @param app
	 * @param instanceIndex
	 * @param show
	 * 
	 * Start console if show is true, otherwise reset and start only if console
	 * was previously created already
	 */
	public void startConsole(CloudFoundryServer server, IConsoleContents contents,
			CloudFoundryApplicationModule appModule, int instanceIndex, boolean show, boolean clear) {
		CloudFoundryConsole serverLogTail = getCloudFoundryConsole(server, appModule, instanceIndex);

		if (serverLogTail != null) {
			if (clear) {
				serverLogTail.getConsole().clearConsole();
			}
			serverLogTail.startTailing(contents.getContents(server, appModule.getDeployedApplicationName(),
					instanceIndex));
		}

		if (show && serverLogTail != null) {
			consoleManager.showConsoleView(serverLogTail.getConsole());
		}
	}

	protected CloudFoundryConsole getCloudFoundryConsole(CloudFoundryServer server,
			CloudFoundryApplicationModule appModule, int instanceIndex) {
		String appUrl = getConsoleId(server.getServer(), appModule, instanceIndex);
		CloudFoundryConsole serverLogTail = consoleByUri.get(appUrl);
		if (serverLogTail == null) {

			MessageConsole appConsole = getApplicationConsole(server, appModule, instanceIndex);

			serverLogTail = new CloudFoundryConsole(appModule, appConsole);
			consoleByUri.put(getConsoleId(server.getServer(), appModule, instanceIndex), serverLogTail);
		}
		return serverLogTail;
	}
	
	/**
	 * Find the message console that corresponds to the server and a given module. If there are multiple instances
	 * of the application, only the first one will get returned.
	 * @param server the server for that console
	 * @param appModule the app for that console
	 * @return the message console. Null if no corresponding console is found.
	 */
	public MessageConsole findCloudFoundryConsole(IServer server, CloudFoundryApplicationModule appModule) {
		String curConsoleId = getConsoleId(server, appModule, 0);
		if (curConsoleId != null) {
			return consoleByUri.get(curConsoleId).getConsole();
		}
		return null;
	}

	public void synchWriteToStd(String message, CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			int instanceIndex, boolean clear, boolean isError, IProgressMonitor monitor) {
		CloudFoundryConsole serverLogTail = getCloudFoundryConsole(server, appModule, instanceIndex);

		if (serverLogTail != null) {
			if (clear) {
				serverLogTail.getConsole().clearConsole();
			}

			if (isError) {
				serverLogTail.synchWriteToStdError(message, monitor);
			}
			else {
				serverLogTail.synchWriteToStdOut(message, monitor);
			}
			consoleManager.showConsoleView(serverLogTail.getConsole());
		}
	}

	/**
	 * Makes the general Cloud Foundry trace console visible in the console
	 * view.
	 */
	public void setTraceVisible() {
		CloudFoundryTraceConsole console = getTraceConsoleStream();
		if (console != null) {
			consoleManager.showConsoleView(console.getMessageConsole());
		}
	}

	/**
	 * Sends a trace message to a Cloud Foundry trace console.
	 * 
	 * @param message if null, nothing is written to trace console.
	 * @param type type of trace. It is used to determine whether a specific
	 * format/colour should be used for the message.
	 * @param server Cloud Foundry server where tracing should occur. If null,
	 * tracing message will be sent to the general Cloud Foundry trace console.
	 * @param clear whether trace console should be cleared prior to displaying
	 * the trace message.
	 */
	public synchronized void writeTrace(String message, ITraceType type, CloudFoundryServer server, boolean clear) {
		try {
			CloudFoundryTraceConsole console = getTraceConsoleStream();

			if (console != null) {
				// Do not make trace visible as another console may be visible
				// while
				// tracing is occuring.
				console.tail(message, type);
			}
		}
		catch (Throwable e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	public void stopConsole(IServer server, CloudFoundryApplicationModule appModule, int instanceIndex) {
		String appUrl = getConsoleId(server, appModule, instanceIndex);
		CloudFoundryConsole serverLogTail = consoleByUri.get(appUrl);
		if (serverLogTail != null) {
			serverLogTail.stop();
			consoleByUri.remove(appUrl);
		}
	}

	public void stopConsoles() {
		for (Entry<String, CloudFoundryConsole> tailEntry : consoleByUri.entrySet()) {
			tailEntry.getValue().stop();
		}
		consoleByUri.clear();
		if (traceConsole != null) {
			traceConsole.close();
			traceConsole = null;
		}
	}

	public static MessageConsole getApplicationConsole(CloudFoundryServer server,
			CloudFoundryApplicationModule appModule, int instanceIndex) {
		MessageConsole appConsole = null;
		String consoleName = getConsoleId(server.getServer(), appModule, instanceIndex);
		for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
			if (console instanceof MessageConsole && console.getName().equals(consoleName)) {
				appConsole = (MessageConsole) console;
			}
		}
		if (appConsole == null) {
			appConsole = new MessageConsole(getConsoleDisplayName(server, appModule, instanceIndex),
					CloudFoundryConsole.CONSOLE_TYPE, null, true);
			appConsole.setAttribute(CloudFoundryConsole.ATTRIBUTE_SERVER, server);
			appConsole.setAttribute(CloudFoundryConsole.ATTRIBUTE_APP, appModule);
			appConsole.setAttribute(CloudFoundryConsole.ATTRIBUTE_INSTANCE, instanceIndex);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { appConsole });
		}

		return appConsole;
	}

	protected synchronized CloudFoundryTraceConsole getTraceConsoleStream() {

		if (traceConsole == null) {
			MessageConsole messageConsole = null;
			for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
				if (console instanceof MessageConsole
						&& console.getName().equals(CloudFoundryTraceConsole.CLOUD_FOUNDRY_TRACE_CONSOLE_NAME)) {
					messageConsole = (MessageConsole) console;
				}
			}
			if (messageConsole == null) {
				messageConsole = new MessageConsole(CloudFoundryTraceConsole.CLOUD_FOUNDRY_TRACE_CONSOLE_NAME,
						CloudFoundryTraceConsole.TRACE_CONSOLE_ID, null, true);
			}
			traceConsole = new CloudFoundryTraceConsole(messageConsole);
			traceConsole.initialiseStreams();

			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });

		}

		return traceConsole;
	}

	public static String getConsoleId(IServer server, CloudFoundryApplicationModule appModule, int instanceIndex) {
		return server.getId() + "/" + appModule.getDeployedApplicationName() + "#" + instanceIndex;
	}

	public static String getConsoleDisplayName(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			int instanceIndex) {
		StringWriter writer = new StringWriter();
		writer.append(server.getServer().getName());
		writer.append('-');

		CloudFoundrySpace space = server.getCloudFoundrySpace();

		if (space != null) {
			writer.append('-');
			writer.append(space.getOrgName());
			writer.append('-');
			writer.append('-');
			writer.append(space.getSpaceName());
			writer.append('-');
			writer.append('-');
		}

		writer.append(appModule.getDeployedApplicationName());
		writer.append('#');
		writer.append(instanceIndex + "");
		return writer.toString();
	}

}
