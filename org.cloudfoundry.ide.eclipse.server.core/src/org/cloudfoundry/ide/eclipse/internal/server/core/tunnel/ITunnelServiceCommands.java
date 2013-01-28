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

import java.util.List;

public interface ITunnelServiceCommands {
	/**
	 * Will never be null.
	 * @return non-null list of services
	 */
	public List<ServerService> getServices();

	public void setServices(List<ServerService> services);

	public CommandTerminal getDefaultTerminal();

	public void setDefaultTerminal(CommandTerminal defaultTerminal);

}
