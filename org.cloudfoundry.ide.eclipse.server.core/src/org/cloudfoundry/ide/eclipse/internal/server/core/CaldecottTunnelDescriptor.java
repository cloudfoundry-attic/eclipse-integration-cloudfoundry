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

import org.cloudfoundry.caldecott.client.TunnelServer;

public class CaldecottTunnelDescriptor {

	private final String userName;

	private final String password;

	private final int tunnelPort;

	private final TunnelServer server;

	private final String serviceName;

	public CaldecottTunnelDescriptor(String userName, String password, String serviceName, TunnelServer server,
			int tunnelPort) {
		this.server = server;
		this.userName = userName;
		this.password = password;
		this.tunnelPort = tunnelPort;
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public int tunnelPort() {
		return tunnelPort;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public TunnelServer getTunnelServer() {
		return server;
	}
}
