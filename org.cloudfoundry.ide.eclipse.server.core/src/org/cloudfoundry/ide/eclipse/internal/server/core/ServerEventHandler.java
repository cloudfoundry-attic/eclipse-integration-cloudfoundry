package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/

/**
 * Fires server refresh events. Only one handler is active per workbench runtime
 * session.
 * 
 */
public class ServerEventHandler {

	private static ServerEventHandler handler;

	public static ServerEventHandler getDefault() {
		if (handler == null) {
			handler = new ServerEventHandler();
		}
		return handler;
	}

	private final List<CloudServerListener> applicationListeners = new CopyOnWriteArrayList<CloudServerListener>();

	public void addServerListener(CloudServerListener listener) {
		applicationListeners.add(listener);
	}

	public void removeServerListener(CloudServerListener listener) {
		applicationListeners.remove(listener);
	}

	public void fireInstancesUpdated(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_INSTANCES));
	}

	public void fireServicesUpdated(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_SERVICES));
	}

	public void firePasswordUpdated(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_PASSWORD));
	}

	public void fireServerRefreshed(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_SERVER_REFRESHED));
	}

	private void fireServerEvent(CloudServerEvent event) {
		CloudServerListener[] listeners = applicationListeners.toArray(new CloudServerListener[0]);
		for (CloudServerListener listener : listeners) {
			listener.serverChanged(event);
		}
	}
}
