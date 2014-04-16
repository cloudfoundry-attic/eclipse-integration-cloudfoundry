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
 * Contains command definitions with additional information that should not be
 * serialised, like predefined commands per service.
 * 
 */
public class CommandDefinitionsWithPredefinition implements ITunnelServiceCommands {

	private final TunnelServiceCommands original;

	private final PredefinedServiceCommands predefined;

	private List<ServerService> wrapped;

	public CommandDefinitionsWithPredefinition(TunnelServiceCommands commands, PredefinedServiceCommands predefined) {
		this.original = commands;
		this.predefined = predefined;

	}

	public TunnelServiceCommands getSerialisableCommands() {
		List<ServerService> serverServices = getServices();
		List<ServerService> serialisable = new ArrayList<ServerService>();

		for (ServerService service : serverServices) {
			if (service instanceof ServerServiceWithPredefinitions) {
				serialisable.add(((ServerServiceWithPredefinitions) service).getOriginal());
			}
			else {
				serialisable.add(service);
			}
		}
		original.setServices(serialisable);
		return original;
	}

	public List<ServerService> getServices() {
		// Wrap the services with services that have predefined definitions

		if (wrapped == null) {
			List<ServerService> existingServices = original.getServices();

			wrapped = new ArrayList<ServerService>();
			for (ServerService existing : existingServices) {
				wrapped.add(new ServerServiceWithPredefinitions(existing, predefined));
			}
		}

		return wrapped;
	}

	public void setServices(List<ServerService> services) {
		original.setServices(services);
	}

	public CommandTerminal getDefaultTerminal() {
		return original.getDefaultTerminal();
	}

	public void setDefaultTerminal(CommandTerminal defaultTerminal) {
		original.setDefaultTerminal(defaultTerminal);
	}

}
