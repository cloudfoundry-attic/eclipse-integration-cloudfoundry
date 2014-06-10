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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;

public class CloudServerUtil {

	private CloudServerUtil() {
		// util class
	}

	/**
	 * Returns list of cloud foundry server instances. May be emtpy, but not
	 * null.
	 * @return returns a non-null list of cloud foundry server instances. May be
	 * empty.
	 */
	public static List<CloudFoundryServer> getCloudServers() {
		IServer[] servers = ServerCore.getServers();
		Set<CloudFoundryServer> matchedServers = new HashSet<CloudFoundryServer>();

		if (servers != null) {
			for (IServer server : servers) {
				CloudFoundryServer cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
				if (cfServer != null) {
					matchedServers.add(cfServer);
				}
			}
		}

		return new ArrayList<CloudFoundryServer>(matchedServers);

	}

	/**
	 * 
	 * @param serverID unique ID of the server. This should not just be the name
	 * of the server, but the full id (e.g. for V2, it should include the space
	 * name)
	 * @return CloudFoundry server that corresponds to the given ID, or null if
	 * not found
	 */
	public static CloudFoundryServer getCloudServer(String serverID) {
		IServer[] servers = ServerCore.getServers();
		if (servers == null) {
			return null;
		}
		CloudFoundryServer cfServer = null;

		for (IServer server : servers) {
			cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			if (cfServer != null && cfServer.getServerId().equals(serverID)) {
				break;
			}
		}
		return cfServer;
	}

	/**
	 * Check if the server is a Cloud Foundry-based server
	 * @param server
	 * @return true if it is a Cloud Foundry server
	 */
	public static boolean isCloudFoundryServer(IServer server) {
		if (server != null) {
			return isCloudFoundryServerType(server.getServerType());
		}
		return false;
	}
	
	/**
	 * Check if the server type is a Cloud Foundry-based server type
	 * @param serverType
	 * @return true if it is a Cloud Foundry server type
	 */
	public static boolean isCloudFoundryServerType(IServerType serverType) {
		if (serverType != null) {
			String serverId = serverType.getId();
			return CloudFoundryBrandingExtensionPoint.getServerTypeIds().contains(serverId);
		}
		return false;
	}
}
