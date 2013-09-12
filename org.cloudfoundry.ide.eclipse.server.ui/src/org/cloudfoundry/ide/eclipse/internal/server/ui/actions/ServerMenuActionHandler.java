/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.eclipse.jface.action.IAction;
import org.eclipse.wst.server.core.IServer;

public class ServerMenuActionHandler extends MenuActionHandler<IServer> {

	protected ServerMenuActionHandler() {
		super(IServer.class);
	}

	@Override
	protected List<IAction> getActionsFromSelection(IServer server) {
		CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		if (cloudFoundryServer == null || server.getServerState() != IServer.STATE_STARTED) {
			return Collections.emptyList();
		}
		List<IAction> actions = new ArrayList<IAction>();

		if (new TunnelBehaviour(cloudFoundryServer).hasCaldecottTunnels()) {
			actions.add(new CaldecottTunnelAction(cloudFoundryServer));
			actions.add(new CaldecottDisconnectAllAction(cloudFoundryServer));
		}

		return actions;
	}

}
