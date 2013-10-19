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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerListener;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
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
		CloudFoundryPlugin.getDefault().addServerListener(listener);
	}

	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof ModuleServer) {
			ModuleServer moduleServer = (ModuleServer) element;
			IServer s = moduleServer.getServer();
			if (s != null && isCloudFoundryServerType(s)) {
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
			if (isCloudFoundryServerType(server)) {
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
		CloudFoundryPlugin.getDefault().removeServerListener(listener);
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

	private boolean isCloudFoundryServerType(IServer server) {
		IServerType serverType = server.getServerType();
		return serverType != null && serverType.getId().startsWith("org.cloudfoundry.appcloudserver");
	}

}
