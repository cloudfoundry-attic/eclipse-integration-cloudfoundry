/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
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
 * 
 * Using getters and setters with no-argument constructors for JSON serialisation
 * 
 */
public class TunnelServiceCommands {

	private List<ServerService> services;

	public TunnelServiceCommands() {
		services = new ArrayList<ServerService>(0);
	}

	/**
	 * Will never be null.
	 * @return non-null list of services
	 */
	public List<ServerService> getServices() {
		return services;
	}

	/**
	 * Setting null will set an empty list of services.
	 * @param services
	 */
	public void setServices(List<ServerService> services) {
		this.services = services != null ? services : new ArrayList<ServerService>();
	}

}
