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
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
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
