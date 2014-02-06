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
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.WaitWithProgressJob;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * 
 * Each individual test creates only ONE application module, and checks that
 * only one module exists in the server instance.
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
public class CloudFoundryServerBehaviourTest extends AbstractCloudFoundryTest {

	public void testConnect() throws Exception {
		serverBehavior.connect(null);

		final int[] serverState = new int[1];
		new WaitWithProgressJob(5, 2000) {

			@Override
			protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException {
				serverState[0] = server.getServerState();
				return serverState[0] == IServer.STATE_STARTED;
			}

			@Override
			protected boolean shouldRetryOnError(Throwable t) {
				return true;
			}

		}.run(new NullProgressMonitor());

		assertEquals(IServer.STATE_STARTED, serverState[0]);
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
	}

	public void testStartModule() throws Exception {
		assertCreateApplication(DYNAMIC_WEBAPP_NAME);

		// There should only be one module
		IModule[] modules = server.getModules();

		serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

		assertWebApplicationURL(modules, DYNAMIC_WEBAPP_NAME);
		assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);
	}

	public void testServerBehaviourIsApplicationRunning() throws Exception {
		assertCreateApplication(DYNAMIC_WEBAPP_NAME);
		IModule[] modules = server.getModules();

		serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

		assertWebApplicationURL(modules, DYNAMIC_WEBAPP_NAME);
		assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);

		// Verify that the Server Behaviour API to check that an app is
		// validates against
		// expected conditions

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(modules[0]);

		// Verify that the server behaviour API to determine that an app is
		// running tests correctly
		assertTrue(serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));

		// The following are the expected conditions for the server behaviour to
		// determine that the app is running
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
		assertNotNull(serverBehavior.getApplicationStats(DYNAMIC_WEBAPP_NAME, new NullProgressMonitor()));
		assertNotNull(serverBehavior.getInstancesInfo(DYNAMIC_WEBAPP_NAME, new NullProgressMonitor()));
	}

	public void testStartModuleInvalidToken() throws Exception {
		assertCreateApplication(DYNAMIC_WEBAPP_NAME);
		IModule[] modules = server.getModules();

		try {
			serverBehavior.resetClient();
			getClient("invalid", "invalidPassword");
		}
		catch (Exception e) {
			assertEquals("403 Error requesting access token.", e.getMessage());
		}

		try {
			serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());
		}
		catch (Throwable e) {
			assertEquals("Operation not permitted (403 Forbidden)", e.getMessage());
		}

		// Set the client again
		serverBehavior.resetClient();
		getClient();

		serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

		assertWebApplicationURL(modules, DYNAMIC_WEBAPP_NAME);
		assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);

	}

	public void testStartModuleInvalidPassword() throws Exception {

		assertCreateApplication(DYNAMIC_WEBAPP_NAME);
		IModule[] modules = server.getModules();

		try {
			serverBehavior.resetClient();
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			String userName = cloudServer.getUsername();
			CloudCredentials credentials = new CloudCredentials(userName, "invalid-password");
			getClient(credentials);

			serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertEquals("403 Error requesting access token.", e.getMessage());
		}

		serverBehavior.resetClient();
		getClient();

		serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

		assertWebApplicationURL(modules, DYNAMIC_WEBAPP_NAME);
		assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);

	}

	public void testDeleteModuleExternally() throws Exception {
		assertCreateApplication(DYNAMIC_WEBAPP_NAME);
		IModule[] modules = server.getModules();

		serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

		assertWebApplicationURL(modules, DYNAMIC_WEBAPP_NAME);
		assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);

		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(DYNAMIC_WEBAPP_NAME)) {
				found = true;
				break;
			}
		}
		assertTrue(found);
		URL url = new URL(CloudFoundryTestFixture.PIVOTAL_CF.getUrl());
		CloudFoundryOperations client = CloudFoundryPlugin.getCloudFoundryClientFactory()
				.getNonUAACloudFoundryOperations(CloudFoundryTestFixture.PIVOTAL_CF.getCredentials().userEmail,
						CloudFoundryTestFixture.PIVOTAL_CF.getCredentials().password, url);
		client.login();
		client.deleteApplication(DYNAMIC_WEBAPP_NAME);

		serverBehavior.refreshModules(new NullProgressMonitor());
		applications = serverBehavior.getApplications(new NullProgressMonitor());
		found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(DYNAMIC_WEBAPP_NAME)) {
				found = true;
				break;
			}
		}
		assertFalse(found);
	}

	public void testStartModuleWithDifferentId() throws Exception {
		harness = CloudFoundryTestFixture.current("dynamic-webapp-test").harness();
		server = harness.createServer();
		cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		serverBehavior = (CloudFoundryServerBehaviour) server.loadAdapter(CloudFoundryServerBehaviour.class, null);

		assertCreateApplication(DYNAMIC_WEBAPP_NAME);
		IModule[] modules = server.getModules();

		serverBehavior.startModuleWaitForDeployment(modules, new NullProgressMonitor());

		assertWebApplicationURL(modules, DYNAMIC_WEBAPP_NAME);
		assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);

		serverBehavior.refreshModules(new NullProgressMonitor());
		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals("dynamic-webapp-test")) {
				found = true;
				break;
			}
		}
		assertTrue(found);
	}

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

}
