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

	protected boolean refreshed = false;

	protected final CloudFoundryServer cloudServer;

	protected String error;

	protected final int eventToExpect;

	protected CloudServerEvent matchedEvent = null;

	public CloudServerEvent getMatchedEvent() {
		return matchedEvent;
	}

	public ModulesRefreshListener(CloudFoundryServer cloudServer, int eventToExpect) {
		this.cloudServer = cloudServer;
		this.eventToExpect = eventToExpect;
		ServerEventHandler.getDefault().addServerListener(this);
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

		if (!refreshed || matchedEvent == null) {
			error = "Timed out waiting for refresh event: " + eventToExpect;
		}
		else if (matchedEvent.getType() != eventToExpect) {
			error = "Expected refresh event: " + eventToExpect + " but got event type: " + matchedEvent.getType();
		}

		if (error != null) {
			throw CloudErrorUtil.toCoreException(error);
		}
		return refreshed;
	}

	public boolean hasBeenRefreshed() {
		return refreshed || error != null;
	}

	public void dispose() {
		ServerEventHandler.getDefault().removeServerListener(this);
	}

	@Override
	public void serverChanged(CloudServerEvent event) {
		// if already refreshed on a previous notification do not handle any
		// more events
		if (refreshed || error != null) {
			return;
		}

		if (event.getServer() == null) {
			error = "Null Cloud server in event.";
		}
		else if (event.getType() == eventToExpect) {
			processEvent(event);
		}
	}

	protected void processEvent(CloudServerEvent event) {
		matchedEvent = event;
		refreshed = true;
	}

	/**
	 *
	 * @param cloudServer
	 * @return
	 */
	public static ModulesRefreshListener getListener(String appName, CloudFoundryServer cloudServer, int expectedEvent) {
		return new ModulesRefreshListener(cloudServer, expectedEvent);
	}
}
