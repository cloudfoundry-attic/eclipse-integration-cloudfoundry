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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.eclipse.jface.wizard.Wizard;

public class CaldecottTunnelWizard extends Wizard {

	private final CloudFoundryServerBehaviour behaviour;

	private CaldecottTunnelWizardPage page;

	public CaldecottTunnelWizard(CloudFoundryServerBehaviour behaviour) {
		super();
		this.behaviour = behaviour;

		setWindowTitle("Active Caldecott Tunnels");
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new CaldecottTunnelWizardPage(behaviour);
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
