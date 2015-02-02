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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.WaitWithProgressJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Listens for module refresh events. It does not trigger refresh operations,
 * but rather listens for them when other Cloud operations trigger refresh
 * events.
 * <p/>
 * Since module refreshes are performed asynchronously in the Cloud server
 * instance, this module refresh test listener will register a listener to
 * detect module refresh operation completion.
 * <p/>
 * Checking if refresh of modules requires two steps:
 * <p/>
 * 1. Create this handler BEFORE an operation that triggers a refresh is
 * performed
 * <p/>
 * 2. Invoke the API {@link #modulesRefreshed()} AFTER that operation completes
 * to check if refresh has occurred. The handler will wait to be notified when
 * refresh is completed so it will block the thread once
 * {@link #modulesRefreshed()} is invoked
 * <p/>
 * Full, server-wide module refresh occurs when connecting the server or
 * performing non-application specific operations, like creating or deleting a
 * service
 * <p/>
 * Module-specific refresh occurs when performing application operations, like
 * binding/unbinding service to application, updating instances, updating memory
 * and app URL, or pushing, starting, restarting or stopping an application.
 * <p/>
 * Note that the handler blocks the current thread.
 *
 *
 */
public class ModulesRefreshListener implements CloudServerListener {

	protected boolean refreshed;

	protected final CloudFoundryServer cloudServer;

	protected String error;

	protected final int eventToExpect;

	protected int actualEvent;

	public int getActualEventType() {
		return actualEvent;
	}

	ModulesRefreshListener(CloudFoundryServer cloudServer, int eventToExpect) {
		this.cloudServer = cloudServer;
		this.eventToExpect = eventToExpect;
		ServerEventHandler.getDefault().addServerListener(this);
	}

	public boolean isCurrentlyRefreshed() {
		return refreshed || error != null;
	}

	/**
	 * Waits to determine if modules have been refreshed.
	 * @return
	 * @throws CoreException
	 */
	public boolean modulesRefreshed(IProgressMonitor monitor) throws CoreException {
		// Wait until the listener has been notified that refresh is complete

		new WaitWithProgressJob(30, 3000) {

			@Override
			protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException {
				return refreshed;
			}
		}.run(monitor);

		if (actualEvent != eventToExpect) {
			error = "Expected refresh event: " + eventToExpect + " but got event type: " + actualEvent;

		}
		else if (!refreshed) {
			error = "Timed out waiting for refresh event: " + eventToExpect;
		}

		if (error != null) {
			throw CloudErrorUtil.toCoreException(error);
		}
		return refreshed;
	}

	public void dispose() {
		ServerEventHandler.getDefault().removeServerListener(this);
	}

	@Override
	public void serverChanged(CloudServerEvent event) {

		error = null;
		refreshed = true;
		actualEvent = event.getType();
		if (event.getServer() == null) {
			error = "Null Cloud server in event.";
		}
	}

	static class SingleModuleRefreshListener extends ModulesRefreshListener {

		protected final String appName;

		protected SingleModuleRefreshListener(String appName, CloudFoundryServer cloudServer, int expectedEvent) {
			super(cloudServer, expectedEvent);
			this.appName = appName;
		}

		@Override
		public void serverChanged(CloudServerEvent event) {
			error = null;
			refreshed = true;
			actualEvent = event.getType();
			if (event.getServer() == null) {
				error = "Null Cloud server in event.";
			}
			else if (actualEvent == eventToExpect) {
				if (event instanceof ModuleChangeEvent) {
					ModuleChangeEvent appEvent = (ModuleChangeEvent) event;

					CloudFoundryApplicationModule appModule = event.getServer().getExistingCloudModule(
							appEvent.getModule());
					if (appModule == null) {
						error = "Expected non-null appModule for event " + event;
					}
					else if (!appName.equals(appModule.getDeployedApplicationName())) {
						error = "Expected refresh event for " + appName + " but got event for: "
								+ appModule.getDeployedApplicationName();
					}
				}
				else {
					error = "Expected " + ModuleChangeEvent.class.toString() + " but got "
							+ event.getClass().toString();
				}
			}
		}
	}

	/**
	 * Returns appropriate refresh listener based on whether a full refresh of
	 * modules is expected ( app name is null) or refresh for only one
	 * application module (app name is not null)
	 * @param appName if refresh should be expected on only one app. Null if
	 * refresh should be expected on all modules.
	 * @param cloudServer
	 * @return
	 */
	public static ModulesRefreshListener getListener(String appName, CloudFoundryServer cloudServer, int expectedEvent) {
		return appName != null ? new SingleModuleRefreshListener(appName, cloudServer, expectedEvent)
				: new ModulesRefreshListener(cloudServer, expectedEvent);
	}
}
