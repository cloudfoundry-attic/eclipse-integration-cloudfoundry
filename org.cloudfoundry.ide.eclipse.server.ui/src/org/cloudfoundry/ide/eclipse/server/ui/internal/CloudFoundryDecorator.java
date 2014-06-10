/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 *     Elson Yuen, IBM - Improve logic in determining whether a server type is a Cloud Foundry Server
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.ui.internal.view.servers.ModuleServer;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class CloudFoundryDecorator extends LabelProvider implements ILightweightLabelDecorator {

	private final CloudServerListener listener;

	public CloudFoundryDecorator() {
		this.listener = new CloudServerListener() {
			public void serverChanged(final CloudServerEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						LabelProviderChangedEvent labelEvent = new LabelProviderChangedEvent(CloudFoundryDecorator.this);
						fireLabelProviderChanged(labelEvent);
					}
				});
			}
		};
		ServerEventHandler.getDefault().addServerListener(listener);
	}

	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof ModuleServer) {
			ModuleServer moduleServer = (ModuleServer) element;
			IServer s = moduleServer.getServer();
			if (s != null && CloudServerUtil.isCloudFoundryServer(s)) {
				IModule[] modules = moduleServer.getModule();
				if (modules != null && modules.length == 1) {
					CloudFoundryServer server = getCloudFoundryServer(moduleServer.getServer());
					if (server == null || !server.isConnected()) {
						return;

					}
					CloudFoundryApplicationModule module = server.getExistingCloudModule(modules[0]);

					// module may no longer exist
					if (module == null) {
						return;
					}

					if (module.getLocalModule() != null) {
						// show local information?
					}

					CloudApplication application = module.getApplication();
					// if (application != null) {
					// decoration.addSuffix(NLS.bind("  [{0}, {1}, {2}]",
					// new Object[]{application.getName(),
					// getAppStateString(application.getState()),
					// application.getUris()}));
					// } else if (module.getName() != null) {
					// decoration.addSuffix(NLS.bind("  [{0}]",
					// module.getName()));
					// }
					if (application != null) {
						decoration.addSuffix(NLS.bind(" - Deployed as {0}", application.getName()));
					}
					else {
						decoration.addSuffix(" - Not Deployed");
					}

					if (module.getErrorMessage() != null) {
						decoration.addOverlay(CloudFoundryImages.OVERLAY_ERROR, IDecoration.BOTTOM_LEFT);
					}
				}
			}
		}
		else if (element instanceof Server) {
			Server server = (Server) element;
			if (CloudServerUtil.isCloudFoundryServer(server)) {
				CloudFoundryServer cfServer = getCloudFoundryServer(server);
				if (cfServer != null && cfServer.getUsername() != null) {
					// decoration.addSuffix(NLS.bind("  [{0}, {1}]",
					// cfServer.getUsername(), cfServer.getUrl()));
					if (cfServer.hasCloudSpace()) {
						CloudFoundrySpace clSpace = cfServer.getCloudFoundrySpace();
						if (clSpace != null) {
							decoration
									.addSuffix(NLS.bind(" - {0} - {1}", clSpace.getOrgName(), clSpace.getSpaceName()));

						}
					}
					List<CloudServerURL> cloudUrls = CloudUiUtil.getAllUrls(cfServer.getBehaviour().getServer()
							.getServerType().getId());
					String url = cfServer.getUrl();
					// decoration.addSuffix(NLS.bind("  {0}",
					// cfServer.getUsername()));
					for (CloudServerURL cloudUrl : cloudUrls) {
						if (cloudUrl.getUrl().equals(url)) {
							decoration.addSuffix(NLS.bind(" - {0}", cloudUrl.getUrl()));
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		ServerEventHandler.getDefault().removeServerListener(listener);
	}

	private CloudFoundryServer getCloudFoundryServer(IServer server) {
		Object obj = server.getAdapter(CloudFoundryServer.class);
		if (obj instanceof CloudFoundryServer) {
			return (CloudFoundryServer) obj;
		}
		return null;
	}

	// private String getAppStateString(AppState state) {
	// if (state == AppState.STARTED) {
	// return "Started";
	// }
	// if (state == AppState.STOPPED) {
	// return "Stopped";
	// }
	// if (state == AppState.UPDATING) {
	// return "Updating";
	// }
	// return "unknown";
	// }

}
