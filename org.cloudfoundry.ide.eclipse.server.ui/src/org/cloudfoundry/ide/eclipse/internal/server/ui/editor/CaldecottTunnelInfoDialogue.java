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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.TunnelDisplayPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

public class CaldecottTunnelInfoDialogue extends Dialog {

	private final CloudFoundryServer cloudServer;

	private final List<String> servicesWithTunnels;

	public CaldecottTunnelInfoDialogue(Shell parent, CloudFoundryServer cloudServer, List<String> servicesWithTunnels) {
		super(parent);
		this.cloudServer = cloudServer;
		this.servicesWithTunnels = servicesWithTunnels;

	}

	protected Control createDialogArea(Composite parent) {
		TunnelDisplayPart part = new TunnelDisplayPart(getParent(), cloudServer, servicesWithTunnels);
		Control area = part.createControl(parent);

		return area;
	}

}
