/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.CaldecottUIHelper;
import org.eclipse.jface.action.Action;

public class CaldecottTunnelAction extends Action {

	protected final CloudFoundryServer cloudServer;

	public CaldecottTunnelAction(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		setActionValues();
	}

	protected void setActionValues() {
		setText("Show Tunnels...");
		setImageDescriptor(CloudFoundryImages.CONNECT);
		setToolTipText("Show active tunnels");
		setEnabled(true);
	}

	public void run() {
		new CaldecottUIHelper(cloudServer).openCaldecottTunnelWizard();
	}
}
