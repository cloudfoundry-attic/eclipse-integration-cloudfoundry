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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;

/**
 * 
 * Each individual test creates only ONE application module, and checks that
 * only one module exists in the server instance.
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
public class CloudFoundryServerBehaviourTest extends AbstractCloudFoundryTest {

	public void testBaseSetupConnect() throws Exception {

		assertEquals(IServer.STATE_STARTED, serverBehavior.getServer().getServerState());
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
	}

	public void testDisconnect() throws Exception {
		assertEquals(IServer.STATE_STARTED, serverBehavior.getServer().getServerState());
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
		serverBehavior.disconnect(new NullProgressMonitor());
		assertEquals(IServer.STATE_STOPPED, serverBehavior.getServer().getServerState());
	}

	public void testCreateStartAppHarness() throws Exception {
		String prefix = "testStart";
		createPerTestWebApplication(prefix);

		assertApplicationFromDefaultProjectIsRunning(prefix);
	}

	public void testServerBehaviourIsApplicationRunning() throws Exception {
		String prefix = "isApplicationRunning";
		createPerTestWebApplication(prefix);

		CloudFoundryApplicationModule appModule = assertApplicationFromDefaultProjectIsRunning(prefix);

		// Verify that the server behaviour API to determine that an app is
		// running tests correctly
		assertTrue(serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));

		// The following are the expected conditions for the server behaviour to
		// determine that the app is running
		String appName = getTestFixture().harness().getDefaultWebAppName(prefix);
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
		assertNotNull(serverBehavior.getApplicationStats(appName, new NullProgressMonitor()));
		assertNotNull(serverBehavior.getInstancesInfo(appName, new NullProgressMonitor()));
	}

	public void testStartModuleInvalidToken() throws Exception {
		String prefix = "startModuleInvalidToken";

		createPerTestWebApplication(prefix);
		try {
			connectClient("invalid", "invalidPassword");
		}
		catch (Exception e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		try {
			assertDeployAndStartApplication(prefix);
		}
		catch (CoreException ce) {
			assertNotNull(ce);
		}

		// Deploying application should have failed, so it must not exist in the
		// server
		String appName = getTestFixture().harness().getDefaultWebAppName(prefix);
		assertNull("Expecting no deployed application: " + appName + " but it exists in the server",
				getUpdatedApplication(appName));

		// Set the client again with the correct server-stored credentials
		connectClient();

		// Starting the app should now pass without errors
		assertDeployAndStartApplication(prefix);

	}

	public void testStartModuleInvalidPassword() throws Exception {

		String prefix = "startModuleInvalidPassword";

		createPerTestWebApplication(prefix);

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			String userName = cloudServer.getUsername();
			CloudCredentials credentials = new CloudCredentials(userName, "invalid-password");
			connectClient(credentials);

			assertDeployAndStartApplication(prefix);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		assertDeployAndStartApplication(prefix);

	}

	public void testDeleteModuleExternally() throws Exception {
		String prefix = "deleteModuleExternally";
		String appName = getTestFixture().harness().getDefaultWebAppName(prefix);
		createPerTestWebApplication(prefix);

		assertApplicationFromDefaultProjectIsRunning(prefix);

		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(appName)) {
				found = true;
				break;
			}
		}
		assertTrue(found);

		// Now create a separate external standalone client (external to the WST
		// CF Server instance) to delete the app
		URL url = new URL(getTestFixture().getUrl());
		CloudFoundryOperations client = CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(
				new CloudCredentials(getTestFixture().getCredentials().userEmail,
						getTestFixture().getCredentials().password), url);
		client.login();
		client.deleteApplication(appName);

		// Now check if the app is indeed deleted through the server behaviour
		// delegate
		serverBehavior.refreshModules(new NullProgressMonitor());
		applications = serverBehavior.getApplications(new NullProgressMonitor());
		found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(appName)) {
				found = true;
				break;
			}
		}
		assertFalse(found);
	}

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

}
