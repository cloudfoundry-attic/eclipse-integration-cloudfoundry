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

public class PredefinedServiceCommands {

	public List<ServiceCommand> getPredefinedCommands(ServiceInfo serviceVendor) {
		List<ServiceCommand> templates = new ArrayList<ServiceCommand>();
		if (serviceVendor != null) {
			String[][] value = null;
			switch (serviceVendor) {
			case mysql:
				value = new String[][] {
						{
								"mysql",
								"mysql",
								"--protocol=TCP --host=localhost --port=${" + TunnelOptions.port.name() + "} --user=${"
										+ TunnelOptions.user.name() + "} --password=${" + TunnelOptions.password.name()
										+ "} ${" + TunnelOptions.databasename.name() + "}" },
						{
								"mysqldump",
								"mysqldump",
								"--protocol=TCP --host=localhost --port=${" + TunnelOptions.port.name() + "} --user=${"
										+ TunnelOptions.user.name() + "} --password=${" + TunnelOptions.password.name()
										+ "} ${" + TunnelOptions.databasename.name() + "} > ${Output file}"

						} };

				break;
			case redis:
				value = new String[][] { {
						"redis-cli",
						"redis-cli",
						"-h localhost -p ${" + TunnelOptions.port.name() + "} -a ${" + TunnelOptions.password.name()
								+ "}" }

				};
				break;

			case mongodb:
				value = new String[][] {
						{
								"mongo",
								"mongo",
								"--host localhost --port ${" + TunnelOptions.port.name() + "} -u ${"
										+ TunnelOptions.user.name() + "} -p ${" + TunnelOptions.password.name()
										+ "} ${" + TunnelOptions.databasename.name() + "}" },
						{
								"mongodump",
								"mongodump",
								"--host localhost --port ${" + TunnelOptions.port.name() + "} -u ${"
										+ TunnelOptions.user.name() + "} -p ${" + TunnelOptions.password.name()
										+ "} ${" + TunnelOptions.databasename.name() + "}" },
						{
								"mongorestore",
								"mongorestore",
								"--host localhost --port ${" + TunnelOptions.port.name() + "} -u ${"
										+ TunnelOptions.user.name() + "} -p ${" + TunnelOptions.password.name()
										+ "} ${" + TunnelOptions.databasename.name()
										+ "} ${Directory or filename}" },

				};
				break;
			case postgresql:
				value = new String[][] { {
						"psql",
						"psql",
						"-h localhost -p ${" + TunnelOptions.port.name() + "} -d ${"
								+ TunnelOptions.databasename.name() + "} -U ${" + TunnelOptions.user.name() + "} -w" }

				};
				break;

			}

			if (value != null) {
				List<ServiceCommand> tmpls = getCommands(value);

				if (tmpls != null) {
					templates.addAll(tmpls);
				}
			}

		}
		return templates;

	}

	protected List<EnvironmentVariable> getEnvironmentVariables(String applicationName) {
		if ("psql".equals(applicationName)) {
			EnvironmentVariable envVariable = new EnvironmentVariable();
			envVariable.setVariable("PGPASSWORD");
			envVariable.setValue("${" + TunnelOptions.password.name() + "}");
			List<EnvironmentVariable> variables = new ArrayList<EnvironmentVariable>();
			variables.add(envVariable);

			return variables;
		}
		return null;
	}

	protected List<ServiceCommand> getCommands(String[][] nameAndOptions) {

		List<ServiceCommand> commands = new ArrayList<ServiceCommand>();

		if (nameAndOptions != null) {
			for (String[] nameAndOption : nameAndOptions) {

				if (nameAndOption != null && nameAndOption.length == 3) {
					ServiceCommand command = new ServiceCommand();
					ExternalApplication app = new ExternalApplication();

					command.setDisplayName(nameAndOption[0]);
					app.setExecutableNameAndPath(nameAndOption[1]);
					command.setExternalApplication(app);

					CommandOptions options = new CommandOptions();
					options.setOptions(nameAndOption[2]);
					command.setOptions(options);
					commands.add(command);

					// Get any environment variables
					List<EnvironmentVariable> envVars = getEnvironmentVariables(command.getDisplayName());
					if (envVars != null) {
						command.setEnvironmentVariables(envVars);
					}
				}
			}
		}

		return commands;
	}
}
