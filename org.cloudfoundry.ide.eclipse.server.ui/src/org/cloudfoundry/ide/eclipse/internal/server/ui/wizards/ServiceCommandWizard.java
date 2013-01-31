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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOptions;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandTerminal;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ExternalApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommandManager;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.AddCommandDisplayPart;
import org.eclipse.jface.wizard.Wizard;

/**
 * Edits or adds a new service command. Upon wizard finish, the edited command
 * will be added to the given commands. If a user clicks Cancel or closes the
 * wizard, the edited command will not be added.
 * 
 */
public class ServiceCommandWizard extends Wizard {

	private final ServiceCommand contextCommand;

	private ServiceCommand editedCommand;

	private final ServerService service;

	private ServiceCommandWizardPage page;

	private final ITunnelServiceCommands commands;

	/**
	 * 
	 * @param service must not be null
	 * @param serviceCommandToEdit Existing values will be populated in the UI,
	 * and new values set
	 */
	public ServiceCommandWizard(ITunnelServiceCommands commands, ServerService service,
			ServiceCommand serviceCommandToEdit) {
		super();
		this.service = service;
		this.contextCommand = serviceCommandToEdit;
		this.commands = commands;

		setWindowTitle("Configure a command to run:");
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new ServiceCommandWizardPage();
		addPage(page);
	}

	/**
	 * 
	 * @return the edited command, or null if a user clicked Cancel or the
	 * edited command was not successfully added to the list existing commands
	 */
	public ServiceCommand getEditedServiceCommand() {
		return editedCommand;
	}

	public ServiceCommand getContextServiceCommand() {
		return contextCommand;
	}

	public ITunnelServiceCommands getCommands() {
		return commands;
	}

	public ServerService getService() {
		return service;
	}

	public boolean applyTerminalToAllCommands() {
		return page != null && page.getCommandPart() != null ? page.getCommandPart().applyTerminalToAllCommands()
				: false;
	}

	@Override
	public boolean performFinish() {
		AddCommandDisplayPart part = page != null ? page.getCommandPart() : null;

		if (part != null) {
			editedCommand = new ServiceCommandManager(commands).createCommand(part.getDisplayName());

			if (editedCommand != null) {
				// External application location should never be null
				ExternalApplication appInfo = new ExternalApplication();
				appInfo.setExecutableNameAndPath(part.getExecutableLocation());
				editedCommand.setExternalApplication(appInfo);

				// Options and terminal may be optional
				String optionsVal = part.getOptions();
				if (optionsVal != null) {
					optionsVal = optionsVal.trim();
					if (optionsVal.length() > 0) {
						CommandOptions options = new CommandOptions();
						options.setOptions(optionsVal);
						editedCommand.setOptions(options);
					}
				}

				String terminalLocationVal = part.getTerminal();
				if (terminalLocationVal != null) {
					CommandTerminal terminal = new CommandTerminal();
					terminal.setTerminal(terminalLocationVal);
					editedCommand.setCommandTerminal(terminal);
				}
			}

		}

		return true;
	}
}
