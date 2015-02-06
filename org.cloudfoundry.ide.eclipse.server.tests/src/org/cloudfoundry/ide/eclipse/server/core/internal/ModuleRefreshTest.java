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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.wst.server.core.IModule;

/**
 * Test Module refresh scenarios, including deleting an application externally
 * (outside of the Eclipse tools), as well as updating existing applications
 * externally (e.g. scaling memory), and making sure that the module in the
 * tools is updated accordingly when invoking update modules API on the
 * {@link CloudFoundryServer} and {@link CloudFoundryServerBehaviour}
 *
 */
public class ModuleRefreshTest extends AbstractRefreshCloudTest {

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

	public void testUpdateModulesServerBehaviourExistingCloudApp() throws Exception {
		// Update modules API in behaviour will return a
		// CloudFoundryApplicationModule for an existing Cloud application in
		// the Cloud Space. This associated update modules for the Cloud Foundry
		// Server
		// which the behaviour uses is tested separately in a different test
		// case

		String prefix = "testUpdateModulesServerBehaviourExistingCloudApp";
		String expectedAppName = harness.getDefaultWebAppName(prefix);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(expectedAppName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls,
				new ArrayList<String>());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Tooling has not yet been updated so there is no corresponding
		// appModule even though the app exists in the Cloud space
		assertNull(appModule);

		// This will tell the behaviour to fetch the Cloud application from the
		// Cloud space and generate a module
		CloudFoundryApplicationModule updateModule = serverBehavior.updateCloudModule(expectedAppName,
				new NullProgressMonitor());
		assertEquals(expectedAppName, updateModule.getDeployedApplicationName());
		assertEquals(updateModule.getDeployedApplicationName(), updateModule.getApplication().getName());

		// Check the mapping is correct
		assertEquals(updateModule.getName(), updateModule.getApplication().getName());
		assertEquals(CloudUtil.DEFAULT_MEMORY, updateModule.getApplication().getMemory());
		assertEquals(updateModule.getDeploymentInfo().getMemory(), updateModule.getApplication().getMemory());
	}

	public void testUpdateModuleInstances() throws Exception {
		// Update modules API in behaviour will return a
		// CloudFoundryApplicationModule for an existing Cloud application in
		// the Cloud Space. This associated update modules for the Cloud Foundry
		// Server
		// which the behaviour uses is tested separately in a different test
		// case

		String prefix = "testUpdateModuleInstances";
		String expectedAppName = harness.getDefaultWebAppName(prefix);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(expectedAppName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls,
				new ArrayList<String>());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Tooling has not yet been updated so there is no corresponding
		// appModule even though the app exists in the Cloud space
		assertNull(appModule);

		// This will tell the behaviour to fetch the Cloud application from the
		// Cloud space and generate a module
		CloudFoundryApplicationModule updateModule = serverBehavior.updateCloudModuleWithInstances(expectedAppName,
				new NullProgressMonitor());
		assertEquals(expectedAppName, updateModule.getDeployedApplicationName());
		assertEquals(updateModule.getDeployedApplicationName(), updateModule.getApplication().getName());

		// Check the mapping is correct
		assertEquals(updateModule.getName(), updateModule.getApplication().getName());
		assertEquals(CloudUtil.DEFAULT_MEMORY, updateModule.getApplication().getMemory());
		assertEquals(updateModule.getDeploymentInfo().getMemory(), updateModule.getApplication().getMemory());
		assertEquals(1, updateModule.getInstanceCount());

		// There is one instance, but since the app was created EXTERNALLY and
		// not started, there should
		// be no instance info
		assertEquals(0, updateModule.getApplicationStats().getRecords().size());
		assertNull(updateModule.getInstancesInfo());

		updateModule = serverBehavior.updateCloudModuleWithInstances((String) null, new NullProgressMonitor());
		assertNull(updateModule);

		updateModule = serverBehavior.updateCloudModuleWithInstances("wrongName", new NullProgressMonitor());
		assertNull(updateModule);

		updateModule = serverBehavior.updateCloudModuleWithInstances((IModule) null, new NullProgressMonitor());
		assertNull(updateModule);

	}

	public void testUpdateModulesServerBehaviourWrongCloudApp() throws Exception {
		// Update modules API in behaviour will return a
		// CloudFoundryApplicationModule for an existing Cloud application in
		// the Cloud Space. This associated update modules for the Cloud Foundry
		// Server
		// which the behaviour uses is tested separately in a different test
		// case
		String prefix = "testUpdateModulesServerBehaviourWrongCloudApp";
		String expectedAppName = harness.getDefaultWebAppName(prefix);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(expectedAppName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls,
				new ArrayList<String>());

		// The tool has not refreshed after the app was created with an external
		// client, so there should be no module
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertNull(appModule);

		CloudFoundryApplicationModule wrongModule = serverBehavior.updateCloudModule("wrongApp",
				new NullProgressMonitor());
		assertNull(wrongModule);

		wrongModule = serverBehavior.updateCloudModuleWithInstances("wrongApp", new NullProgressMonitor());
		assertNull(wrongModule);
	}

	public void testUpdateModulesCloudServer() throws Exception {

		// Tests the Update modules API in the server that will CREATE or return
		// an existing
		// CloudFoundryApplicationModule ONLY if it is given a CloudApplication.

		String prefix = "testUpdateModulesCloudServer";
		String expectedAppName = harness.getDefaultWebAppName(prefix);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(expectedAppName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls,
				new ArrayList<String>());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Tooling has not yet been updated so there is no corresponding
		// appModule even though the app exists in the Cloud space
		assertNull(appModule);

		// No actual cloud application passed to update therefore no associated
		// CloudFoundryApplicationModule should be found
		appModule = cloudServer.updateModule(null, expectedAppName, new NullProgressMonitor());
		assertNull(appModule);

		appModule = cloudServer.updateModule(null, null, new NullProgressMonitor());
		assertNull(appModule);

		assertTrue(cloudServer.getExistingCloudModules().isEmpty());

		// Get the actual cloud app directly from the Cloud space
		CloudApplication actualApp = client.getApplications().get(0);

		// Now create the CloudFoundryApplicationModule
		appModule = cloudServer.updateModule(actualApp, expectedAppName, new NullProgressMonitor());

		assertEquals(expectedAppName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Check the mapping is correct
		assertEquals(actualApp.getName(), appModule.getApplication().getName());

		assertEquals(CloudUtil.DEFAULT_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());

		// It should match what is obtained through getExisting API
		CloudFoundryApplicationModule existingCloudMod = cloudServer.getExistingCloudModule(expectedAppName);

		assertEquals(expectedAppName, existingCloudMod.getDeployedApplicationName());
		assertEquals(existingCloudMod.getDeployedApplicationName(), existingCloudMod.getApplication().getName());

		// Check the mapping is correct
		assertEquals(actualApp.getName(), existingCloudMod.getApplication().getName());

		assertEquals(CloudUtil.DEFAULT_MEMORY, existingCloudMod.getApplication().getMemory());
		assertEquals(existingCloudMod.getDeploymentInfo().getMemory(), existingCloudMod.getApplication().getMemory());

		// Check the other existing Modules API
		CloudFoundryApplicationModule sameExistingApp = cloudServer.getExistingCloudModules().iterator().next();
		assertEquals(expectedAppName, sameExistingApp.getDeployedApplicationName());
		assertEquals(sameExistingApp.getDeployedApplicationName(), sameExistingApp.getApplication().getName());

		// Check the mapping is correct
		assertEquals(actualApp.getName(), sameExistingApp.getApplication().getName());

		assertEquals(CloudUtil.DEFAULT_MEMORY, sameExistingApp.getApplication().getMemory());
		assertEquals(sameExistingApp.getDeploymentInfo().getMemory(), sameExistingApp.getApplication().getMemory());

	}

	public void testModuleRefreshAllOp() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh listener
		String prefix = "testModuleRefreshAllOp";
		createWebApplicationProject();

		deployAndWaitForDeploymentEvent(prefix);

		asynchExecuteOperationWaitForRefresh(cloudServer.getBehaviour().operations().refreshAll(null), prefix,
				CloudServerEvent.EVENT_SERVER_REFRESHED);
	}

	public void testModuleUpdatesExternalChanges() throws Exception {

		// Tests various module update scenarios due to external changes.
		// This is performed in one test to avoid multiple application creations
		// and deployments during junit setups which are slow
		// Tests the following cases:
		// 1. Push application for the first time - Module should be created and
		// mapped to a CloudApplication
		// 2. Update the application externally and update through behaviour API
		// - Module and mapping to CloudApplication should be updated
		// 3. Update the application externally and refresh all Modules - Module
		// and mapping to CloudApplication should be updated.

		String prefix = "testModuleUpdatesExternalChanges";
		String appName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudUtil.DEFAULT_MEMORY, stopMode);

		IModule module = getModule(project.getName());

		// Push the application
		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		assertNotNull(appModule);
		// Test that the mapping to the actual application in the Cloud space is
		// present. Since a
		// CloudApplication is not created by the underlying client unless it
		// exists, this also
		// indirectly tests that the CloudApplication was successfully created
		// indicating the application
		// exists in the Cloud space.
		assertNotNull(appModule.getApplication());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// To test update on external changes, verify the current memory
		assertEquals(CloudUtil.DEFAULT_MEMORY, appModule.getDeploymentInfo().getMemory());

		// Verify that the CloudApplication in the Cloud space exists through
		// the list of all CloudApplications
		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(appName)) {
				found = true;
				break;
			}
		}
		assertTrue("Expected CloudApplication for " + appName + " to exist in the Cloud space", found);

		// Now modify the application externally and verify that when performing
		// a module update
		// that the new changes are picked up by the tooling

		// Create separate external client
		CloudFoundryOperations externalClient = harness.createExternalClient();
		externalClient.login();

		// Refresh Module through behaviour to check if it picks up changes

		// 1. Test via single-module update
		externalClient.updateApplicationMemory(appName, 737);
		CloudApplication updatedCloudApplicationFromClient = externalClient.getApplication(appName);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(737, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

		// 2. Test via single-module update and it's instances
		externalClient.updateApplicationMemory(appName, 555);

		updatedCloudApplicationFromClient = externalClient.getApplication(appName);
		appModule = serverBehavior.updateCloudModuleWithInstances(appName, new NullProgressMonitor());

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());

		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(555, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

		// 3. Test via module refresh of all modules
		externalClient.updateApplicationMemory(appName, 345);
		updatedCloudApplicationFromClient = externalClient.getApplication(appName);
		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		allApps.put(updatedCloudApplicationFromClient.getName(), updatedCloudApplicationFromClient);

		cloudServer.updateModules(allApps);

		appModule = cloudServer.getExistingCloudModule(appName);

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());

		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(345, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

	}

	public void testSingleModuleUpdateExternalAppDeletion() throws Exception {

		String prefix = "testSingleModuleUpdateExternalAppDeletion";
		String appName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudUtil.DEFAULT_MEMORY, stopMode);

		IModule module = getModule(project.getName());

		// Push the application.
		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Delete module externally and verify that module refresh picks up the
		// change

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		client.deleteApplication(appName);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule(appName);

		assertNull(appModule);

		CloudApplication nonexistantApp = null;
		appModule = cloudServer.updateModule(nonexistantApp, appName, new NullProgressMonitor());
		assertNull(appModule);

	}

	public void testSingleModuleUpdateNonExistantApp() throws Exception {

		String prefix = "testSingleModuleUpdateNonExistantApp";
		String appName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudUtil.DEFAULT_MEMORY, stopMode);

		IModule module = getModule(project.getName());

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule((String) null);

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule((IModule) null);

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule(module);

		assertNull(appModule);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertNull(appModule);

		CloudApplication nonexistantApp = null;
		appModule = cloudServer.updateModule(nonexistantApp, appName, new NullProgressMonitor());
		assertNull(appModule);

		appModule = cloudServer.updateModule(null, null, new NullProgressMonitor());
		assertNull(appModule);

	}

	public void testAllModuleUpdateExternalAppDeletion() throws Exception {

		String prefix = "testAllModuleUpdateExternalAppDeletion";
		String appName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudUtil.DEFAULT_MEMORY, stopMode);

		IModule module = getModule(project.getName());

		// Push the application.
		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		// Verify the module exists
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Delete module externally and verify that module refresh picks up the
		// change

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		client.deleteApplication(appName);

		// Update through all-modules update, and also verify existing modules
		// matches results
		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		cloudServer.updateModules(allApps);

		appModule = cloudServer.getExistingCloudModule(appName);
		assertNull(appModule);

		// Update through single-module update, and also verify that existing
		// modules matches the results
		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());
		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule(appName);
		assertNull(appModule);
	}

	public void testSingleModuleUpdateExternalCreation() throws Exception {

		String prefix = "testSingleModuleUpdateExternalCreation";
		String appName = harness.getDefaultWebAppName(prefix);

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);
		assertNull(appModule);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());
		assertNull(appModule);

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(appName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls, new ArrayList<String>());

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());
		assertEquals(CloudUtil.DEFAULT_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());
	}

	public void testAllModuleUpdateExternalCreation() throws Exception {

		String prefix = "testAllModuleUpdateExternalCreation";
		String appName = harness.getDefaultWebAppName(prefix);

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);
		assertNull(appModule);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());
		assertNull(appModule);

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(appName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls, new ArrayList<String>());

		CloudApplication application = client.getApplication(appName);
		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		allApps.put(application.getName(), application);
		cloudServer.updateModules(allApps);

		appModule = cloudServer.getExistingCloudModule(appName);

		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());
		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(CloudUtil.DEFAULT_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());

		// It should match what is obtained through update cloud module
		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());
		assertNotNull(appModule);
		assertNotNull(appModule.getApplication());
		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(CloudUtil.DEFAULT_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());
	}

	public void testScheduleRefreshHandlerAllModules() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh listener
		String prefix = "testScheduleRefreshHandlerAllModules";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForDeploymentEvent(prefix);

		// Test the server-wide refresh of all modules without specifying a
		// selected module.
		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);
		cloudServer.getBehaviour().getRefreshHandler().scheduleRefreshAll();

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_SERVER_REFRESHED);

		refreshListener = ModulesRefreshListener
				.getListener(null, cloudServer, CloudServerEvent.EVENT_SERVER_REFRESHED);

		cloudServer.getBehaviour().getRefreshHandler().scheduleRefreshAll(null);

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_SERVER_REFRESHED);

		refreshListener = ModulesRefreshListener
				.getListener(null, cloudServer, CloudServerEvent.EVENT_SERVER_REFRESHED);

		cloudServer.getBehaviour().getRefreshHandler().scheduleRefreshAll(appModule.getLocalModule());

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_SERVER_REFRESHED);
	}

	public void testScheduleRefreshHandlerRefreshApplication() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh listener
		String prefix = "testScheduleRefreshHandlerRefreshApplication";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForDeploymentEvent(prefix);

		// Test the server-wide refresh of all modules therefore do not pass the
		// app name
		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(
				appModule.getDeployedApplicationName(), cloudServer, CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		cloudServer.getBehaviour().getRefreshHandler().schedulesRefreshApplication(appModule.getLocalModule());

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_APPLICATION_REFRESHED);
	}

	public void testScheduleRefreshHandlerAllModuleInstances() throws Exception {

		String prefix = "testScheduleRefreshHandlerAllModuleInstances";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForDeploymentEvent(prefix);

		// Test the server-wide refresh of all modules including refreshing
		// instances of a selected module
		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		cloudServer.getBehaviour().getRefreshHandler().scheduleRefreshAll(appModule.getLocalModule());

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_SERVER_REFRESHED);
	}

	public void testScheduleRefreshHandlerDeploymentChange() throws Exception {

		String prefix = "testScheduleRefreshHandlerDeploymentChange";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		// Test the server-wide refresh of all modules including refreshing
		// instances of a selected module
		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED);

		cloudServer.getBehaviour().getRefreshHandler().scheduleRefreshForDeploymentChange(appModule.getLocalModule());

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED);
	}

	public void testFireEventAppChanged() throws Exception {
		// Tests the general event handler

		String prefix = "testFireEventAppChanged";
		createWebApplicationProject();

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		deployAndWaitForDeploymentEvent(prefix);

		final IModule module = cloudServer.getExistingCloudModule(expectedAppName).getLocalModule();

		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(expectedAppName, cloudServer,
				CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				ServerEventHandler.getDefault().fireApplicationRefreshed(cloudServer, module);
			}
		};

		asynchExecuteOperation(runnable);

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_APPLICATION_REFRESHED);
	}

	public void testModuleRefreshApplicationOp() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh handler

		String prefix = "testModuleRefreshModuleInstances";
		createWebApplicationProject();

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		deployAndWaitForDeploymentEvent(prefix);

		final IModule module = cloudServer.getExistingCloudModule(expectedAppName).getLocalModule();

		asynchExecuteOperationWaitForRefresh(cloudServer.getBehaviour().operations().refreshApplication(module),
				prefix, CloudServerEvent.EVENT_APPLICATION_REFRESHED);

	}

	public void testModuleRefreshDuringServerConnect1() throws Exception {
		String appPrefix = "testModuleRefreshDuringServerConnect1";
		createWebApplicationProject();

		deployAndWaitForDeploymentEvent(appPrefix);

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
		assertApplicationIsRunning(appModules.iterator().next());

	}

	public void testModuleRefreshDuringServerConnect2() throws Exception {
		// Deploy and start an application.
		// Disconnect through the server behaviour. Verify through an external
		// client that the app
		// remains deployed and in started mode.
		// Reconnect, and verify that the application is still running (i.e.
		// disconnecting
		// the server should not stop the application).

		String appPrefix = "testModuleRefreshDuringServerConnect2";
		String expectedAppName = harness.getDefaultWebAppName(appPrefix);

		createWebApplicationProject();

		// Note that deploying application fires off an app change event AFTER
		// the deployment is
		// successful. To make sure that the second event listener further down
		// does not accidentally receive the app
		// change event,
		// wait for the app change event from the deploy first, and then
		// schedule the second listener to
		// listen to the expected refresh event
		deployAndWaitForDeploymentEvent(appPrefix);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModules.iterator().next().getDeployedApplicationName());

		// Disconnect and verify that there are no cloud foundry application
		// modules
		serverBehavior.disconnect(new NullProgressMonitor());
		appModules = cloudServer.getExistingCloudModules();
		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());

		// Now create an external client to independently check that the
		// application remains deployed and in started mode

		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		List<CloudApplication> deployedApplications = client.getApplications();
		assertEquals("Expected 1 Cloud application in Cloud space after server disconnect", 1,
				deployedApplications.size());
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

}
