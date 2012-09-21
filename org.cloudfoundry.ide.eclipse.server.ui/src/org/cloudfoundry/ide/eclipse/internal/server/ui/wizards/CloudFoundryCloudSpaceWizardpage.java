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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpaceDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class CloudFoundryCloudSpaceWizardpage extends WizardPage {

	private CloudSpacesSelectionPart spacesPart;

	private CloudSpaceDescriptor spacesDescriptor;

	private final CloudFoundryServer cloudServer;

	protected CloudFoundryCloudSpaceWizardpage(CloudFoundryServer cloudServer, CloudSpaceDescriptor spacesDescriptor) {
		super(cloudServer.getServer().getName() + " Organization and Spaces");
		this.cloudServer = cloudServer;
		this.spacesDescriptor = spacesDescriptor;
	}

	public void createControl(Composite parent) {
		spacesPart = new CloudSpacesSelectionPart(spacesDescriptor, cloudServer, this);
		spacesPart.createComposite(parent);

	}

	public void setCloudSpacesDescriptor(CloudSpaceDescriptor spacesDescriptor) {
		this.spacesDescriptor = spacesDescriptor;
		if (spacesPart != null) {
			spacesPart.setInput(spacesDescriptor);
		}
	}

	public boolean isPageComplete() {
		return spacesPart != null && spacesPart.isComplete();
	}

}
