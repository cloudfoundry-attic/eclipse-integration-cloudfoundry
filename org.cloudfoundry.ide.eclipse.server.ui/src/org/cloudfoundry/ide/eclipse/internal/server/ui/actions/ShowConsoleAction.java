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

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.eclipse.jface.action.Action;


/**
 * @author Steffen Pingel
 */
public class ShowConsoleAction extends Action {

	private final CloudFoundryServer server;

	private final CloudApplication app;

	private final int instanceIndex;

	public ShowConsoleAction(CloudFoundryServer server, CloudApplication app, int instanceIndex) {
		this.server = server;
		this.app = app;
		this.instanceIndex = instanceIndex;
		setText("Show Console");
	}

	@Override
	public void run() {
		ConsoleManager.getInstance().startConsole(server, app, instanceIndex, true);
	}

}
