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
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceInfo;
import org.eclipse.jface.wizard.Wizard;

public class ExternalToolsCommandWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private ITunnelServiceCommands originalCommands;

	private ExternalToolsCommandWizardPage page;

	private final ServiceInfo serviceContext;

	public ExternalToolsCommandWizard(ITunnelServiceCommands originalCommands, CloudFoundryServer cloudServer,
			ServiceInfo serviceContext) {
		super();
		this.cloudServer = cloudServer;
		this.originalCommands = originalCommands;
		this.serviceContext = serviceContext;
		setWindowTitle("External Tools Commands");
		setNeedsProgressMonitor(true);
	}
	
	public ServiceInfo getServiceContext() {
		return serviceContext;
	}

	@Override
	public void addPages() {
		page = new ExternalToolsCommandWizardPage(originalCommands, cloudServer);
		addPage(page);
	}

	public ITunnelServiceCommands getExternalToolLaunchCommandsServer() {
		return page != null ? page.getExtToolLaunchCommandsServer() : originalCommands;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
