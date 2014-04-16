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

import org.cloudfoundry.ide.eclipse.internal.server.core.application.EnvironmentVariable;

/**
 * Creates commands and services for an existing service command container. It
 * also configures the added commands based on properties defined in the command
 * container, like setting a default command terminal
 * 
 */
public class ServiceCommandManager {

	private final ITunnelServiceCommands commands;

	public ServiceCommandManager(ITunnelServiceCommands commands) {
		this.commands = commands;
	}

	protected ServerService getService(ServiceInfo serviceInfo) {
		List<ServerService> services = commands.getServices();
		ServerService resolvedService = null;
		if (services != null) {
			for (ServerService src : services) {
				if (src.getServiceInfo().equals(serviceInfo)) {
					resolvedService = src;
					break;
				}
			}
		}
		return resolvedService;
	}

	/**
	 * Creates command configured for the given list of commands.
	 * @param serviceInfo required.
	 * @param displayName optional.
	 * @return
	 */
	public ServiceCommand createCommand(String displayName) {

		ServiceCommand command = new ServiceCommand();
		command.setDisplayName(displayName);
		CommandTerminal defaultTerminal = commands.getDefaultTerminal();
		if (defaultTerminal != null) {
			command.setCommandTerminal(defaultTerminal);
		}

		return command;
	}

	public List<ServerService> addServices(ServiceInfo[] serviceInfos) {
		List<ServerService> services = new ArrayList<ServerService>();
		// Fill in all the default services if creating a clean list of
		// services
		for (ServiceInfo service : serviceInfos) {
			ServerService serverService = new ServerService();
			serverService.setServiceInfo(service);
			services.add(serverService);
		}
		commands.setServices(services);
		return services;
	}

	/**
	 * Adds the given command to the given service. If the service is not found,
	 * the command is null, returns false. If successfully added, returns true
	 * @param service
	 * @param command
	 * @return true if successfully added. False otherwise
	 */
	public boolean addCommand(ServiceInfo service, ServiceCommand command) {
		if (command == null || service == null) {
			return false;
		}

		ServerService serverService = getService(service);

		if (serverService == null) {
			return false;
		}

		List<ServiceCommand> commands = serverService.getCommands();
		if (commands == null) {
			commands = new ArrayList<ServiceCommand>();
			serverService.setCommands(commands);
		}

		commands.add(command);
		return true;
	}

	/**
	 * Adds the given predefined commands as actual defined commands. Note that
	 * existing commands with the same predefined command display name may be
	 * replaced. It's up to the caller
	 * @param predefinedCommands
	 */
	public void addPredefinedCommands(PredefinedServiceCommands predefinedCommands) {
		if (predefinedCommands != null) {
			List<ServerService> services = commands.getServices();
			if (services != null) {
				for (ServerService service : services) {
					List<ServiceCommand> predefs = predefinedCommands.getPredefinedCommands(service.getServiceInfo());

					if (predefs != null) {

						for (ServiceCommand predefCommand : predefs) {

							ServiceCommand newCommand = createCommand(predefCommand.getDisplayName());

							if (newCommand != null) {

								CommandTerminal predefTerminal = predefCommand.getCommandTerminal();
								if (predefTerminal != null) {
									newCommand.setCommandTerminal(predefTerminal);
								}

								ExternalApplication predefExternalApp = predefCommand.getExternalApplication();
								if (predefExternalApp != null) {
									newCommand.setExternalApplication(predefExternalApp);
								}

								CommandOptions options = predefCommand.getOptions();
								if (options != null) {
									newCommand.setOptions(options);
								}

								List<EnvironmentVariable> envVars = predefCommand.getEnvironmentVariables();
								if (envVars != null) {
									newCommand.setEnvironmentVariables(envVars);
								}

								addCommand(service.getServiceInfo(), newCommand);
							}

						}
					}
				}
			}
		}
	}

}
