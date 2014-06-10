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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.rse.internal;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.eclipse.rse.core.events.ISystemResourceChangeEvents;
import org.eclipse.rse.core.events.SystemResourceChangeEvent;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.search.ISearchService;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IHostFileToRemoteFileAdapter;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;



/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class ApplicationSubSystem extends FileServiceSubSystem implements CloudServerListener, IServerListener,
		IServerLifecycleListener {

	public ApplicationSubSystem(IHost host, IConnectorService connectorService, IFileService hostFileService,
			IHostFileToRemoteFileAdapter fileAdapter, ISearchService searchService) {
		super(host, connectorService, hostFileService, fileAdapter, searchService);
		supportsConnecting = false;
		host.setOffline(true);
		addServerListeners();
	}

	@Override
	public String getDescription() {
		return "This configuration allows you to work with files deployed to the cloud";
	}

	public void serverAdded(IServer server) {
		if (CloudFoundryRsePlugin.doesServerBelongToHost(server, getHost())) {
			server.addServerListener(this);
			fireEventChangeChildren();
		}
	}

	public void serverChanged(CloudServerEvent event) {
		int type = event.getType();
		if (type == CloudServerEvent.EVENT_UPDATE_INSTANCES) {
			fireEventChangeChildren();
		}
	}

	public void serverChanged(IServer server) {
		if (CloudFoundryRsePlugin.doesServerBelongToHost(server, getHost())) {
			fireEventChangeChildren();
		}
	}

	public void serverChanged(ServerEvent event) {
		if (CloudFoundryRsePlugin.doesServerBelongToHost(event.getServer(), getHost())) {
			if ((event.getKind() & ServerEvent.MODULE_CHANGE) != 0 && (event.getKind() & ServerEvent.STATE_CHANGE) != 0) {
				if (event.getState() == IServer.STATE_STARTED || event.getState() == IServer.STATE_STOPPED) {
					fireEventChangeChildren();
				}
			}
		}
	}

	public void serverRemoved(IServer server) {
		if (CloudFoundryRsePlugin.doesServerBelongToHost(server, getHost())) {
			server.removeServerListener(this);
			fireEventChangeChildren();
		}
	}

	private void addServerListeners() {
		IServer[] servers = ServerCore.getServers();
		for (int i = 0; i < servers.length; i++) {
			IServer candidate = servers[i];
			if (CloudFoundryRsePlugin.doesServerBelongToHost(candidate, getHost())) {
				candidate.addServerListener(this);
			}
		}
	}

	private void fireEventChangeChildren() {
		fireEvent(new SystemResourceChangeEvent(null, ISystemResourceChangeEvents.EVENT_CHANGE_CHILDREN, this));
	}

}
