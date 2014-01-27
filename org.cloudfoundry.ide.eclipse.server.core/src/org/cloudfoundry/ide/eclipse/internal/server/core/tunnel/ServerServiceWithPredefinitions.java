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

import java.util.List;

/**
 * Wrapper around a service that also includes a list of pre-defined commands.
 * This additional information is subtyped as it is not meant to be persisted.
 */
public class ServerServiceWithPredefinitions extends ServerService {

	private final PredefinedServiceCommands predefined;

	private final ServerService original;

	public ServerServiceWithPredefinitions(ServerService original, PredefinedServiceCommands predefined) {
		this.predefined = predefined;
		this.original = original;
	}

	public ServerService getOriginal() {
		return original;
	}

	public List<ServiceCommand> getPredefinedCommands() {
		return predefined.getPredefinedCommands(getServiceInfo());
	}

	public List<ServiceCommand> getCommands() {
		return original.getCommands();
	}

	public void setCommands(List<ServiceCommand> commands) {
		original.setCommands(commands);
	}

	public ServiceInfo getServiceInfo() {
		return original.getServiceInfo();
	}

	public void setServiceInfo(ServiceInfo serviceInfo) {
		original.setServiceInfo(serviceInfo);
	}

}
