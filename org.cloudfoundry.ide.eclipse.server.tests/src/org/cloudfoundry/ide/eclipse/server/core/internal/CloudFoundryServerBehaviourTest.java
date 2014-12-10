/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License,
 * Version 2.0 (the "License�); you may not use this file except in compliance
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
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
		assertEquals(Collections.emptyList(), cloudServer.getExistingCloudModules());
	}

	public void testCloudFoundryModuleCreationNonWSTPublish() throws Exception {
		// Test that a cloud foundry module is created when an application is
		// pushed
		// using framework API rather than test harness API.

		String appPrefix = "testCloudFoundryModuleCreationNonWSTPublish";
		createWebApplicationProject();

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		String projectName = harness.getDefaultWebAppProjectName();
		String expectedAppName = harness.getDefaultWebAppName(appPrefix);
		getTestFixture().configureForApplicationDeployment(expectedAppName, false);

		IModule module = getModule(projectName);

		assertNotNull("Expected non-null IModule when deploying application", module);

		// Publish with non-WST publish API (i.e. publish that is not invoked by
		// WST framework)
		serverBehavior.publishAdd(module.getName(), new NullProgressMonitor());

		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertNotNull("Expected list of cloud modules after deploying: " + appPrefix, appModules);
		assertTrue("Expected one application module for " + appPrefix + " but got: " + appModules.size(),
				appModules.size() == 1);

		CloudFoundryApplicationModule applicationModule = appModules.iterator().next();
		assertEquals(expectedAppName, applicationModule.getDeployedApplicationName());

	}

	public void testCloudFoundryModuleCreationWSTPublish() throws Exception {
		// Test that a cloud foundry module is created when an application is
		// pushed
		// using framework API rather than test harness API.

		String appPrefix = "testCloudFoundryModuleCreationWSTPublish";
		createWebApplicationProject();

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		String projectName = harness.getDefaultWebAppProjectName();
		String expectedAppName = harness.getDefaultWebAppName(appPrefix);
		getTestFixture().configureForApplicationDeployment(expectedAppName, false);

		IModule module = getModule(projectName);

		assertNotNull("Expected non-null IModule when deploying application", module);

		// Publish through WST publish method
		serverBehavior.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());

		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertNotNull("Expected list of cloud modules after deploying: " + appPrefix, appModules);
		assertTrue("Expected one application module for " + appPrefix + " but got: " + appModules.size(),
				appModules.size() == 1);

		CloudFoundryApplicationModule applicationModule = appModules.iterator().next();
		assertEquals(expectedAppName, applicationModule.getDeployedApplicationName());

	}

	public void testCreateDeployAppHelpersStartMode() throws Exception {
		// Tests whether the helper method to deploy and start an app matches
		// expected
		// app state.
		String prefix = "testCreateDeployAppHelpers";
		createWebApplicationProject();

		// Invoke the helper method
		assertDeployApplicationStartMode(prefix);

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

		assertFalse("Expected application to be stopped, but server behaviour indicated it is running",
				serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));

	}

	public void testStopApplication() throws Exception {
		String prefix = "stopApplication";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

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
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

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

		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

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
			assertDeployApplicationStartMode(prefix);
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
		assertDeployApplicationStartMode(prefix);

	}

	public void testStartModuleInvalidPassword() throws Exception {

		String prefix = "startModuleInvalidPassword";

		createWebApplicationProject();

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			String userName = cloudServer.getUsername();
			CloudCredentials credentials = new CloudCredentials(userName, "invalid-password");
			connectClient(credentials);

			assertDeployApplicationStartMode(prefix);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		// Should now deploy without errors
		assertDeployApplicationStartMode(prefix);

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

		assertDeployApplicationStartMode(prefix);

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

		CloudFoundryOperations client = harness.createExternalClient();

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

	public void testCloudModulesClearedOnDisconnect() throws Exception {
		// Test the following:
		// Create an application and deploy it.
		// Then disconnect and verify that the app module cache is cleared.

		String appPrefix = "testCloudModulesClearedOnDisconnect";
		createWebApplicationProject();
		assertDeployApplicationStartMode(appPrefix);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

		serverBehavior.disconnect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();

		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());
	}

	public void testCloudModulesCreatedForExistingApps() throws Exception {
		// Test the following:
		// Create an application and deploy it.
		// Disconnect (which clears cloud module cache).
		// Re-connect again and verify that cloud modules are created for the
		// deployed
		// app.

		String appPrefix = "testCloudModulesCreatedForExistingApps";
		createWebApplicationProject();
		assertDeployApplicationStartMode(appPrefix);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

		serverBehavior.disconnect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();

		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());

		serverBehavior.connect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

	}

	public void testApplicationRemainsStartedAfterDisconnectedBehaviour() throws Exception {
		// Deploy and start an application.
		// Disconnect through the server behaviour. Verify through an external
		// client that the app
		// remains deployed and in started mode.
		// Reconnect, and verify that the application is still running (i.e.
		// disconnecting
		// the server should not stop the application).

		String appPrefix = "testApplicationRemainsStartedAfterDisconnected";
		String expectedAppName = harness.getDefaultWebAppName(appPrefix);

		createWebApplicationProject();
		assertDeployApplicationStartMode(appPrefix);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

		// Disconnect and verify that there are no cloud foundry application
		// modules
		serverBehavior.disconnect(new NullProgressMonitor());
		appModules = cloudServer.getExistingCloudModules();
		assertTrue("Expected empty list of cloud application modules after server disconnect but got list with size: "
				+ appModules.size(), appModules.isEmpty());

		// Now create an external client to independently check that the
		// application remains deployed and in started mode

		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		List<CloudApplication> deployedApplications = client.getApplications();
		assertTrue("Expected one cloud application for " + appPrefix + " but got: " + deployedApplications.size(),
				deployedApplications.size() == 1);
		assertEquals(expectedAppName, deployedApplications.get(0).getName());
		assertTrue(deployedApplications.get(0).getState() == AppState.STARTED);

		// Re-connect through the server behaviour. It should re-create a cloud
		// application module for the deployed
		// application
		serverBehavior.connect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();
		CloudFoundryApplicationModule appModule = appModules.iterator().next();

		assertEquals(expectedAppName, appModule.getDeployedApplicationName());

		assertApplicationIsRunning(appModule);
	}

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

}
