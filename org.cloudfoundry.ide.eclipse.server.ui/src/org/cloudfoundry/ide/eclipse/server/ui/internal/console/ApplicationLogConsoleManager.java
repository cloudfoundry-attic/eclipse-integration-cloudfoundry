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
 *     IBM - add external finder method for console
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudFoundrySpace;
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
public class ApplicationLogConsoleManager extends CloudConsoleManager {

	private IConsoleManager consoleManager;

	Map<String, ApplicationLogConsole> consoleByUri;

	private final IConsoleListener listener = new IConsoleListener() {

		public void consolesAdded(IConsole[] consoles) {
			// ignore
		}

		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (ApplicationLogConsole.CONSOLE_TYPE.equals(console.getType())) {
					Object server = ((MessageConsole) console).getAttribute(ApplicationLogConsole.ATTRIBUTE_SERVER);
					Object app = ((MessageConsole) console).getAttribute(ApplicationLogConsole.ATTRIBUTE_APP);
					Object index = ((MessageConsole) console).getAttribute(ApplicationLogConsole.ATTRIBUTE_INSTANCE);
					if (server instanceof IServer && app instanceof CloudFoundryApplicationModule
							&& index instanceof Integer) {
						stopConsole((IServer) server, (CloudFoundryApplicationModule) app, (Integer) index);
					}
				}
			}
		}
	};

	public ApplicationLogConsoleManager() {
		consoleByUri = new HashMap<String, ApplicationLogConsole>();
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.addConsoleListener(listener);
	}

	@Override
	public void startConsole(CloudFoundryServer server, LogContentType type, CloudFoundryApplicationModule appModule,
			int instanceIndex, boolean show, boolean clear, IProgressMonitor monitor) {
		// As of 1.7.2, instances are not used for loggregator. Loggregator
		// shows content for all instances in the same console
		CloudFoundryConsole serverLogTail = getApplicationLogConsole(server, appModule);

		if (serverLogTail != null) {
			if (clear) {
				serverLogTail.getConsole().clearConsole();
			}
			serverLogTail.startTailing(type, appModule, server);
		}

		if (show && serverLogTail != null) {
			consoleManager.showConsoleView(serverLogTail.getConsole());
		}
	}

	protected synchronized ApplicationLogConsole getApplicationLogConsole(CloudFoundryServer server,
			CloudFoundryApplicationModule appModule) {

		String appUrl = getConsoleId(server.getServer(), appModule);
		ApplicationLogConsole serverLogTail = consoleByUri.get(appUrl);
		if (serverLogTail == null) {

			MessageConsole appConsole = getApplicationConsole(server, appModule);

			serverLogTail = new ApplicationLogConsole(appConsole);
			consoleByUri.put(getConsoleId(server.getServer(), appModule), serverLogTail);
		}
		return serverLogTail;
	}

	// public String getConsoleName() {
	// CloudApplication cloudApp = app != null ? app.getApplication() : null;
	// String name = (cloudApp != null && cloudApp.getUris() != null &&
	// cloudApp.getUris().size() > 0) ? cloudApp
	// .getUris().get(0) : app.getDeployedApplicationName();
	// return name;
	// }

	@Override
	public MessageConsole findCloudFoundryConsole(IServer server, CloudFoundryApplicationModule appModule) {
		String curConsoleId = getConsoleId(server, appModule);
		if (curConsoleId != null) {
			return consoleByUri.get(curConsoleId).getConsole();
		}
		return null;
	}

	@Override
	public void writeToStandardConsole(String message, CloudFoundryServer server,
			CloudFoundryApplicationModule appModule, int instanceIndex, boolean clear, boolean isError) {
		// As of 1.7.2, instances are not used for loggregator. Loggregator
		// shows content for all instances in the same console
		CloudFoundryConsole serverLogTail = getApplicationLogConsole(server, appModule);

		if (serverLogTail instanceof ApplicationLogConsole) {

			ApplicationLogConsole logConsole = (ApplicationLogConsole) serverLogTail;
			if (clear) {
				serverLogTail.getConsole().clearConsole();
			}

			if (isError) {
				logConsole.writeToStdError(message);
			}
			else {
				logConsole.writeToStdOut(message);
			}
			consoleManager.showConsoleView(serverLogTail.getConsole());
		}
	}

	@Override
	public void showCloudFoundryLogs(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			int instanceIndex, boolean clear, IProgressMonitor monitor) {
		if (appModule == null || server == null) {
			return;
		}
		// As of 1.7.2, instances are not used for loggregator. Loggregator
		// shows content for all instances in the same console
		ApplicationLogConsole console = getApplicationLogConsole(server, appModule);
		if (console != null) {
			if (clear) {
				console.getConsole().clearConsole();
			}
			CloudFoundryServerBehaviour behaviour = server.getBehaviour();
			List<ApplicationLog> logs = behaviour.getRecentApplicationLogs(appModule.getDeployedApplicationName());
			console.writeApplicationLogs(logs, appModule, server);
		}
	}

	@Override
	public void stopConsole(IServer server, CloudFoundryApplicationModule appModule, int instanceIndex) {
		String appUrl = getConsoleId(server, appModule);
		CloudFoundryConsole serverLogTail = consoleByUri.get(appUrl);
		if (serverLogTail != null) {

			consoleByUri.remove(appUrl);

			serverLogTail.stop();

			MessageConsole messageConsole = serverLogTail.getConsole();
			consoleManager.removeConsoles(new IConsole[] { messageConsole });
			messageConsole.destroy();
		}
	}

	@Override
	public void stopConsoles() {
		for (Entry<String, ApplicationLogConsole> tailEntry : consoleByUri.entrySet()) {
			tailEntry.getValue().stop();
		}
		consoleByUri.clear();
	}

	public static MessageConsole getApplicationConsole(CloudFoundryServer server,
			CloudFoundryApplicationModule appModule) {
		MessageConsole appConsole = null;
		String consoleName = getConsoleId(server.getServer(), appModule);
		for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
			if (console instanceof MessageConsole && console.getName().equals(consoleName)) {
				appConsole = (MessageConsole) console;
			}
		}
		if (appConsole == null) {
			appConsole = new MessageConsole(getConsoleDisplayName(server, appModule),
					ApplicationLogConsole.CONSOLE_TYPE, null, true);
			appConsole.setAttribute(ApplicationLogConsole.ATTRIBUTE_SERVER, server);
			appConsole.setAttribute(ApplicationLogConsole.ATTRIBUTE_APP, appModule);
			// appConsole.setAttribute(ApplicationLogConsole.ATTRIBUTE_INSTANCE,
			// instanceIndex);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { appConsole });
		}

		return appConsole;
	}

	public static String getConsoleId(IServer server, CloudFoundryApplicationModule appModule) {
		return server.getId() + "/" + appModule.getDeployedApplicationName(); //$NON-NLS-1$
	}

	public static String getConsoleDisplayName(CloudFoundryServer server, CloudFoundryApplicationModule appModule) {
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
		return writer.toString();
	}
}
