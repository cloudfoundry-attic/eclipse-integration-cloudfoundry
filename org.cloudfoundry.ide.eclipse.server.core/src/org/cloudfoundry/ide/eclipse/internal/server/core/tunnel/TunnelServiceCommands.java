/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
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
