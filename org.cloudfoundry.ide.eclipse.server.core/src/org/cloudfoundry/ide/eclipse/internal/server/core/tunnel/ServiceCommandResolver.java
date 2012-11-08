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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerUtil;

public class ServiceCommandResolver {
	

	public List<ServicesServer> getServerServiceCommands() {
		List<CloudFoundryServer> cfServers = CloudServerUtil.getCloudServers();
		List<ServicesServer> servicesServer = new ArrayList<ServicesServer>();
		
		for (CloudFoundryServer cfServer : cfServers) {
			servicesServer.add(cfServer.getServicesServer());
		}
		
		return servicesServer;
	}

}
