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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOptions;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelOptions;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.UnsetOptionsWizard;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class ExternalToolUIOptionsHandler {

	private final ServiceCommand serviceCommand;

	private final CaldecottTunnelDescriptor descriptor;

	private final Shell shell;

	public ExternalToolUIOptionsHandler(Shell shell, ServiceCommand serviceCommand, CaldecottTunnelDescriptor descriptor) {
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
	 * Will resolve an option variables for tunnel options like username and
	 * password, and prompt the user for non-user variables. Returns a service
	 * command with resolved variables, or null if the user cancelled entering
	 * values for non-user variables. If not service command is returned, it
	 * indicates that the application command should not be executed.
	 * @return resolved service command, or null if variables are not resolved,
	 * most likely due to user canceling the prompt
	 */
	public ServiceCommand promptForValues() {
		CommandOptions options = serviceCommand.getOptions();
		if (options != null) {

			List<String> variables = ServiceCommand.getOptionVariables(serviceCommand, options.getOptions());
			boolean shouldPromptForNonTunnel = false;
			if (variables != null && !variables.isEmpty()) {

				Map<String, String> variableToValue = new HashMap<String, String>();
				for (String variable : variables) {
					String value = null;
					// First resolve tunnel variables
					if (variable != null) {
						TunnelOptions tunnelOption = getTunnelOption(variable);
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
								value = descriptor.tunnelPort() + "";
								break;
							}

						}
						else {
							// found a non-tunnel variable that needs further
							// input from user
							shouldPromptForNonTunnel = true;
						}
					}
					variableToValue.put(variable, value);
				}

				// Now prompt for the remaining values

				if (shouldPromptForNonTunnel) {
					variableToValue = promptForUnsetValues(variableToValue);
					// If user cancelled entering values, return a null service
					// command to indicate the caller
					// that the command should not be run
					if (variableToValue == null) {
						return null;
					}
				}

				// Finally set the resolved values back in the command
				ServiceCommand.setOptionVariableValues(serviceCommand, variableToValue);

			}

		}
		return serviceCommand;
	}

	/**
	 * 
	 * @param variableToValue
	 * @return resolved values or null if user cancelled entered values for the
	 * variables
	 */
	protected Map<String, String> promptForUnsetValues(Map<String, String> variableToValue) {
		Map<String, String> resolvedVariables = variableToValue;
		if (variableToValue != null && !variableToValue.isEmpty()) {

			UnsetOptionsWizard wizard = new UnsetOptionsWizard(variableToValue);
			WizardDialog dialog = new WizardDialog(shell, wizard);
			if (dialog.open() == Window.OK) {
				resolvedVariables = wizard.getVariables();
			}
			else {
				// user cancelled therefore return null;
				return null;
			}
		}

		return resolvedVariables;

	}

}
