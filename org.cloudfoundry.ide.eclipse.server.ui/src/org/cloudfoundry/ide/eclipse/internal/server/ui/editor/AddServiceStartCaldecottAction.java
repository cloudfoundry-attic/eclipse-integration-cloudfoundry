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
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.AddServicesToApplicationAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

public class AddServiceStartCaldecottAction extends AddServicesToApplicationAction {

	private final String jobName;

	public AddServiceStartCaldecottAction(List<String> services, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage, String jobName) {
		// Null application module, as it is resolved only at action run time
		super(services, null, serverBehaviour, editorPage, RefreshArea.ALL);
		this.jobName = jobName;
		setText(jobName);
		setImageDescriptor(CloudFoundryImages.CONNECT);
	}

	@Override
	public String getJobName() {
		return jobName;
	}

	@Override
	public IStatus performAction(IProgressMonitor monitor) throws CoreException {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(getBehavior().getCloudFoundryServer());

		IModule caldecottApp = handler.getCaldecottModule(monitor);
		if (caldecottApp instanceof ApplicationModule) {

			// Application Module MUST be set first before invoking parent
			// action, as the latter
			// requires a valid application module
			setApplicationModule((ApplicationModule) caldecottApp);

			super.performAction(monitor);
			// Create tunnel once services have been added
			List<String> servicesToAdd = getServicesToAdd();
			if (servicesToAdd != null && !servicesToAdd.isEmpty()) {
				handler.startCaldecottTunnel(servicesToAdd.get(0), monitor);
			}

			return Status.OK_STATUS;
		}
		else {
			return CloudFoundryPlugin
					.getErrorStatus("Unable to resolve Caldecott application module. Failed to open Caldecott tunnel.");
		}

	}

}
