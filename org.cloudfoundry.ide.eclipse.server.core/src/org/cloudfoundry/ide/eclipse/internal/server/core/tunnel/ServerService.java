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

public class ServerService extends CommandMetaElement {

	private final List<ServiceCommand> commands;

	private final String serviceName;

	public ServerService(String serviceName) {
		super("ServerService");
		this.commands = new ArrayList<ServiceCommand>();
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public List<ServiceCommand> getLaunchCommands() {
		return commands;
	}

}
