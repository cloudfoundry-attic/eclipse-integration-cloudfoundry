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

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.eclipse.jface.wizard.Wizard;

public class ServiceCommandWizard extends Wizard {

	private final ServiceCommand initialServiceCommand;

	private ServiceCommandWizardPage page;

	public ServiceCommandWizard(ServiceCommand serviceCommand) {
		super();

		this.initialServiceCommand = serviceCommand;

		setWindowTitle("Configure a command to run:");
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new ServiceCommandWizardPage(initialServiceCommand);
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
