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

import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.jface.wizard.Wizard;

public class CaldecottTunnelWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CaldecottTunnelWizardPage page;

	public CaldecottTunnelWizard(CloudFoundryServer cloudServer) {
		super();
		this.cloudServer = cloudServer;

		setWindowTitle("Active Tunnels");
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
