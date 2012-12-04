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

import java.util.List;

/**
 * Using getters and setters for JSON serialisation.
 * 
 */
public class ServerService {

	private List<ServiceCommand> commands;

	private String serviceName;

	public ServerService() {

	}

	public String getServiceName() {
		return serviceName;
	}

	public List<ServiceCommand> getCommands() {
		return commands;
	}

	public void setCommands(List<ServiceCommand> commands) {
		this.commands = commands;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}
