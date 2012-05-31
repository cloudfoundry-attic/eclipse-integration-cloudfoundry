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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RemoveServicesFromApplicationAction extends ModifyServicesForApplicationAction {

	private final List<String> services;

	public RemoveServicesFromApplicationAction(IStructuredSelection selection, ApplicationModule application,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {
		super(application, serverBehaviour, editorPage, RefreshArea.ALL);

		setText("Remove from Application");
		setImageDescriptor(CloudFoundryImages.REMOVE);

		services = getServiceNames(selection);
	}

	@Override
	public IStatus performAction(IProgressMonitor monitor) throws CoreException {
		IStatus status = super.performAction(monitor);
		// Remove any Caldecott tunnels associated with removed services.
		List<String> servicesToRemove = getServicesToRemove();

		if (servicesToRemove != null) {
			CaldecottTunnelHandler handler = new CaldecottTunnelHandler(getBehavior().getCloudFoundryServer());
			for (String serviceName : servicesToRemove) {
				handler.stopAndDeleteCaldecottTunnel(serviceName, monitor);
			}
		}

		return status;
	}

	@Override
	public String getJobName() {
		return "Removing services";
	}

	@Override
	public List<String> getServicesToAdd() {
		return new ArrayList<String>();
	}

	@Override
	public List<String> getServicesToRemove() {
		return services;
	}

}
