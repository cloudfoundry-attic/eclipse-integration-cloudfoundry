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
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RemoveServicesFromApplicationAction extends ModifyServicesForApplicationAction {

	private final List<String> services;

	public RemoveServicesFromApplicationAction(IStructuredSelection selection, CloudFoundryApplicationModule application,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {
		super(application, serverBehaviour, editorPage);

		setText("Unbind from Application");
		setImageDescriptor(CloudFoundryImages.REMOVE);

		services = getServiceNames(selection);
	}

	@Override
	public String getJobName() {
		return "Unbinding services";
	}

	@Override
	public List<String> getServicesToAdd() {
		return new ArrayList<String>();
	}

	@Override
	public List<String> getServicesToRemove() {
		return services;
	}

	protected void updateServices(IProgressMonitor monitor, CloudFoundryApplicationModule appModule,
			CloudFoundryServerBehaviour serverBehaviour, List<String> updatedServices) throws CoreException {
		serverBehaviour.updateServicesAndCloseCaldecottTunnels(appModule.getDeployedApplicationName(), updatedServices, monitor);
	}

}
