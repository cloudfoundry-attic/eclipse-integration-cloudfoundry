/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudRoutePart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Edits existing Cloud routes.
 */
public class CloudRoutesWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CloudRoutesPage routePage;

	public CloudRoutesWizard(CloudFoundryServer server) {
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {

		routePage = new CloudRoutesPage(cloudServer);
		routePage.setWizard(this);
		addPage(routePage);
	}

	@Override
	public boolean performFinish() {

		final List<CloudRoute> toDelete = routePage.getRoutesToDelete();

		if (!toDelete.isEmpty()) {
			CloudUiUtil.runForked(new ICoreRunnable() {
				public void run(final IProgressMonitor monitor) throws CoreException {
					cloudServer.getBehaviour().deleteRoute(toDelete, monitor);
				}
			}, this);
		}

		return true;
	}

	static class CloudRoutesPage extends PartsWizardPage {

		private CloudRoutePart routePart;

		private final CloudFoundryServer server;

		public CloudRoutesPage(CloudFoundryServer server) {
			super("Cloud Route Page", "Cloud Routes", CloudFoundryImages.getWizardBanner(server.getServer()
					.getServerType().getId()));
			setDescription(Messages.ROUTE_PAGE_DESCRIPTION);
			this.server = server;
		}

		public List<CloudRoute> getRoutesToDelete() {
			return routePart.getRoutesToDelete();
		}

		public boolean isPageComplete() {
			return super.isPageComplete() && getRoutesToDelete() != null && !getRoutesToDelete().isEmpty();
		}

		protected void performWhenPageVisible() {
			updateRoutes();
		}

		protected void updateRoutes() {

			runAsynchWithWizardProgress(new ICoreRunnable() {

				@Override
				public void run(IProgressMonitor monitor) throws CoreException {

					CloudFoundryServerBehaviour behaviour = server.getBehaviour();

					List<CloudDomain> domains = behaviour.getDomainsForSpace(monitor);

					final List<CloudRoute> allRoutes = new ArrayList<CloudRoute>();

					if (domains != null) {
						for (CloudDomain domain : domains) {

							List<CloudRoute> routes = behaviour.getRoutes(domain.getName(), monitor);
							if (routes != null) {
								allRoutes.addAll(routes);

								// Note that fetching routes per domain may be a
								// long running process.
								// Update the UI as routes are fetched to
								// indicate progress to the user (In addition to
								// the progress monitor)
								Display.getDefault().syncExec(new Runnable() {

									public void run() {
										routePart.setInput(allRoutes);
									}

								});
							}
						}
					}

				}

			}, Messages.REFRESHING_DOMAIN_ROUTES);
		}

		public void createControl(Composite parent) {
			routePart = new CloudRoutePart();
			routePart.addPartChangeListener(this);

			Control control = routePart.createPart(parent);
			setControl(control);
		}

		@Override
		public void handleChange(PartChangeEvent event) {

			if (event.getSource() == CloudRoutePart.ROUTES_REMOVED) {
				Object data = event.getData();
				if (data instanceof List<?>) {
					List<?> routes = (List<?>) data;
					IStatus errorInUse = null;
					for (Object obj : routes) {
						if (obj instanceof CloudRoute) {
							CloudRoute rt = (CloudRoute) obj;
							if (rt.inUse()) {
								errorInUse = CloudFoundryPlugin.getStatus(
										NLS.bind(Messages.ERROR_ROUTE_IN_USE, rt.getName()), IStatus.ERROR);
							}
						}
					}
					if (errorInUse != null) {
						event = new PartChangeEvent(event.getData(), errorInUse, event.getSource());
					}

				}

			}
			super.handleChange(event);
		}
	}
}
