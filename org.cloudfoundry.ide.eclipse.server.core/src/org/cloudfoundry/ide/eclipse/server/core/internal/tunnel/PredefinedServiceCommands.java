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
package org.cloudfoundry.ide.eclipse.server.core.internal.tunnel;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;

public class PredefinedServiceCommands {

	public List<ServiceCommand> getPredefinedCommands(ServiceInfo serviceVendor) {
		List<ServiceCommand> templates = new ArrayList<ServiceCommand>();
		if (serviceVendor != null) {
			String[][] value = null;
			switch (serviceVendor) {
			case mysql:
				value = new String[][] {
						{
								"mysql", //$NON-NLS-1$
								"mysql", //$NON-NLS-1$
								"--protocol=TCP --host=localhost --port=${" + TunnelOptions.port.name() + "} --user=${" //$NON-NLS-1$ //$NON-NLS-2$
										+ TunnelOptions.user.name() + "} --password=${" + TunnelOptions.password.name() //$NON-NLS-1$
										+ "} ${" + TunnelOptions.databasename.name() + "}" }, //$NON-NLS-1$ //$NON-NLS-2$
						{
								"mysqldump", //$NON-NLS-1$
								"mysqldump", //$NON-NLS-1$
								"--protocol=TCP --host=localhost --port=${" + TunnelOptions.port.name() + "} --user=${" //$NON-NLS-1$ //$NON-NLS-2$
										+ TunnelOptions.user.name() + "} --password=${" + TunnelOptions.password.name() //$NON-NLS-1$
										+ "} ${" + TunnelOptions.databasename.name() + "} > ${Output file}" //$NON-NLS-1$ //$NON-NLS-2$

						} };

				break;
			case redis:
				value = new String[][] { {
						"redis-cli", //$NON-NLS-1$
						"redis-cli", //$NON-NLS-1$
						"-h localhost -p ${" + TunnelOptions.port.name() + "} -a ${" + TunnelOptions.password.name() //$NON-NLS-1$ //$NON-NLS-2$
								+ "}" } //$NON-NLS-1$

				};
				break;

			case mongodb:
				value = new String[][] {
						{
								"mongo", //$NON-NLS-1$
								"mongo", //$NON-NLS-1$
								"--host localhost --port ${" + TunnelOptions.port.name() + "} -u ${" //$NON-NLS-1$ //$NON-NLS-2$
										+ TunnelOptions.user.name() + "} -p ${" + TunnelOptions.password.name() //$NON-NLS-1$
										+ "} ${" + TunnelOptions.databasename.name() + "}" }, //$NON-NLS-1$ //$NON-NLS-2$
						{
								"mongodump", //$NON-NLS-1$
								"mongodump", //$NON-NLS-1$
								"--host localhost --port ${" + TunnelOptions.port.name() + "} -u ${" //$NON-NLS-1$ //$NON-NLS-2$
										+ TunnelOptions.user.name() + "} -p ${" + TunnelOptions.password.name() //$NON-NLS-1$
										+ "} ${" + TunnelOptions.databasename.name() + "}" }, //$NON-NLS-1$ //$NON-NLS-2$
						{
								"mongorestore", //$NON-NLS-1$
								"mongorestore", //$NON-NLS-1$
								"--host localhost --port ${" + TunnelOptions.port.name() + "} -u ${" //$NON-NLS-1$ //$NON-NLS-2$
										+ TunnelOptions.user.name() + "} -p ${" + TunnelOptions.password.name() //$NON-NLS-1$
										+ "} ${" + TunnelOptions.databasename.name() //$NON-NLS-1$
										+ "} ${Directory or filename}" }, //$NON-NLS-1$

				};
				break;
			case postgresql:
				value = new String[][] { {
						"psql", //$NON-NLS-1$
						"psql", //$NON-NLS-1$
						"-h localhost -p ${" + TunnelOptions.port.name() + "} -d ${" //$NON-NLS-1$ //$NON-NLS-2$
								+ TunnelOptions.databasename.name() + "} -U ${" + TunnelOptions.user.name() + "} -w" } //$NON-NLS-1$ //$NON-NLS-2$

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
		if ("psql".equals(applicationName)) { //$NON-NLS-1$
			EnvironmentVariable envVariable = new EnvironmentVariable();
			envVariable.setVariable("PGPASSWORD"); //$NON-NLS-1$
			envVariable.setValue("${" + TunnelOptions.password.name() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
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
