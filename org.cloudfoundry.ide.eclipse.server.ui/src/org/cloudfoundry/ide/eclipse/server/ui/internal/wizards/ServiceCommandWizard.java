/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CommandOptions;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CommandTerminal;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ExternalApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceCommandManager;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel.AddCommandDisplayPart;
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

		setWindowTitle(Messages.ServiceCommandWizard_TITLE_CONFIG_CMD);
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
				}
				if (optionsVal != null && optionsVal.length() > 0) {
					CommandOptions options = new CommandOptions();
					options.setOptions(optionsVal);
					editedCommand.setOptions(options);
				}
				else {
					editedCommand.setOptions(null);
				}

				String terminalLocationVal = part.getTerminal();
				if (terminalLocationVal != null) {
					terminalLocationVal = terminalLocationVal.trim();
				}
				if (terminalLocationVal != null && terminalLocationVal.length() > 0) {
					CommandTerminal terminal = new CommandTerminal();
					terminal.setTerminal(terminalLocationVal);
					editedCommand.setCommandTerminal(terminal);
				}
				else {
					editedCommand.setCommandTerminal(null);
				}

				List<EnvironmentVariable> envVars = part.getEnvironmentVariables();
				if (envVars != null && !envVars.isEmpty()) {
					editedCommand.setEnvironmentVariables(envVars);
				}
				else {
					editedCommand.setEnvironmentVariables(null);
				}

			}

		}

		return true;
	}
}
