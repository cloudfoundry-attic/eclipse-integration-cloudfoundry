/*******************************************************************************
 * Copyright (c) 2012 - 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceInfo;
import org.eclipse.jface.wizard.Wizard;

public class TunnelCommandDefinitionWizard extends Wizard {



	private ITunnelServiceCommands originalCommands;

	private TunnelCommandDefinitionWizardPage page;

	private final ServiceInfo serviceContext;

	public TunnelCommandDefinitionWizard(ITunnelServiceCommands originalCommands, ServiceInfo serviceContext) {
		super();
		this.originalCommands = originalCommands;
		this.serviceContext = serviceContext;
		setWindowTitle("Service Tunnel Commands");
		setNeedsProgressMonitor(true);
	}

	public ServiceInfo getServiceContext() {
		return serviceContext;
	}

	@Override
	public void addPages() {
		page = new TunnelCommandDefinitionWizardPage(originalCommands, null);
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
