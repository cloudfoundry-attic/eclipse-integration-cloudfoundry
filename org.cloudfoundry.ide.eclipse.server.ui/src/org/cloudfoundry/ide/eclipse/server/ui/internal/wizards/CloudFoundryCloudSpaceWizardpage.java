/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudServerSpacesDelegate;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudSpacesDelegate;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudSpacesSelectionPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CloudFoundryCloudSpaceWizardpage extends WizardPage {

	protected CloudSpacesDelegate cloudServerSpaceDelegate;

	protected final CloudFoundryServer cloudServer;

	protected CloudSpacesSelectionPart spacesPart;

	public CloudFoundryCloudSpaceWizardpage(CloudFoundryServer cloudServer, CloudServerSpacesDelegate cloudServerSpaceDelegate) {
		super(cloudServer.getServer().getName() + Messages.CloudFoundryCloudSpaceWizardpage_TEXT_ORG_AND_SPACES);
		this.cloudServer = cloudServer;
		this.cloudServerSpaceDelegate = cloudServerSpaceDelegate;
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {

		spacesPart = new CloudSpacesSelectionPart(cloudServerSpaceDelegate, cloudServer, this);
		spacesPart.addPartChangeListener(new WizardPageStatusHandler(this));
		Control composite = spacesPart.createPart(parent);
		setControl(composite);
	}

	public boolean isPageComplete() {
		return cloudServerSpaceDelegate != null && cloudServerSpaceDelegate.hasSpace();
	}

	public void refreshListOfSpaces() {
		if (spacesPart != null) {
			spacesPart.setInput();
		}
	}

}
