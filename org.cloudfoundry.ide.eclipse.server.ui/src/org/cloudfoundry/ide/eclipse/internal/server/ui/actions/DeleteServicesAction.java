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

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServicesHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class DeleteServicesAction extends CloudFoundryEditorAction {

	private final CloudFoundryServerBehaviour serverBehaviour;

	private final ServicesHandler servicesHandler;

	public DeleteServicesAction(IStructuredSelection selection, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		super(editorPage, RefreshArea.ALL);
		this.serverBehaviour = serverBehaviour;

		setText("Delete");
		setImageDescriptor(CloudFoundryImages.REMOVE);

		servicesHandler = new ServicesHandler(selection);
	}

	@Override
	public String getJobName() {
		return "Deleting services";
	}

	@Override
	public ICloudFoundryOperation getOperation() throws CoreException {
		return serverBehaviour.getDeleteServicesOperation(servicesHandler.getServiceNames());
	}

	@Override
	public void run() {

		boolean confirm = MessageDialog.openConfirm(getEditorPage().getSite().getShell(), "Delete Services",
				"Are you sure you want to delete " + servicesHandler.toString() + " from the services list?");
		if (confirm) {
			super.run();
		}
	}

}
