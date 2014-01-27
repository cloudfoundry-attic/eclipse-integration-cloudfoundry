/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.wst.server.core.IServer;
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

}
