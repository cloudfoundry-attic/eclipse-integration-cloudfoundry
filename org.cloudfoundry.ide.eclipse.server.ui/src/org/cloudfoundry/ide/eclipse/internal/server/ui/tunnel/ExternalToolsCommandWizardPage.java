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
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ExternalToolsCommandWizardPage extends WizardPage {
	private final List<ExternalToolLaunchCommandsServer> originalServer;

	private ServiceTunnelCommandPart commandPart;

	protected ExternalToolsCommandWizardPage(List<ExternalToolLaunchCommandsServer> originalServer,
			CloudFoundryServer cloudServer) {
		super("External Tools Command Page");
		setTitle("External Tools Commands");
		setDescription("Add, delete, or edit commands to launch external tools for given services when connected to tunnels.");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.originalServer = originalServer;
	}

	public void createControl(Composite parent) {
		commandPart = new ServiceTunnelCommandPart(originalServer);
		Control control = commandPart.createControl(parent);
		setControl(control);

	}

	public List<ExternalToolLaunchCommandsServer> getExtToolLaunchCommandsServer() {
		return commandPart != null ? commandPart.getUpdatedServers() : originalServer;
	}

}
