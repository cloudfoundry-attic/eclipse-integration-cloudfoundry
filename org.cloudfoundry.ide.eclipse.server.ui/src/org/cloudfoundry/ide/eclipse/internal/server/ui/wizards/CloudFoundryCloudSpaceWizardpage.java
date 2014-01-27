/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudServerSpaceDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CloudFoundryCloudSpaceWizardpage extends WizardPage {

	protected CloudServerSpaceDelegate cloudServerSpaceDelegate;

	protected final CloudFoundryServer cloudServer;

	protected CloudSpacesSelectionPart spacesPart;

	public CloudFoundryCloudSpaceWizardpage(CloudFoundryServer cloudServer, CloudServerSpaceDelegate cloudServerSpaceDelegate) {
		super(cloudServer.getServer().getName() + " Organization and Spaces");
		this.cloudServer = cloudServer;
		this.cloudServerSpaceDelegate = cloudServerSpaceDelegate;
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {

		spacesPart = new CloudSpacesSelectionPart(cloudServerSpaceDelegate, new WizardPageChangeListener(this), cloudServer, this);
		Control composite = spacesPart.createPart(parent);
		setControl(composite);
	}

	public boolean isPageComplete() {
		return cloudServerSpaceDelegate != null && cloudServerSpaceDelegate.hasSetSpace();
	}

	public void refreshListOfSpaces() {
		if (spacesPart != null) {
			spacesPart.setInput();
		}
	}

}
