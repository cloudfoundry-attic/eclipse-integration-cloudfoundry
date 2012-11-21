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

import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.eclipse.jface.wizard.Wizard;

public class ServiceCommandWizard extends Wizard {
	private final CloudFoundryServer cloudServer;
	
	private ServiceCommand serviceCommand;

	private CaldecottTunnelWizardPage page;

	public ServiceCommandWizard(CloudFoundryServer cloudServer, ServiceCommand serviceCommand) {
		super();
		this.cloudServer = cloudServer;
		this.serviceCommand = serviceCommand;

		setWindowTitle("Configure a command to run:");
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new CaldecottTunnelWizardPage(cloudServer);
		addPage(page);
	}
	
	public Set<CaldecottTunnelDescriptor> getDescriptorsToRemove() {
		return page != null ? page.getDescriptorsToRemove() : null;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
