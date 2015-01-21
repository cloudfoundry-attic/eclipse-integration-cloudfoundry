/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

	public synchronized void addServerListener(CloudServerListener listener) {
		if (listener != null && !applicationListeners.contains(listener)) {
			applicationListeners.add(listener);
		}
	}

	public synchronized void removeServerListener(CloudServerListener listener) {
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

	public synchronized void fireServerEvent(CloudServerEvent event) {
		CloudServerListener[] listeners = applicationListeners.toArray(new CloudServerListener[0]);
		for (CloudServerListener listener : listeners) {
			listener.serverChanged(event);
		}
	}
}
