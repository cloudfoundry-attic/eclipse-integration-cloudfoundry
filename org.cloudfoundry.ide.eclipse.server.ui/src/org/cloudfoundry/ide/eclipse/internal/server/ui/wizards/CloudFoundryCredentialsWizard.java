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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpaceDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Prompts for the password if an operation requires authentication.
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryCredentialsWizard extends Wizard {

	private final CloudFoundryServer server;

	private final IServerWorkingCopy serverWC;

	private CloudFoundryCloudSpaceWizardpage cloudSpacePage;

	public CloudFoundryCredentialsWizard(CloudFoundryServer server) {
		serverWC = server.getServer().createWorkingCopy();
		this.server = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class, null);
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		CloudFoundryCredentialsWizardPage page = new CloudFoundryCredentialsWizardPage(server);

		addPage(page);

	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof CloudFoundryCredentialsWizardPage) {
			CloudFoundryCredentialsWizardPage credentialsPage = (CloudFoundryCredentialsWizardPage) page;
			CloudSpaceDescriptor spacesDescriptor = credentialsPage.getCloudSpacesDescriptor();
			if (spacesDescriptor != null && spacesDescriptor.supportsSpaces()) {
				cloudSpacePage = new CloudFoundryCloudSpaceWizardpage(server, null);
				cloudSpacePage.setCloudSpacesDescriptor(spacesDescriptor);
				return cloudSpacePage;
			}
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			serverWC.save(true, null);
		}
		catch (CoreException e) {
			CloudFoundryServerUiPlugin.getDefault().getLog().log(e.getStatus());
		}
		return true;
	}

}
