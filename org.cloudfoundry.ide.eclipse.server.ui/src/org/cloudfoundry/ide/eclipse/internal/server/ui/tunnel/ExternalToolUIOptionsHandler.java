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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOption;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.UnsetOptionsWizard;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class ExternalToolUIOptionsHandler {

	public enum TunnelOptions {
		Username, Password, Url, Databasename, Port
	}

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

	public ServiceCommand promptForValues() {
		List<CommandOption> options = serviceCommand.getOptions();
		if (options != null) {
			// first fill in any Caldecott options
			List<CommandOption> unsetOptions = new ArrayList<CommandOption>();
			for (CommandOption option : options) {

				String optionName = CommandOption.getVariableName(option);
				if (optionName != null) {
					TunnelOptions tunnelOption = getTunnelOption(optionName);
					if (tunnelOption != null) {

						switch (tunnelOption) {

						case Username:
							option.setValue(descriptor.getUserName());
							break;
						case Password:
							option.setValue(descriptor.getPassword());
							break;
						case Url:
							option.setValue(descriptor.getURL());
							break;
						case Databasename:
							option.setValue(descriptor.getDatabaseName());
							break;
						case Port:
							option.setValue(descriptor.tunnelPort() + "");
							break;
						}
					}
					else {
						unsetOptions.add(option);
					}
				}

			}

			promptForUnsetValues(unsetOptions);

		}
		return serviceCommand;
	}

	protected void promptForUnsetValues(List<CommandOption> unsetOptions) {

		if (unsetOptions != null && !unsetOptions.isEmpty()) {
			UnsetOptionsWizard wizard = new UnsetOptionsWizard(unsetOptions);
			WizardDialog dialog = new WizardDialog(shell, wizard);
			if (dialog.open() == Window.OK) {
				List<CommandOption> options = wizard.getCommandOptions();
				if (options != null) {
					serviceCommand.setOptions(options);
				}
			}
		}

	}

}
