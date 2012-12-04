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
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.util.ArrayList;
import java.util.List;

/**
 * Using getters and setters for JSON serialisation.
 * 
 */
public class ServicesServer {

	private List<ServerService> services;

	private String serverName;
	
	private String serverID;

	public ServicesServer() {

		this.services = new ArrayList<ServerService>();
	}

	public String getServerName() {
		return serverName;
	}
	
	public String getServerID() {
		return serverID;
	}

	public List<ServerService> getServices() {
		return services;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	public void setServerID(String serverID) {
		this.serverID = serverID;
	}

	public void setServices(List<ServerService> services) {
		this.services = services;

	}

}
