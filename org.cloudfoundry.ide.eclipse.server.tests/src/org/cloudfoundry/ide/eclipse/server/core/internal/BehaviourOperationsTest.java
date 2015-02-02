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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.ModulesRefreshListener;
import org.cloudfoundry.ide.eclipse.server.tests.util.WaitForApplicationToStopOp;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.Server;

/**
 * Tests {@link ICloudFoundryOperation} in a target
 * {@link CloudFoundryServerBehaviour} obtained through
 * {@link CloudFoundryServerBehaviour#operations()} as well as refresh events
 * triggered by each of the operations.
 * <p/>
 * This may be a long running test suite as it involves multiple application
 * deployments as well as waiting for refresh operations to complete.
 *
 */
public class BehaviourOperationsTest extends AbstractAsynchCloudTest {

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

	public void testAsynchInstanceUpdate() throws Exception {
		// Test asynchronous Application instance update and that it triggers
		// a module refresh event
		String prefix = "testAsynchInstanceUpdate";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		assertEquals(1, appModule.getApplicationStats().getRecords().size());
		assertEquals(1, appModule.getInstanceCount());
		assertEquals(1, appModule.getInstancesInfo().getInstances().size());
		assertEquals(1, appModule.getDeploymentInfo().getInstances());

		asynchExecuteOperationWaitForRefresh(cloudServer.getBehaviour().operations().instancesUpdate(appModule, 2),
				prefix, CloudServerEvent.EVENT_APP_CHANGED);

		// Get updated module
		appModule = cloudServer.getExistingCloudModule(expectedAppName);

		assertEquals(2, appModule.getApplicationStats().getRecords().size());
		assertEquals(2, appModule.getInstanceCount());
		assertEquals(2, appModule.getInstancesInfo().getInstances().size());
		assertEquals(2, appModule.getDeploymentInfo().getInstances());

		CloudApplication actualApp = getUpdatedApplication(expectedAppName);

		assertEquals(2, actualApp.getInstances());
	}

	public void testAsynchMemoryUpdate() throws Exception {
		// Test asynchronous app memory update and that it triggers
		// a module refresh event
		String prefix = "testAsynchMemoryUpdate";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		final int changedMemory = 678;

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations().memoryUpdate(appModule, changedMemory), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

		// Get updated module
		appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Verify that the same module has been updated
		assertEquals(changedMemory, appModule.getDeploymentInfo().getMemory());
		assertEquals(changedMemory, appModule.getApplication().getMemory());

		assertEquals(changedMemory, appModule.getApplication().getMemory());
	}

	public void testAsynchEnvVarUpdate() throws Exception {
		// Test asynchronous app memory update and that it triggers
		// a module refresh event
		String prefix = "testAsynchEnvVarUpdate";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		EnvironmentVariable variable = new EnvironmentVariable();
		variable.setVariable("JAVA_OPTS");
		variable.setValue("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n");
		List<EnvironmentVariable> vars = new ArrayList<EnvironmentVariable>();
		vars.add(variable);
		DeploymentInfoWorkingCopy cp = appModule.resolveDeploymentInfoWorkingCopy(new NullProgressMonitor());
		cp.setEnvVariables(vars);
		cp.save();

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations().environmentVariablesUpdate(appModule), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

		// Get updated module
		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		Map<String, String> actualVars = getUpdatedApplication(expectedAppName).getEnvAsMap();
		assertEquals(vars.size(), actualVars.size());
		Map<String, String> expectedAsMap = new HashMap<String, String>();

		for (EnvironmentVariable v : vars) {
			String actualValue = actualVars.get(v.getVariable());
			assertEquals(v.getValue(), actualValue);
			expectedAsMap.put(v.getVariable(), v.getValue());
		}

		// Also verify that the env vars are set in deployment info
		assertEquals(vars.size(), appModule.getDeploymentInfo().getEnvVariables().size());

		List<EnvironmentVariable> deploymentInfoVars = appModule.getDeploymentInfo().getEnvVariables();

		for (EnvironmentVariable var : deploymentInfoVars) {
			String expectedValue = expectedAsMap.get(var.getVariable());
			assertEquals(var.getValue(), expectedValue);
		}
	}

	public void testAsynchAppURLUpdate() throws Exception {
		// Test asynchronous URL update of an application and that it triggers
		// a module refresh event
		String prefix = "testAsynchAppURLUpdate";
		final String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		String expectedURL = harness.getExpectedDefaultURL(prefix);
		assertEquals(expectedURL, appModule.getDeploymentInfo().getUris().get(0));

		String changedURL = harness.getExpectedDefaultURL("changedURtestCloudModuleRefreshURLUpdate");
		final List<String> expectedUrls = new ArrayList<String>();
		expectedUrls.add(changedURL);

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations().mappedUrlsUpdate(expectedAppName, expectedUrls), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

		// Get updated module
		appModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertEquals(expectedUrls, appModule.getDeploymentInfo().getUris());
		assertEquals(expectedUrls, appModule.getApplication().getUris());
	}

	public void testCloudModuleRefreshOnConnect() throws Exception {
		String appPrefix = "testCloudModuleRefreshOnConnect";
		createWebApplicationProject();
		assertDeployApplicationStartMode(appPrefix);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

		serverBehavior.disconnect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();

		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());

		ModulesRefreshListener listener = getModulesRefreshListener(null, cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		serverBehavior.connect(new NullProgressMonitor());

		assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_SERVER_REFRESHED);
	}

	public void testApplicationRemainsStartedAfterDisconnected() throws Exception {
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

		// Register a module refresh listener before connecting again to be
		// notified when
		// modules are refreshed
		ModulesRefreshListener listener = getModulesRefreshListener(null, cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		serverBehavior.connect(new NullProgressMonitor());

		assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_SERVER_REFRESHED);

		appModules = cloudServer.getExistingCloudModules();
		CloudFoundryApplicationModule appModule = appModules.iterator().next();

		assertEquals(expectedAppName, appModule.getDeployedApplicationName());

		assertApplicationIsRunning(appModule);
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

		ModulesRefreshListener listener = getModulesRefreshListener(null, cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		serverBehavior.connect(new NullProgressMonitor());

		assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_SERVER_REFRESHED);

		appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

	}

	public void testAsynchStopApplication() throws Exception {
		// Test asynchronous application stop and that it triggers
		// a module refresh event
		String prefix = "testAsynchStopApplication";

		createWebApplicationProject();
		// Deploy and start the app without the refresh listener
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.STOP),
				prefix, CloudServerEvent.EVENT_APP_CHANGED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		boolean stopped = new WaitForApplicationToStopOp(cloudServer, appModule).run(new NullProgressMonitor());
		assertTrue("Expected application to be stopped", stopped);
		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

	}

	public void testAsynchStartApplication() throws Exception {
		// Test asynchronous application stop and that it triggers
		// a module refresh event
		String prefix = "testAsynchStartApplication";

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployApplication(prefix, true);

		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.START),
				prefix, CloudServerEvent.EVENT_APP_CHANGED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertApplicationIsRunning(appModule.getLocalModule(), prefix);
		assertTrue("Expected application to be started", appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);

	}

	public void testAsychRestartApplication() throws Exception {

		String prefix = "testAsychRestartApplication";

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployApplication(prefix, true);

		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.RESTART),
				prefix, CloudServerEvent.EVENT_APP_CHANGED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertApplicationIsRunning(appModule.getLocalModule(), prefix);

		assertTrue("Expected application to be started", appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);

	}

	public void testAsynchUpdateRestartApplication() throws Exception {

		String prefix = "testAsynchUpdateRestartApplication";

		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployApplication(prefix, true);

		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

		asynchExecuteOperationWaitForRefresh(
				cloudServer.getBehaviour().operations()
						.applicationDeployment(appModule, ApplicationAction.UPDATE_RESTART), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertTrue("Expected application to be started", appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);

	}

	public void testAsynchPushApplicationStopMode() throws Exception {

		String prefix = "testAsynchPushApplicationStopMode";
		String expectedAppName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();
		getTestFixture().configureForApplicationDeployment(expectedAppName, CloudUtil.DEFAULT_MEMORY, true);

		IModule module = getModule(project.getName());

		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);
	}

	public void testAsynchPushApplicationStartMode() throws Exception {

		String prefix = "testAsynchPushApplicationStartMode";

		String expectedAppName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();
		getTestFixture().configureForApplicationDeployment(expectedAppName, CloudUtil.DEFAULT_MEMORY, false);

		IModule module = getModule(project.getName());

		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

		assertApplicationIsRunning(appModule.getLocalModule(), prefix);

		assertTrue("Expected application to be started", appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);
	}

}
