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

import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandTerminal;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ExternalApplicationLaunchInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.CommandDisplayPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ServiceCommandWizardPage extends WizardPage {

	private ServiceCommand serviceCommand;

	private CommandDisplayPart displayPart;

	private IStatus partStatus;

	protected ServiceCommandWizardPage(ServiceCommand serviceCommand) {
		super("Command Page");
		setTitle("Command Definition");
		setDescription("Define a command to launch on a service tunnel");
		this.serviceCommand = serviceCommand;
	}

	public void createControl(Composite parent) {
		displayPart = new CommandDisplayPart(serviceCommand);
		displayPart.addPartChangeListener(new IPartChangeListener() {

			public void handleChange(PartChangeEvent event) {
				if (event != null) {
					partStatus = event.getStatus();
					if (partStatus == null || partStatus.isOK()) {
						setErrorMessage(null);
						setPageComplete(true);
					}
					else {
						if (ValueValidationUtil.isEmpty(partStatus.getMessage())) {
							setErrorMessage(null);
							setPageComplete(false);
						}
						else {
							setErrorMessage(partStatus.getMessage());
						}
					}
				}
			}

		});
		Control control = displayPart.createPart(parent);
		setControl(control);
	}

	@Override
	public boolean isPageComplete() {
		return partStatus == null || partStatus.isOK();
	}

	public ServiceCommand getServiceCommand() {
		if (displayPart != null) {
			String location = displayPart.getLocation();
			String options = displayPart.getOptions();
			String displayName = displayPart.getDisplayName();
			String terminalLocation = displayPart.getTerminalLocation();

			ServiceCommand editedCommand = new ServiceCommand();

			if (terminalLocation != null) {
				CommandTerminal terminal = new CommandTerminal();
				terminal.setTerminalLaunchCommand(terminalLocation);
				editedCommand.setCommandTerminal(terminal);
			}
			ExternalApplicationLaunchInfo appInfo = new ExternalApplicationLaunchInfo();
			appInfo.setDisplayName(displayName);
			appInfo.setExecutableName(location);
			editedCommand.setExternalApplicationLaunchInfo(appInfo);

			ServiceCommand.covertToOptions(editedCommand, options);
			serviceCommand = editedCommand;
		}
		return serviceCommand;
	}

}
