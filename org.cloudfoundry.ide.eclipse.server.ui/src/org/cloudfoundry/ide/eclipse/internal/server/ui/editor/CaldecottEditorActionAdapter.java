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

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

public class CaldecottEditorActionAdapter {

	private final CloudFoundryApplicationsEditorPage editorPage;

	private final CloudFoundryServerBehaviour behaviour;

	public CaldecottEditorActionAdapter(CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		this.behaviour = serverBehaviour;
		this.editorPage = editorPage;
	}

	public void addServiceAndCreateTunnel(List<String> services, IProgressMonitor monitor) {

		try {
			IModule caldecottModule = new CaldecottTunnelHandler(behaviour.getCloudFoundryServer())
					.getCaldecottModule(monitor);
			if (caldecottModule instanceof ApplicationModule) {
				new StartAndAddCaldecottService(services, (ApplicationModule) caldecottModule, behaviour, editorPage,
						getActionName()).run();
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(
					"Failed to add Caldecott Tunnel editor action. Check server connection and try again.", e);
		}
	}

	public String getActionName() {
		return "Open New Caldecott Tunnel...";
	}

}
