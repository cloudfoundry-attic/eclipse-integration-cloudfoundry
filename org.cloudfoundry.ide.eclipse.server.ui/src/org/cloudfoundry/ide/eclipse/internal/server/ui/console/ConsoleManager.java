/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
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
					if (server instanceof
							IServer && app instanceof CloudApplication && index instanceof Integer) {
						stopConsole((IServer) server, (CloudApplication) app, (Integer) index);
					}
				}
			}

		}
	};

	private static ConsoleManager instance = new ConsoleManager();

	public static ConsoleManager getInstance() {
		return instance;
	}

	Map<String, CloudFoundryConsole> consoleByUri;

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
	 * Start console if show is true, otherwise reset and start only if console was previously created already
	 */
	public void startConsole(CloudFoundryServer server, CloudApplication app, int instanceIndex, boolean show) {
		String appUrl = getConsoleId(server.getServer(), app, instanceIndex);
		CloudFoundryConsole serverLogTail = consoleByUri.get(appUrl);
		if (serverLogTail == null && show) {
			MessageConsole appConsole = getOrCreateConsole(server.getServer(), app, instanceIndex);
			serverLogTail = new CloudFoundryConsole(server, app, instanceIndex, appConsole);
			consoleByUri.put(getConsoleId(server.getServer(), app, instanceIndex), serverLogTail);
		}
		
		if (serverLogTail != null) {
			serverLogTail.resetConsole();
			serverLogTail.startTailing();
		}
		
		if (show && serverLogTail != null) {
			consoleManager.showConsoleView(serverLogTail.getConsole());
		}
	}

	public void stopConsole(IServer server, CloudApplication app, int instanceIndex) {
		String appUrl = getConsoleId(server, app, instanceIndex);
		CloudFoundryConsole serverLogTail = consoleByUri.get(appUrl);
		if (serverLogTail != null) {
			serverLogTail.stopTailing();
			consoleByUri.remove(appUrl);
		}
	}

	public void stopConsoles() {
		for (Entry<String, CloudFoundryConsole> tailEntry : consoleByUri.entrySet()) {
			tailEntry.getValue().stopTailing();
		}
	}

	private MessageConsole getOrCreateConsole(IServer server, CloudApplication app, int instanceIndex) {
		MessageConsole appConsole = null;
		String consoleName = getConsoleId(server, app, instanceIndex);
		for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
			if (console instanceof MessageConsole && console.getName().equals(consoleName)) {
				appConsole = (MessageConsole) console;
			}
		}
		if (appConsole == null) {
			appConsole = new MessageConsole(app.getName() + "#" + instanceIndex, CloudFoundryConsole.CONSOLE_TYPE, null, true);
			appConsole.setAttribute(CloudFoundryConsole.ATTRIBUTE_SERVER, server);
			appConsole.setAttribute(CloudFoundryConsole.ATTRIBUTE_APP, app);
			appConsole.setAttribute(CloudFoundryConsole.ATTRIBUTE_INSTANCE, instanceIndex);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { appConsole });
		}

		return appConsole;
	}

	String getConsoleId(IServer server, CloudApplication app, int instanceIndex) {
		return server.getId() + "/" + app.getName() + "#" + instanceIndex;
	}

}
