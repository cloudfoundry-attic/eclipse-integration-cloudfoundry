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
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;
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

	public void testCreateDeployAppHelpersStartMode() throws Exception {
		// Tests whether the helper method to deploy and start an app matches
		// expected
		// app state.
		String prefix = "testCreateDeployAppHelpers";
		createWebApplicationProject();

		// Invoke the helper method
		deployApplicationStartMode(prefix);

		// Verify it is deployed
		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(prefix, IServer.STATE_STARTED);
		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		IModule module = getModule(harness.getDefaultWebAppProjectName());

		// Verify the app is running
		assertApplicationIsRunning(module, prefix);

		// Now CHECK that the expected conditions in the helper method assert to
		// expected values
		appModule = assertCloudFoundryModuleExists(module, prefix);

		assertNotNull(
				"No Cloud Application mapping in Cloud module. Application mapping failed or application did not deploy",
				appModule.getApplication());

		assertEquals(IServer.STATE_STARTED, appModule.getState());
		assertEquals(AppState.STARTED, appModule.getApplication().getState());

		// Check the module state in the WST server is correct
		int moduleState = server.getModuleState(new IModule[] { module });
		assertEquals(IServer.STATE_STARTED, moduleState);

	}

	public void testCreateDeployAppHelpersStopMode() throws Exception {
		String prefix = "testCreateDeployAppHelpers";
		createWebApplicationProject();

		boolean stopMode = true;

		deployApplication(prefix, stopMode);

		// Invoke the helper method
		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(prefix, IServer.STATE_STOPPED);
		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		// Now CHECK that the expected conditions in the helper method assert to
		// expected values
		IModule module = getModule(harness.getDefaultWebAppProjectName());

		appModule = assertCloudFoundryModuleExists(module, prefix);

		assertNotNull(
				"No Cloud Application mapping in Cloud module. Application mapping failed or application did not deploy",
				appModule.getApplication());

		assertEquals(IServer.STATE_STOPPED, appModule.getState());
		assertEquals(AppState.STOPPED, appModule.getApplication().getState());

		// Check the module state in the WST server is correct
		int moduleState = server.getModuleState(new IModule[] { module });
		assertEquals(IServer.STATE_STOPPED, moduleState);

		Exception error = null;
		try {
			assertApplicationIsRunning(module, prefix);
		}
		catch (Exception e) {
			error = e;
		}

		assertNotNull("Expected error when checking if application is running", error);

	}

	public void testStopApplication() throws Exception {
		String prefix = "stopApplication";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployApplicationStartMode(prefix);

		assertStopModule(appModule);

		// Check the helper method assertions have correct values
		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == IServer.STATE_STOPPED);
	}

	public void testStartStopAfterDeployment() throws Exception {
		// Tests that after an application has been deployed and started, if
		// stopped and restarted, the application starts without problems
		String prefix = "startStopApplication";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployApplicationStartMode(prefix);

		assertStopModule(appModule);

		assertStartModule(appModule);
		
		assertEquals(IServer.STATE_STARTED, appModule.getState());
		assertEquals(AppState.STARTED, appModule.getApplication().getState());
	}

	public void testDeployAppStopModeThenStart() throws Exception {
		// Tests that an app is deployed in stop mode. Then, after deployment,
		// it is started.
		String prefix = "stopModeThenStartApp";
		createWebApplicationProject();
		deployApplication(prefix, true);

		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(prefix, IServer.STATE_STOPPED);

		assertStartModule(appModule);
	}

	public void testServerBehaviourIsApplicationRunning() throws Exception {
		// Tests the server behaviour API that checks if the application is
		// running
		String prefix = "isApplicationRunning";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployApplicationStartMode(prefix);

		// Verify that the server behaviour API to determine that an app is
		// running tests correctly
		assertTrue(serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));

		// The following are the expected conditions for the server behaviour to
		// determine that the app is running
		String appName = harness.getDefaultWebAppName(prefix);
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
		assertNotNull(serverBehavior.getApplicationStats(appName, new NullProgressMonitor()));
		assertNotNull(serverBehavior.getInstancesInfo(appName, new NullProgressMonitor()));
	}

	public void testStartModuleInvalidToken() throws Exception {
		String prefix = "startModuleInvalidToken";

		createWebApplicationProject();
		try {
			connectClient("invalid", "invalidPassword");
		}
		catch (Exception e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		try {
			deployApplicationStartMode(prefix);
		}
		catch (CoreException ce) {
			assertNotNull(ce);
		}

		// Deploying application should have failed, so it must not exist in the
		// server
		String appName = harness.getDefaultWebAppName(prefix);
		assertNull("Expecting no deployed application: " + appName + " but it exists in the server",
				getUpdatedApplication(appName));

		// Set the client again with the correct server-stored credentials
		connectClient();

		// Starting the app should now pass without errors
		deployApplicationStartMode(prefix);

	}

	public void testStartModuleInvalidPassword() throws Exception {

		String prefix = "startModuleInvalidPassword";

		createWebApplicationProject();

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			String userName = cloudServer.getUsername();
			CloudCredentials credentials = new CloudCredentials(userName, "invalid-password");
			connectClient(credentials);

			deployApplicationStartMode(prefix);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		// Should now deploy without errors
		deployApplicationStartMode(prefix);

		assertApplicationIsDeployed(prefix, IServer.STATE_STARTED);

	}

	/*
	 * Creates an application through the Cloud Foundry server instance, and
	 * then deletes the application using an external standalone client not
	 * associated with the Cloud Foundry server instance to simulate deleting
	 * the application outside of Eclipse or the runtime workbench.
	 */
	public void testDeleteModuleExternally() throws Exception {

		String prefix = "deleteModuleExternally";
		String appName = harness.getDefaultWebAppName(prefix);
		createWebApplicationProject();

		deployApplicationStartMode(prefix);

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
						getTestFixture().getCredentials().password), url, false);
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
