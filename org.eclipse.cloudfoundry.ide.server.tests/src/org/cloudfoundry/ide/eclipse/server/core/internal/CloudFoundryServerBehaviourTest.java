/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc.
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
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

	public void testApplicationDeploymentInfo() throws Exception {

		String prefix = "testApplicationDeploymentInfo";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		CloudService service = getCloudServiceToCreate("sqlService", "elephantsql", "turtle");
		List<CloudService> servicesToBind = new ArrayList<CloudService>();
		servicesToBind.add(service);
		client.createService(service);

		EnvironmentVariable variable = new EnvironmentVariable();
		variable.setVariable("JAVA_OPTS");
		variable.setValue("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n");
		List<EnvironmentVariable> vars = new ArrayList<EnvironmentVariable>();
		vars.add(variable);

		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployApplication(prefix, CloudUtil.DEFAULT_MEMORY, false, vars,
				servicesToBind);
		waitForApplicationToStart(appModule.getLocalModule(), prefix);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertNotNull(appModule.getApplication());
		assertNotNull(appModule.getDeploymentInfo());

		CloudApplication actualApp = appModule.getApplication();

		assertEquals(appModule.getDeployedApplicationName(), actualApp.getName());
		assertEquals(expectedAppName, actualApp.getName());

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		assertEquals(expectedAppName, info.getDeploymentName());

		// Verify that both the deployment info and the actual Cloud application
		// contain
		// the same information
		assertEquals(actualApp.getInstances(), info.getInstances());
		assertEquals(1, info.getInstances());

		String expectedUrl = harness.getExpectedDefaultURL(prefix);
		assertEquals(actualApp.getUris().get(0), info.getUris().get(0));
		assertEquals(expectedUrl, info.getUris().get(0));

		assertEquals(actualApp.getMemory(), info.getMemory());
		assertEquals(CloudUtil.DEFAULT_MEMORY, info.getMemory());

		assertEquals("JAVA_OPTS", appModule.getDeploymentInfo().getEnvVariables().get(0).getVariable());
		assertEquals("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n", appModule
				.getDeploymentInfo().getEnvVariables().get(0).getValue());

		assertTrue(actualApp.getEnvAsMap().containsKey("JAVA_OPTS"));
		assertEquals("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n", actualApp.getEnvAsMap()
				.get("JAVA_OPTS"));
		assertEquals("sqlService", appModule.getDeploymentInfo().getServices().get(0).getName());
		assertEquals("sqlService", actualApp.getServices().get(0));

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
		getTestFixture().configureForApplicationDeployment(expectedAppName, CloudUtil.DEFAULT_MEMORY, false);

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
		getTestFixture().configureForApplicationDeployment(expectedAppName, CloudUtil.DEFAULT_MEMORY, false);

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
		String prefix = "testCreateDeployAppHelpersStartMode";
		createWebApplicationProject();

		// Invoke the helper method
		deployAndWaitForAppStart(prefix);

		// Verify it is deployed
		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(prefix, IServer.STATE_STARTED);
		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		IModule module = getModule(harness.getDefaultWebAppProjectName());

		// Verify the app is running
		waitForApplicationToStart(module, prefix);

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
		String prefix = "testCreateDeployAppHelpersStopMode";
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

	public void testWSTBehaviourStopModule() throws Exception {

		// Tests WST overridden method in Cloud behaviour to stop modules
		String prefix = "testWSTBehaviourStopModule";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		serverBehavior.stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		waitForAppToStop(appModule);

		// Check the helper method assertions have correct values
		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == IServer.STATE_STOPPED);
	}

	public void testWSTBehaviourStartStop() throws Exception {
		// Tests WST overridden methods in the Cloud behaviour
		String prefix = "testWSTBehaviourStartStop";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		serverBehavior.stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		waitForAppToStop(appModule);

		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == IServer.STATE_STOPPED);

		serverBehavior.startModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		assertApplicationIsRunning(appModule);

		assertEquals(IServer.STATE_STARTED, appModule.getState());
		assertEquals(AppState.STARTED, appModule.getApplication().getState());
	}

	public void testServerBehaviourIsApplicationRunning() throws Exception {
		// Tests the server behaviour API that checks if the application is
		// running
		String prefix = "testServerBehaviourIsApplicationRunning";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		// Verify that the server behaviour API to determine that an app is
		// running tests correctly
		assertTrue(serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));

		// The following are the expected conditions for the server behaviour to
		// determine that the app is running
		String appName = harness.getDefaultWebAppName(prefix);
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
		assertEquals(1, serverBehavior.getApplicationStats(appName, new NullProgressMonitor()).getRecords().size());
		assertEquals(1, serverBehavior.getInstancesInfo(appName, new NullProgressMonitor()).getInstances().size());
	}

	public void testStartModuleInvalidPassword() throws Exception {

		String prefix = "testStartModuleInvalidPassword";

		createWebApplicationProject();

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			String userName = cloudServer.getUsername();
			CloudCredentials credentials = new CloudCredentials(userName, "invalid-password");
			connectClient(credentials);

			deployAndWaitForAppStart(prefix);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		// Should now deploy without errors
		deployAndWaitForAppStart(prefix);

	}

	public void testStartModuleInvalidUsername() throws Exception {

		String prefix = "startModuleInvalidUsername";

		createWebApplicationProject();

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			CloudCredentials credentials = new CloudCredentials("invalidusername", cloudServer.getPassword());
			connectClient(credentials);

			deployAndWaitForAppStart(prefix);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		// Should now deploy without errors
		deployAndWaitForAppStart(prefix);

	}

	/*
	 * Creates an application through the Cloud Foundry server instance, and
	 * then deletes the application using an external standalone client not
	 * associated with the Cloud Foundry server instance to simulate deleting
	 * the application outside of Eclipse or the runtime workbench.
	 */
	public void testDeleteModuleExternally() throws Exception {

		String prefix = "testDeleteModuleExternally";
		String appName = harness.getDefaultWebAppName(prefix);
		createWebApplicationProject();

		deployAndWaitForAppStart(prefix);

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
		deployAndWaitForAppStart(appPrefix);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

		serverBehavior.disconnect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();

		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());
	}

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}
}
