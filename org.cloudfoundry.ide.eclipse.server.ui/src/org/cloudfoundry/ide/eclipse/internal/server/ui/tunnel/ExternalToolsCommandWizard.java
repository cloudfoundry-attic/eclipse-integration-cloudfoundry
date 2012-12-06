/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ExternalToolLaunchCommandsServer;
import org.eclipse.jface.wizard.Wizard;

public class ExternalToolsCommandWizard extends Wizard {

	private CloudFoundryServer cloudServer;

	private List<ExternalToolLaunchCommandsServer> originalServers;

	private ExternalToolsCommandWizardPage page;

	public ExternalToolsCommandWizard(List<ExternalToolLaunchCommandsServer> originalServers,
			CloudFoundryServer cloudServer) {
		super();
		this.cloudServer = cloudServer;
		this.originalServers = originalServers;

		setWindowTitle("External Tools Commands");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new ExternalToolsCommandWizardPage(originalServers, cloudServer);
		addPage(page);
	}

	public List<ExternalToolLaunchCommandsServer> getExternalToolLaunchCommandsServer() {
		return page != null ? page.getExtToolLaunchCommandsServer() : originalServers;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
