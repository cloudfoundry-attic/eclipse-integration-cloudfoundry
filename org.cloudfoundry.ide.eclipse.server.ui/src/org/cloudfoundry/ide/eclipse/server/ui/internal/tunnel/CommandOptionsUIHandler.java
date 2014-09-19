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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CommandOptions;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.TunnelOptions;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.SetValueVariablesWizard;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Resolves both command option values as well as command environment variables.
 * For both command options and environment variables, if their values are
 * variables themselves using the pattern ${varname} (e.g. -db ${databasename}),
 * the value variables will first be checked if they are variables reserved for
 * tunnel values, like user name, database, and password, and will be
 * substituted automatically.
 * 
 * For value variables that are not tunnel values, the user will be prompted for
 * values.
 * 
 */
public class CommandOptionsUIHandler {

	private final ServiceCommand serviceCommand;

	private final CaldecottTunnelDescriptor descriptor;

	private final Shell shell;

	public CommandOptionsUIHandler(Shell shell, ServiceCommand serviceCommand, CaldecottTunnelDescriptor descriptor) {
		this.serviceCommand = serviceCommand;
		this.descriptor = descriptor;
		this.shell = shell;
	}

	protected TunnelOptions getTunnelOption(String optionName) {
		for (TunnelOptions option : TunnelOptions.values()) {
			if (option.name().equals(optionName)) {
				return option;
			}
		}
		return null;
	}

	/**
	 * Will resolve an option variables for tunnel options like user name and
	 * password, and prompt the user for non-user variables. Returns a service
	 * command with resolved variables, or null if the user cancelled entering
	 * values for non-user variables. If not service command is returned, it
	 * indicates that the application command should not be executed.
	 * @return resolved service command, or null if variables are not resolved,
	 * most likely due to user canceling the prompt
	 */
	public ServiceCommand promptForValues() {
		Map<String, String> resolvedOptionVars = new HashMap<String, String>();
		Map<String, String> resolvedEnvVariables = new HashMap<String, String>();

		boolean shouldPromptOptions = resolveTunnelOptions(resolvedOptionVars);

		boolean shouldPromptEnvVariables = resolveEnvironmentVariables(resolvedEnvVariables);

		// Now prompt for the remaining values

		if (shouldPromptOptions || shouldPromptEnvVariables) {
			boolean executeCommand = promptForUnsetValues(resolvedOptionVars, resolvedEnvVariables);
			// If user cancelled entering values, return a null service
			// command to indicate the caller
			// that the command should not be run
			if (!executeCommand) {
				return null;
			}
		}

		// Finally set the resolved values back in the command
		ServiceCommand.setOptionVariableValues(serviceCommand, resolvedOptionVars);

		// Set environment variables
		List<EnvironmentVariable> variables = serviceCommand.getEnvironmentVariables();
		if (variables != null) {
			for (EnvironmentVariable var : variables) {
				String value = resolvedEnvVariables.get(var.getVariable());
				if (value != null) {
					var.setValue(value);
				}
			}
		}
		return serviceCommand;
	}

	protected boolean resolveTunnelOptions(Map<String, String> variablesToValues) {
		boolean shouldPromptForNonTunnel = false;
		CommandOptions options = serviceCommand.getOptions();
		if (options != null) {

			List<String> variables = ServiceCommand.getOptionVariables(serviceCommand, options.getOptions());

			if (variables != null) {

				for (String variable : variables) {
					String value = resolveTunnelVariable(variable);

					if (value == null) {
						// found a non-tunnel variable that needs further
						// input from user
						shouldPromptForNonTunnel = true;
					}
					variablesToValues.put(variable, value);
				}
			}
		}

		return shouldPromptForNonTunnel;
	}

	protected String resolveTunnelVariable(String variable) {
		if (variable == null) {
			return null;
		}
		TunnelOptions tunnelOption = getTunnelOption(variable);
		String value = null;
		if (tunnelOption != null) {

			switch (tunnelOption) {

			case user:
				value = descriptor.getUserName();
				break;
			case password:
				value = descriptor.getPassword();
				break;
			case url:
				value = descriptor.getURL();
				break;
			case databasename:
				value = descriptor.getDatabaseName();
				break;
			case port:
				value = descriptor.tunnelPort() + ""; //$NON-NLS-1$
				break;
			}

		}
		return value;
	}

	protected boolean resolveEnvironmentVariables(Map<String, String> envVariables) {
		List<EnvironmentVariable> vars = serviceCommand.getEnvironmentVariables();
		boolean shouldPrompt = false;
		if (vars != null) {
			for (EnvironmentVariable var : vars) {
				// Get the name value variable if the value is specified by a
				// ${varnam}
				String valueVar = EnvironmentVariable.getValueVariable(var);

				// Get the value again, in case it is not a value variable
				String value = var.getValue();

				// Make sure the value is a variable
				if (valueVar != null) {
					value = resolveTunnelVariable(valueVar);

					// If null, it means the value is a variable, but not a
					// tunnel variable, therefore prompt the user
					if (value == null) {
						shouldPrompt = true;
					}
				}
				envVariables.put(var.getVariable(), value);
			}
		}

		return shouldPrompt;
	}

	/**
	 * Prompts user for missing environment or options Variables. The map
	 * arguments should be modifiable as they are modified by the UI
	 * @return true if the user entered missing values. False if user cancelled or there were no values to enter
	 */
	protected boolean promptForUnsetValues(Map<String, String> optionsVariables, Map<String, String> envVariables) {
		if ((optionsVariables != null && !optionsVariables.isEmpty())
				|| (envVariables != null && !envVariables.isEmpty())) {

			SetValueVariablesWizard wizard = new SetValueVariablesWizard(optionsVariables, envVariables);
			WizardDialog dialog = new WizardDialog(shell, wizard);
			if (dialog.open() == Window.OK) {
				return true;
			}
			else {
				// user cancelled therefore return null;
				return false;
			}
		}

		return false;

	}

}
