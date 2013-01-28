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

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.eclipse.jface.wizard.Wizard;

/**
 * Edits or adds a new service command. If no service command is passed to the
 * constructor, it will assume that a new command is being added. If an existing
 * command is being passed, it will assume the existing command is being edited.
 * 
 */
public class ServiceCommandWizard extends Wizard {

	private final ServiceCommand initialServiceCommand;

	private final ServerService service;

	private final boolean addNewCommand;

	private ServiceCommandWizardPage page;

	public ServiceCommandWizard(ServerService service, ServiceCommand serviceCommandToEdit) {
		super();
		this.service = service;
		this.initialServiceCommand = serviceCommandToEdit;

		// Only add a new command if an existing one was not passed.
		this.addNewCommand = serviceCommandToEdit == null;
		setWindowTitle("Configure a command to run:");
		setNeedsProgressMonitor(true);
	}

	public ServiceCommandWizard(ServerService service) {
		this(service, null);
	}

	public void addPages() {
		page = new ServiceCommandWizardPage(service, initialServiceCommand, addNewCommand);
		addPage(page);
	}

	public ServiceCommand getServiceCommand() {
		return page != null ? page.getServiceCommand() : initialServiceCommand;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
