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
import java.util.List;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON serialisation
 * 
 */
public class ServiceCommand {

	private CommandTerminal commandTerminal;

	private List<CommandOption> options;

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

	/**
	 * 
	 * @return a copy of the options, or null if no options are set
	 */
	public List<CommandOption> getOptions() {
		return options != null ? new ArrayList<CommandOption>(options) : null;
	}

	public void setExternalApplicationLaunchInfo(ExternalApplicationLaunchInfo appInfo) {
		this.externalApplicationLaunchInfo = appInfo;
	}

	public void setOptions(List<CommandOption> options) {
		this.options = options;
	}
	
	public static String getSerialisedOptions(ServiceCommand command) {
		List<CommandOption> options = command.getOptions();
		if (options != null) {
			StringWriter writer = new StringWriter();
			for (CommandOption option : options) {
				writer.append(option.getOption());
				writer.append(' ');
				writer.append(option.getValue());
			}
			return writer.toString();
		}

		return null;
	}
	
	public static void covertToOptions(ServiceCommand serviceCommand, String optionsValue) {
		if (optionsValue == null || serviceCommand == null) {
			return;
		}
		
		// Trim trailing and leading white spaces.
		optionsValue = optionsValue.trim();

		List<CommandOption> optionsList = new ArrayList<CommandOption>();
		
		// Parse all options using pattern: [option][space][value]
		StringWriter optionBuffer = null;
		StringWriter valueBuffer = null;

		CommandOption currentOption = null;

		for (int i = 0; i < optionsValue.length(); i++) {
			if (!Character.isSpaceChar(optionsValue.charAt(i))) {
				// First parse the option
				if (optionBuffer == null && valueBuffer == null) {
					optionBuffer = new StringWriter();
				}

				if (optionBuffer != null) {
					optionBuffer.write(optionsValue.charAt(i));
				}
				else if (valueBuffer != null) {
					valueBuffer.write(optionsValue.charAt(i));
				}

			}

			if (Character.isSpaceChar(optionsValue.charAt(i)) || i == optionsValue.length() - 1) {
				// If a space has been encountered, flush whichever buffer is
				// full

				if (optionBuffer != null && optionBuffer.getBuffer().length() > 0) {
					currentOption = new CommandOption();
					currentOption.setOption(optionBuffer.toString());
					optionBuffer = null;
					valueBuffer = new StringWriter();
				}
				if (valueBuffer != null && valueBuffer.getBuffer().length() > 0) {
					if (currentOption != null) {
						currentOption.setValue(valueBuffer.toString());
					}
					valueBuffer = null;
				}

				// Only add a complete, well formed option
				if (currentOption != null && currentOption.getOption() != null && currentOption.getValue() != null) {
					optionsList.add(currentOption);
					currentOption = null;
				}
			}

		}

		if (optionsList.size() > 0) {
			serviceCommand.setOptions(optionsList);
		}
	}

}
