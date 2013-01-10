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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelServiceCommands;
import org.eclipse.jface.wizard.Wizard;

public class ExternalToolsCommandWizard extends Wizard {

	private CloudFoundryServer cloudServer;

	private TunnelServiceCommands originalCommands;

	private ExternalToolsCommandWizardPage page;

	public ExternalToolsCommandWizard(TunnelServiceCommands originalCommands, CloudFoundryServer cloudServer) {
		super();
		this.cloudServer = cloudServer;
		this.originalCommands = originalCommands;

		setWindowTitle("External Tools Commands");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new ExternalToolsCommandWizardPage(originalCommands, cloudServer);
		addPage(page);
	}

	public TunnelServiceCommands getExternalToolLaunchCommandsServer() {
		return page != null ? page.getExtToolLaunchCommandsServer() : originalCommands;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
