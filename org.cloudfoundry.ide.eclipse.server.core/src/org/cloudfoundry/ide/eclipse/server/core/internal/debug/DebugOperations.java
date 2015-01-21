/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ApplicationChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Contains helper methods for common debug operations like launching a debug
 * Job or resolving launch for an existing running debug session.
 *
 */
public class DebugOperations {

	public static final String CLOUD_DEBUG_LAUNCH_ID = "cloudDebugLaunchID"; //$NON-NLS-1$

	public static final String CLOUD_DEBUG_SERVER = "cloudDebugServer"; //$NON-NLS-1$

	public static final String CLOUD_DEBUG_APP_NAME = "cloudDebugAppName"; //$NON-NLS-1$

	public static void addDebuggerConnectionListener(String connectionId, ILaunch launch) {
		Object source = launch.getDebugTarget();
		ConnectToDebuggerListener debugListener = new ConnectToDebuggerListener(connectionId, source);
		DebugPlugin.getDefault().addDebugEventListener(debugListener);
	}

	public static ILaunch getActiveLaunch(String launchId) {
		ILaunch launch = getLaunch(launchId);
		return launch != null && !launch.isTerminated() ? launch : null;
	}

	public static ILaunch getLaunch(String launchId) {
		if (launchId == null) {
			return null;
		}
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (ILaunch launch : launches) {
			ILaunchConfiguration config = launch.getLaunchConfiguration();
			try {
				if (launchId.equals(config.getAttribute(CLOUD_DEBUG_LAUNCH_ID, (String) null))) {
					return launch;
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logWarning(e.getMessage());
			}
		}
		return null;
	}

	public static final void fireDebugChanged(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule,
			IStatus status) {
		ServerEventHandler.getDefault().fireServerEvent(
				new ApplicationChangeEvent(cloudServer, CloudServerEvent.EVENT_APP_DEBUG, appModule, status));
	}

	/**
	 * @param cloudServer
	 * @param appModule
	 * @return
	 * @throws CoreException
	 */
	public static DebugLaunch getDebugLaunch(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule,
			IDebugProvider provider) {
		return new DebugLaunch(cloudServer, appModule, provider);
	}

	public static CloudFoundryServer getCloudServer(ILaunchConfiguration config) {
		if (config != null) {
			try {
				String serverId = config.getAttribute(DebugOperations.CLOUD_DEBUG_SERVER, (String) null);
				return CloudServerUtil.getCloudServer(serverId);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

	public static CloudFoundryApplicationModule getCloudApplication(ILaunchConfiguration config) {
		if (config != null) {
			try {
				CloudFoundryServer cloudServer = getCloudServer(config);
				if (cloudServer != null) {
					String appName = config.getAttribute(DebugOperations.CLOUD_DEBUG_APP_NAME, (String) null);
					if (appName != null) {
						return cloudServer.getExistingCloudModule(appName);
					}
				}

			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

	public static void terminateLaunch(String launchId) {
		ILaunch launch = DebugOperations.getLaunch(launchId);
		CloudFoundryServer cloudServer = null;
		CloudFoundryApplicationModule appModule = null;
		CoreException error = null;

		appModule = null;
		if (launch != null) {

			ILaunchConfiguration config = launch.getLaunchConfiguration();
			if (config != null) {
				try {
					String serverId = config.getAttribute(DebugOperations.CLOUD_DEBUG_SERVER, (String) null);
					cloudServer = CloudServerUtil.getCloudServer(serverId);
					if (cloudServer != null) {
						String appName = config.getAttribute(DebugOperations.CLOUD_DEBUG_APP_NAME, (String) null);
						if (appName != null) {
							appModule = cloudServer.getExistingCloudModule(appName);
						}
					}
				}
				catch (CoreException e) {
					error = e;
				}
			}

			if (!launch.isTerminated()) {
				try {
					launch.terminate();

				}
				catch (DebugException e) {
					CloudFoundryPlugin.logError("Failed to terminate debug connection for : " + launchId, e); //$NON-NLS-1$ 
				}
			}

			DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

			if (cloudServer == null || appModule == null) {
				String errorMessage = "Unable to resolve cloud server or application when notifying of debug termination - " + launchId; //$NON-NLS-1$ 
				CloudFoundryPlugin.logError(errorMessage, error);
			}
			else {
				DebugOperations.fireDebugChanged(cloudServer, appModule, Status.OK_STATUS);
			}
		}
	}

}
