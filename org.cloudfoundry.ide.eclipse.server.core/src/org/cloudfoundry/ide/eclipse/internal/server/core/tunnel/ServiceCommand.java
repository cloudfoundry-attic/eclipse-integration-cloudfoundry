/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON
 * serialisation
 * 
 */
public class ServiceCommand {

	private CommandTerminal commandTerminal;

	private CommandOptions options;

	private ExternalApplicationLaunchInfo externalApplicationLaunchInfo;

	public ServiceCommand() {
		// Set a default terminal for the command
		commandTerminal = CommandTerminal.getDefaultOSTerminal();
	}

	public boolean usesTerminal() {
		return getCommandTerminal() != null;
	}

	public void setCommandTerminal(CommandTerminal terminalCommand) {
		this.commandTerminal = terminalCommand;
	}

	public CommandTerminal getCommandTerminal() {
		return commandTerminal;
	}

	public ExternalApplicationLaunchInfo getExternalApplicationLaunchInfo() {
		return externalApplicationLaunchInfo;
	}

	public CommandOptions getOptions() {
		return options;
	}

	public void setExternalApplicationLaunchInfo(ExternalApplicationLaunchInfo appInfo) {
		this.externalApplicationLaunchInfo = appInfo;
	}

	public void setOptions(CommandOptions options) {
		this.options = options;
	}

	public static List<String> getOptionVariables(ServiceCommand serviceCommand, String options) {
		if (options == null || serviceCommand == null) {
			return Collections.emptyList();
		}

		// Trim trailing and leading white spaces.
		options = options.trim();

		List<String> variableList = new ArrayList<String>();

		// Parse all option values that start with '$'
		StringWriter variableBuffer = null;

		for (int i = 0; i < options.length(); i++) {
			if (options.charAt(i) == '$') {
				// Start parsing the variable
				variableBuffer = new StringWriter();

			}
			// Flush the variable if a white space or end of string is
			// encountered
			else if ((Character.isSpaceChar(options.charAt(i)) || i == options.length() - 1) && variableBuffer != null) {
				if (!Character.isSpaceChar(options.charAt(i))) {
					// append the last character if it is not a whitespace character
					variableBuffer.append(options.charAt(i));
				}
				// Only add variables that have content
				if (variableBuffer.getBuffer().length() > 0) {
					variableList.add(variableBuffer.toString());
				}
				// Prepare for the next variable
				variableBuffer = null;
			}
			else if (variableBuffer != null) {
				variableBuffer.append(options.charAt(i));
			}

		}

		return variableList;

	}

	public static void setOptionVariableValues(ServiceCommand serviceCommand, Map<String, String> variableToValueMap) {

		CommandOptions commandOptions = serviceCommand.getOptions();

		if (commandOptions == null || commandOptions.getOptions() == null) {
			return;
		}

		String options = commandOptions.getOptions();

		// Parse all option values that start with '$'
		StringBuffer variableBuffer = null;

		StringBuffer resolvedOptions = new StringBuffer(options);

		int dollarSignIndex = -1;

		// Note that the resolvedOptions buffer MAY grow therefore length will
		// vary during each iteration
		for (int i = 0; i < resolvedOptions.length(); i++) {

			if (resolvedOptions.charAt(i) == '$') {
				// Start parsing the variable
				dollarSignIndex = i;
				variableBuffer = new StringBuffer();
			}
			// Flush the variable if a white space or end of string is
			// encountered
			else if ((Character.isSpaceChar(resolvedOptions.charAt(i)) || i == resolvedOptions.length() - 1)
					&& variableBuffer != null) {
				if (!Character.isSpaceChar(resolvedOptions.charAt(i))) {
					// append the last character if it is not a whitespace character
					variableBuffer.append(resolvedOptions.charAt(i));
				}

				// Look up the value
				if (variableBuffer.length() > 0) {
					String variable = variableBuffer.toString();
					String value = variableToValueMap.get(variable);
					int endingIndex = dollarSignIndex + variable.length();
					if (value != null && dollarSignIndex >= 0 && (endingIndex < resolvedOptions.length())) {

						// delete the variable
						resolvedOptions.replace(dollarSignIndex, endingIndex, "");

						// append the value
						resolvedOptions.insert(dollarSignIndex, value);
					}
				}
				// Prepare for the next variable
				variableBuffer = null;
				dollarSignIndex = -1;
			}
			else if (variableBuffer != null) {
				variableBuffer.append(resolvedOptions.charAt(i));
			}

		}
		CommandOptions resolvedOp = new CommandOptions();
		resolvedOp.setOptions(resolvedOptions.toString());
		serviceCommand.setOptions(resolvedOp);
	}

}
