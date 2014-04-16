/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.application.EnvironmentVariable;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON
 * serialisation
 * 
 */
public class ServiceCommand {

	private CommandTerminal commandTerminal;

	private CommandOptions options;

	private String displayName;

	private ExternalApplication externalApplication;

	private List<EnvironmentVariable> environmentVariables;

	public ServiceCommand() {

	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setCommandTerminal(CommandTerminal terminalCommand) {
		this.commandTerminal = terminalCommand;
	}

	public CommandTerminal getCommandTerminal() {
		return commandTerminal;
	}

	public ExternalApplication getExternalApplication() {
		return externalApplication;
	}

	public void setExternalApplication(ExternalApplication appInfo) {
		this.externalApplication = appInfo;
	}

	public void setOptions(CommandOptions options) {
		this.options = options;
	}

	public CommandOptions getOptions() {
		return options;
	}

	public void setEnvironmentVariables(List<EnvironmentVariable> environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public List<EnvironmentVariable> getEnvironmentVariables() {
		return environmentVariables;
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
			// If char sequence at i and i+1 start with ${ start parsing
			// variable
			if ((options.charAt(i) == '$') && i + 1 < options.length() && (options.charAt(i + 1) == '{')) {
				// Start parsing the variable
				variableBuffer = new StringWriter();

				// Advance by one more to skip the starting bracket: {
				i++;
			}
			// Flush the variable ending bracket is encountered
			else if ((options.charAt(i) == '}') && variableBuffer != null) {
				// If the last character is a ending curly brace then it can be
				// skipped when it comes to adding characters to the
				// variable name buffer
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
		for (int i = 0; i < resolvedOptions.length();) {
			boolean inserted = false;
			if ((resolvedOptions.charAt(i) == '$') && i + 1 < resolvedOptions.length()
					&& (resolvedOptions.charAt(i + 1) == '{')) {
				// Start parsing the variable
				dollarSignIndex = i;

				// Advance by one more to skip '{'
				i++;
				variableBuffer = new StringBuffer();
			}
			// Flush the variable if an ending bracket or end of string is
			// encountered
			else if ((resolvedOptions.charAt(i) == '}') && variableBuffer != null) {

				// Look up the value
				if (variableBuffer.length() > 0) {
					String variable = variableBuffer.toString();
					String value = variableToValueMap.get(variable);
					// ending index should be the next index after the last
					// position where the value needs to be inserted, so in this
					// case the ending '}'
					// So if the end of the line is reached, the index should be
					// equal to the length of the options buffer.
					int endingIndex = i + 1;
					if (value != null && dollarSignIndex >= 0 && (endingIndex <= resolvedOptions.length())) {

						// delete the variable
						resolvedOptions.replace(dollarSignIndex, endingIndex, "");

						// insert the value
						resolvedOptions.insert(dollarSignIndex, value);

						// move the index past the inserted value
						i = dollarSignIndex + value.length();
						inserted = true;
					}
				}
				// Prepare for the next variable
				variableBuffer = null;
				dollarSignIndex = -1;
			}
			else if (variableBuffer != null) {
				variableBuffer.append(resolvedOptions.charAt(i));
			}

			// Advance to the next character unless a variable was replaced
			if (!inserted) {
				i++;
			}

		}
		CommandOptions resolvedOp = new CommandOptions();
		resolvedOp.setOptions(resolvedOptions.toString());
		serviceCommand.setOptions(resolvedOp);
	}

}
