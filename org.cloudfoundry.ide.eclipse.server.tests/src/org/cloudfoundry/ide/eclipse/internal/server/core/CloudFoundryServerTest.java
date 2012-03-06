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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ServerDescriptor;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ServerHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;

/**
 * @author Steffen Pingel
 */
public class CloudFoundryServerTest extends TestCase {

	private CloudFoundryServer cloudFoundryServer;

	private IServer server;

	private IServerWorkingCopy serverWC;

	// there is no longer just one default url for a server type
	// public void testGetServerIdDefaultUrl() throws CoreException {
	// assertEquals(null, CloudFoundryServer.getServerId());
	//
	// CloudFoundryServer.setUrl(null);
	// assertEquals("null@http://api.cloudfoundry.com",
	// CloudFoundryServer.getServerId());
	// }

	public void testGetServerId() throws CoreException {
		// assertEquals(null, CloudFoundryServer.getServerId());

		cloudFoundryServer.setUsername("user");
		assertEquals("user@http://api.cloudfoundry.com", cloudFoundryServer.getServerId());

		cloudFoundryServer.setPassword("pass");
		assertEquals("user@http://api.cloudfoundry.com", cloudFoundryServer.getServerId());

		cloudFoundryServer.setUrl("http://url");
		assertEquals("user@http://url", cloudFoundryServer.getServerId());
	}

	public void testGetPasswordLegacy() throws CoreException {
		// create legacy password attribute
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.password", "pwd");
		serverWC.save(true, null);

		// assertEquals(null, CloudFoundryServer.getServerId());

		// create new server instance
		serverWC = server.createWorkingCopy();
		cloudFoundryServer = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class, null);

		assertEquals("pwd", cloudFoundryServer.getPassword());

		ServerCredentialsStore store = cloudFoundryServer.getCredentialsStore();
		assertEquals(null, store.getPassword());

		serverWC.save(true, null);
		assertEquals("Unexpected migration of password to secure store although it was not changed", null,
				store.getPassword());
	}

	public void testSetPasswordMigrateLegacy() throws CoreException {
		// create legacy password attribute
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.password", "pwd");
		serverWC.save(true, null);

		// create new server instance
		serverWC = server.createWorkingCopy();
		cloudFoundryServer = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class, null);

		cloudFoundryServer.setPassword("newpwd");
		assertEquals("newpwd", cloudFoundryServer.getPassword());

		serverWC.save(true, null);
		ServerCredentialsStore store = cloudFoundryServer.getCredentialsStore();
		assertEquals("newpwd", store.getPassword());
	}

	public void testSetPasswordMigrateChangePasswordOnly() throws CoreException {
		// create legacy password attribute
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.url", "http://url");
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.username", "user");
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.password", "pwd");
		serverWC.save(true, null);

		// create new server instance
		serverWC = server.createWorkingCopy();
		cloudFoundryServer = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class, null);
		cloudFoundryServer.setPassword("newpwd");
		assertEquals("newpwd", cloudFoundryServer.getPassword());
		assertEquals("user@http://url", cloudFoundryServer.getServerId());

		serverWC.save(true, null);
		cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		assertEquals("user@http://url", cloudFoundryServer.getServerId());
		assertEquals("newpwd", cloudFoundryServer.getPassword());
	}

	public void testSetPasswordMigrateUpdatesClient() throws CoreException {
		// create legacy password attribute
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.password", "pwd");
		((ServerWorkingCopy) serverWC).setAttribute("org.cloudfoundry.ide.eclipse.username", "user");
		// ((ServerWorkingCopy)
		// serverWC).setAttribute("org.cloudfoundry.ide.eclipse.url", "url");
		serverWC.save(true, null);

		assertEquals("user", cloudFoundryServer.getUsername());
		assertEquals("pwd", cloudFoundryServer.getPassword());
		CloudFoundryServerBehaviour CloudFoundryServerBehaviour = (CloudFoundryServerBehaviour) serverWC.loadAdapter(
				CloudFoundryServerBehaviour.class, null);
		CloudFoundryClient client = CloudFoundryServerBehaviour.getClient();

		// create new server instance
		serverWC = server.createWorkingCopy();
		cloudFoundryServer.setPassword("newpwd");
		cloudFoundryServer.setUsername("newuser");
		cloudFoundryServer.setUrl("http://api.cloudfoundry.com");
		serverWC.save(true, null);

		// verify that old instance is updated
		assertEquals("newuser", cloudFoundryServer.getUsername());
		assertEquals("newpwd", cloudFoundryServer.getPassword());
		assertNotSame("Expected new client instance due to password change", client,
				CloudFoundryServerBehaviour.getClient());
	}

	public void testSaveCredentials() throws CoreException {
		cloudFoundryServer.setUsername("user");
		cloudFoundryServer.setPassword("pass");
		assertEquals("user", cloudFoundryServer.getUsername());
		assertEquals("pass", cloudFoundryServer.getPassword());

		serverWC.save(true, null);
		assertEquals("user", cloudFoundryServer.getUsername());
		assertEquals("pass", cloudFoundryServer.getPassword());

		cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		assertEquals("user", cloudFoundryServer.getUsername());
		assertEquals("pass", cloudFoundryServer.getPassword());
	}

	@Override
	protected void setUp() throws Exception {
		ServerDescriptor descriptor = new ServerDescriptor("server") {
			{
				setRuntimeTypeId("org.cloudfoundry.cloudfoundryserver.test.runtime.10");
				setServerTypeId("org.cloudfoundry.cloudfoundryserver.test.10");
				setRuntimeName("Cloud Foundry Test Runtime");
				setServerName("Cloud Foundry Test Server");
				setForceCreateRuntime(true);
			}
		};

		ServerHandler handler = new ServerHandler(descriptor);
		server = handler.createServer(new NullProgressMonitor(), ServerHandler.ALWAYS_OVERWRITE);
		serverWC = server.createWorkingCopy();
		cloudFoundryServer = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class, null);

		// there is no longer a singer default URL for a server type
		cloudFoundryServer.setUrl("http://api.cloudfoundry.com");
	}

}
