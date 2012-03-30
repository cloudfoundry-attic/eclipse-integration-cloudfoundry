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
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.AddServicesToApplicationAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;

public class StartAndAddCaldecottService extends AddServicesToApplicationAction {

	public StartAndAddCaldecottService(IStructuredSelection selection, ApplicationModule caldecottApp,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {
		super(selection, caldecottApp, serverBehaviour, editorPage);
	}

	@Override
	public String getJobName() {
		return "Start Caldecott Tunnel";
	}

	@Override
	public IStatus performAction(IProgressMonitor monitor) throws CoreException {
		super.performAction(monitor);
		// Create tunnel once services have been added
		List<String> servicesToAdd = getServicesToAdd();
		if (servicesToAdd != null && !servicesToAdd.isEmpty()) {
			CloudFoundryServerBehaviour behaviour = getBehavior();
			for (String serviceName : servicesToAdd) {
				behaviour.startCaldecottTunnel(serviceName, monitor);
			}
		}

		return Status.OK_STATUS;
	}

}
