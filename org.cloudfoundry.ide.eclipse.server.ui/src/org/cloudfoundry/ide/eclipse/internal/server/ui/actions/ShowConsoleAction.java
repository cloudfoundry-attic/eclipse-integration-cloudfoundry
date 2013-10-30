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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.jface.action.Action;

/**
 * @author Steffen Pingel
 */
public class ShowConsoleAction extends Action {

	private final CloudFoundryServer server;

	private final CloudFoundryApplicationModule appModule;

	private final int instanceIndex;

	public ShowConsoleAction(CloudFoundryServer server, CloudFoundryApplicationModule appModule, int instanceIndex) {
		this.server = server;
		this.appModule = appModule;
		this.instanceIndex = instanceIndex;
		setText("Show Console");
	}

	@Override
	public void run() {
		if (CloudFoundryPlugin.getCallback() != null) {
			CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, server);
			CloudFoundryPlugin.getCallback().startApplicationConsole(server, appModule, instanceIndex);
		}
		else {
			CloudFoundryPlugin
					.logError("No Cloud Foundry console callback available. Unable to refresh console contents.");
		}
	}

}
