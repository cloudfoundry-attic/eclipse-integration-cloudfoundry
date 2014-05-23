/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudServerSpacesDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerHandler;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerHandlerCallback;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Creates a new server instance from a selected space in an organization and
 * spaces viewer in the wizard. It only creates the server instance if another
 * server instance to that space does not exist.
 */
public class OrgsAndSpacesWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CloneServerPage cloudSpacePage;

	public OrgsAndSpacesWizard(CloudFoundryServer server) {
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {

		cloudSpacePage = new CloneServerPage(cloudServer);
		cloudSpacePage.setWizard(this);
		addPage(cloudSpacePage);
	}

	@Override
	public boolean performFinish() {

		final CloudSpace selectedSpace = cloudSpacePage.getSelectedCloudSpace();

		// Only create a new space, if it doesnt match the existing space
		if (selectedSpace != null
				&& !CloudServerSpacesDelegate.matchesExisting(selectedSpace, cloudServer.getCloudFoundrySpace())) {

			String serverName = cloudSpacePage.getServerName();
			final ServerDescriptor descriptor = ServerDescriptor.getServerDescriptor(cloudServer, serverName);

			if (descriptor == null) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_NO_CLOUD_SERVER_DESCRIPTOR,
						cloudServer.getServerId()));
				return false;
			}
			else {
				final String password = cloudServer.getPassword();
				final String userName = cloudServer.getUsername();
				final String url = cloudServer.getUrl();
				final boolean selfSignedCert = cloudServer.getSelfSignedCertificate();

				CloudUiUtil.runForked(new ICoreRunnable() {
					public void run(final IProgressMonitor monitor) throws CoreException {

						ServerHandler serverHandler = new ServerHandler(descriptor);

						serverHandler.createServer(monitor, ServerHandler.NEVER_OVERWRITE, new ServerHandlerCallback() {

							@Override
							public void configureServer(IServerWorkingCopy wc) throws CoreException {
								CloudFoundryServer cloudServer = (CloudFoundryServer) wc.loadAdapter(
										CloudFoundryServer.class, null);

								if (cloudServer != null) {
									cloudServer.setPassword(password);
									cloudServer.setUsername(userName);
									cloudServer.setUrl(url);
									cloudServer.setSpace(selectedSpace);
									cloudServer.setSelfSignedCertificate(selfSignedCert);
									cloudServer.saveConfiguration(monitor);
								}
							}
						});
					}
				}, this);
			}
		}
		return true;
	}
}
