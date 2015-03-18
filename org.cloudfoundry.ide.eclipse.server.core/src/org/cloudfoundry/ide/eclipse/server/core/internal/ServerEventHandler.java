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

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudRefreshEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

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

	public void fireServicesUpdated(CloudFoundryServer server, List<CloudService> services) {
		fireServerEvent(new CloudRefreshEvent(server, null, CloudServerEvent.EVENT_UPDATE_SERVICES, services));
	}

	public void firePasswordUpdated(CloudFoundryServer server, IStatus status) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_PASSWORD, status));
	}

	public void fireServerRefreshed(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_SERVER_REFRESHED));
	}

	public void fireAppInstancesChanged(CloudFoundryServer server, IModule module) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_INSTANCES_UPDATED, module,
				Status.OK_STATUS));
	}

	public void fireApplicationRefreshed(CloudFoundryServer server, IModule module) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_APPLICATION_REFRESHED, module,
				Status.OK_STATUS));
	}

	public void fireAppDeploymentChanged(CloudFoundryServer server, IModule module) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED, module,
				Status.OK_STATUS));
	}

	public void fireError(CloudFoundryServer server, IModule module, IStatus status) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_CLOUD_OP_ERROR, module, status));
	}

	public synchronized void fireServerEvent(CloudServerEvent event) {
		CloudServerListener[] listeners = applicationListeners.toArray(new CloudServerListener[0]);
		for (CloudServerListener listener : listeners) {
			listener.serverChanged(event);
		}
	}
}
