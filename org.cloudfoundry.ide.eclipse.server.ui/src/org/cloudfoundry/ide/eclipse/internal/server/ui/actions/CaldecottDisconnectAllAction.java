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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.action.Action;

public class CaldecottDisconnectAllAction extends Action {

	protected final CloudFoundryServer cloudServer;

	public CaldecottDisconnectAllAction(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		setActionValues();
	}

	protected void setActionValues() {
		setText("Disconnect All Caldecott Tunnels");
		setImageDescriptor(CloudFoundryImages.CONNECT);
		setToolTipText("Disconnect All Caldecott Tunnels");
		setEnabled(true);
	}

	public void run() {
		new CaldecottTunnelHandler(cloudServer).stopAndDeleteAllTunnels();
	}
}
