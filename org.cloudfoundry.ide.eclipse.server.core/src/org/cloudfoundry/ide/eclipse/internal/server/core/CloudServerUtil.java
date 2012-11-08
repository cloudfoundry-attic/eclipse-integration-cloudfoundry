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

}
