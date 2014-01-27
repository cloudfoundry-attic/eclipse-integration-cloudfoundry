/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON
 * serialisation
 * 
 */
public class TunnelServiceCommands implements ITunnelServiceCommands {

	private List<ServerService> services;

	private CommandTerminal defaultTerminal;

	public TunnelServiceCommands() {
		services = new ArrayList<ServerService>(0);
	}

	public CommandTerminal getDefaultTerminal() {
		return defaultTerminal;
	}

	public void setDefaultTerminal(CommandTerminal defaultTerminal) {
		this.defaultTerminal = defaultTerminal;
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
