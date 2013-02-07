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

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.ServiceTunnelCommandPart;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class TunnelCommandDefinitionWizardPage extends CloudFoundryAwareWizardPage {
	private final ITunnelServiceCommands originalCommands;

	private ServiceTunnelCommandPart commandPart;

	protected TunnelCommandDefinitionWizardPage(ITunnelServiceCommands originalCommands, ImageDescriptor banner) {
		super("Service Tunnel Commands Page", "Service Tunnel Commands",
				"Add, delete, or edit commands to launch external applications for service tunnels", banner);
		this.originalCommands = originalCommands;
	}

	public void createControl(Composite parent) {
		TunnelCommandDefinitionWizard wizard = (TunnelCommandDefinitionWizard) getWizard();
		commandPart = new ServiceTunnelCommandPart(originalCommands, wizard.getServiceContext());
		Control control = commandPart.createPart(parent);
		setControl(control);

	}

	public ITunnelServiceCommands getExtToolLaunchCommandsServer() {
		return commandPart != null ? commandPart.getUpdatedCommands() : originalCommands;
	}

}
