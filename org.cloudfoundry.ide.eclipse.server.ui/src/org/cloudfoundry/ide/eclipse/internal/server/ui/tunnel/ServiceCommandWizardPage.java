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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOptions;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class ServiceCommandWizardPage extends WizardPage {

	private ServiceCommand serviceCommand;

	private CommandDisplayPart displayPart;

	protected ServiceCommandWizardPage(CloudFoundryServer cloudServer, ServiceCommand serviceCommand) {
		super("Command Page");
		setTitle("Command Definition");
		setDescription("Define a command to launch on a service tunnel");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.serviceCommand = serviceCommand;
	}

	public void createControl(Composite parent) {
		displayPart = new CommandDisplayPart(serviceCommand);
		displayPart.createPart(parent);

	}

	public ServiceCommand getServiceCommand() {
		if (displayPart != null) {
			String location = displayPart.getLocation();
			String options = displayPart.getOptions();
			String displayName = displayPart.getDisplayName();
			if (serviceCommand != null) {
				serviceCommand = serviceCommand.getServiceCommand(location, displayName, options);
			}
			else {
				serviceCommand = new ServiceCommand(new ServiceCommand.ExternalApplicationLaunchInfo(displayName,
						location), null, new CommandOptions(options));
			}
		}
		return serviceCommand;
	}

}
