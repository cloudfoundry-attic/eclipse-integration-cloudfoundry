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
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.ServiceTunnelCommandPart;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ExternalToolsCommandWizardPage extends WizardPage {
	private final TunnelServiceCommands originalCommands;

	private ServiceTunnelCommandPart commandPart;

	protected ExternalToolsCommandWizardPage(TunnelServiceCommands originalCommands, CloudFoundryServer cloudServer) {
		super("External Tools Command Page");
		setTitle("External Tools Commands");
		setDescription("Add, delete, or edit commands to launch external tools for given services when connected to tunnels.");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.originalCommands = originalCommands;
	}

	public void createControl(Composite parent) {
		commandPart = new ServiceTunnelCommandPart(originalCommands);
		Control control = commandPart.createControl(parent);
		setControl(control);

	}

	public TunnelServiceCommands getExtToolLaunchCommandsServer() {
		return commandPart != null ? commandPart.getUpdatedCommands() : originalCommands;
	}

}
