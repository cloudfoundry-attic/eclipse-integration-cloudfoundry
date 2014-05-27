/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.rse.internal;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.osgi.framework.BundleContext;



/**
 * The activator class controls the plug-in life cycle
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class CloudFoundryRsePlugin extends SystemBasePlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.rse"; //$NON-NLS-1$

	// The shared instance
	private static CloudFoundryRsePlugin plugin;

	/**
	 * The constructor
	 */
	public CloudFoundryRsePlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	@Override
	protected void initializeImageRegistry() {
		// TODO Auto-generated method stub

	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CloudFoundryRsePlugin getDefault() {
		return plugin;
	}

	public static boolean doesServerBelongToHost(IServer server, IHost host) {
		if (host != null && server != null) {
			IRSESystemType rseSystem = host.getSystemType();
			IServerType serverType = server.getServerType();
			if (rseSystem != null) {
				String hostSystemType = rseSystem.getId();
				if (serverType != null && serverType.getId() != null) {
					String serverSystemType = CloudFoundryBrandingExtensionPoint.getRemoteSystemTypeId(serverType.getId());
					return (hostSystemType != null && hostSystemType.equals(serverSystemType));
				}
			}
		}
		return false;
	}

}
