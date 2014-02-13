/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

/**
 * Specialisation of the client request that updates the local Cloud Foundry
 * server instance during the operation of the client request. Generally, most
 * client calls should using the local server request, although in certain cases
 * where a local server instance is not yet available, the client calls should
 * instead user the {@link ClientRequest}
 * 
 * @param <T>
 */
public abstract class LocalServerRequest<T> extends ClientRequest<T> {

	public LocalServerRequest(String label) {
		super(label);
	}

	@Override
	public T runAsClientRequestCheckConnection(CloudFoundryOperations client, SubMonitor monitor) throws CoreException {
		CloudFoundryServer cloudServer = getCloudServer();
		if (cloudServer.getUsername() == null || cloudServer.getUsername().length() == 0
				|| cloudServer.getPassword() == null || cloudServer.getPassword().length() == 0) {
			CloudFoundryPlugin.getCallback().getCredentials(cloudServer);
		}

		Server server = (Server) cloudServer.getServer();

		// Any Server request will require the server to be connected, so update
		// the server state
		if (server.getServerState() == IServer.STATE_STOPPED || server.getServerState() == IServer.STATE_STOPPING) {
			server.setServerState(IServer.STATE_STARTING);
		}

		try {
			T result = super.runAsClientRequestCheckConnection(client, monitor);

			// No errors at this stage, therefore assume operation was completed
			// successfully, and update
			// server state accordingly
			if (server.getServerState() != IServer.STATE_STARTED) {
				server.setServerState(IServer.STATE_STARTED);
			}
			return result;

		}
		catch (CoreException ce) {
			// If the server state was starting and the error is related when
			// the operation was
			// attempted, but the operation failed
			// set the server state back to stopped.
			if (CloudErrorUtil.getConnectionError(ce) != null && server.getServerState() == IServer.STATE_STARTING) {
				server.setServerState(IServer.STATE_STOPPED);
			}
			// server.setServerPublishState(IServer.PUBLISH_STATE_NONE);
			throw ce;
		}

	}

	@Override
	protected CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
		return getCloudServer().getBehaviour().getClient(monitor);
	}

	/**
	 * 
	 * @return non-null Cloud Foundry server instance. If it cannot be resolved,
	 * throw {@link CoreException}
	 * @throws CoreException if Cloud Foundry server cannot be resolved.
	 */
	abstract protected CloudFoundryServer getCloudServer() throws CoreException;
}
