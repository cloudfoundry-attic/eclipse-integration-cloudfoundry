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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class CaldecottTunnelInfoDialog extends TitleAreaDialog {

	private final CloudFoundryServer cloudServer;

	private final List<String> servicesWithTunnels;

	private TunnelDisplayPart part;

	public CaldecottTunnelInfoDialog(Shell shell, CloudFoundryServer cloudServer, List<String> servicesWithTunnels) {
		super(shell);
		setBlockOnOpen(false);
		this.cloudServer = cloudServer;
		this.servicesWithTunnels = servicesWithTunnels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.
	 * swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		ImageDescriptor descriptor = CloudFoundryImages
				.getWizardBanner(cloudServer.getServer().getServerType().getId());
		setTitle("Tunnel Information");
		
		if (descriptor != null) {
			setTitleImage(CloudFoundryImages.getImage(descriptor));
		}

		// No help support yet
		setHelpAvailable(false);

		part = new TunnelDisplayPart(getShell(), cloudServer, servicesWithTunnels);

		Control area = part.createControl(parent);
		applyDialogFont(area);
		return area;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// Only OK button is needed as this is an info dialogue only
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
}
