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

public class ServicesServer extends CommandMetaElement {

	private final List<ServerService> services;

	private final String serverName;

	public ServicesServer(String serverName) {
		super("Server");
		this.serverName = serverName;
		services = new ArrayList<ServerService>();
	}

	public String getServerName() {
		return serverName;
	}

	public List<ServerService> getServices() {
		return services;
	}

}
