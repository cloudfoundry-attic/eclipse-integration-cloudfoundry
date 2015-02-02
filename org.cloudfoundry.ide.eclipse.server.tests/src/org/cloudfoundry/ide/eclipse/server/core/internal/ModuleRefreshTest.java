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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
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
public class ModuleRefreshTest extends AbstractAsynchCloudTest {

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
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

		String prefix = "testModuleUpdates";
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
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		// Refresh Module through behaviour to check if it picks up changes

		// 1. Test via single-module update
		client.updateApplicationMemory(appName, 737);
		CloudApplication updatedCloudApplicationFromClient = client.getApplication(appName);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertNotNull(appModule.getApplication());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(737, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

		// 2. Test via single-module update and it's instances
		client.updateApplicationMemory(appName, 555);
		updatedCloudApplicationFromClient = client.getApplication(appName);
		appModule = serverBehavior.updateModuleWithInstances(appName, new NullProgressMonitor());

		assertNotNull(appModule.getApplication());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(555, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

		// 3. Test via module refresh of all modules
		client.updateApplicationMemory(appName, 345);
		updatedCloudApplicationFromClient = client.getApplication(appName);
		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		allApps.put(updatedCloudApplicationFromClient.getName(), updatedCloudApplicationFromClient);

		cloudServer.updateModules(allApps);

		appModule = cloudServer.getExistingCloudModule(appName);

		assertNotNull(appModule.getApplication());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(345, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

	}

	public void testSingleModuleUpdateExternalDeletion() throws Exception {

		String prefix = "testSingleModuleUpdateExternalDeletion";
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

		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Delete module externally and verify that module refresh picks up the
		// change

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		client.deleteApplication(appName);

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertNull(appModule);

	}

	public void testAllModuleUpdateExternalDeletion() throws Exception {

		String prefix = "testAllModuleUpdateExternalDeletion";
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

		// Verify the module exists
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Delete module externally and verify that module refresh picks up the
		// change

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		client.deleteApplication(appName);

		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		cloudServer.updateModules(allApps);

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

		// Delete module externally and verify that module refresh picks up the
		// change

		// Create separate external client
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.getExpectedDefaultURL(prefix));
		client.createApplication(appName, new Staging(), CloudUtil.DEFAULT_MEMORY, urls, new ArrayList<String>());

		appModule = serverBehavior.updateCloudModule(appName, new NullProgressMonitor());

		assertNotNull(appModule);
		assertNotNull(appModule.getApplication());
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

		// Delete module externally and verify that module refresh picks up the
		// change

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
		assertNotNull(appModule);
		assertNotNull(appModule.getApplication());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());
		assertEquals(CloudUtil.DEFAULT_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());

	}

	public void testRefreshHandler() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh listener
		String prefix = "testRefreshHandler";
		createWebApplicationProject();
		assertDeployApplicationStartMode(prefix);

		// Test the server-wide refresh of all modules therefore do not pass the
		// app name
		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		cloudServer.getBehaviour().getRefreshHandler().scheduleRefresh();

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_SERVER_REFRESHED);
	}

	public void testRefreshHandlerFireEvent() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh handler

		String prefix = "testRefreshHandlerFireEvent";
		createWebApplicationProject();

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		assertDeployApplicationStartMode(prefix);

		final IModule module = cloudServer.getExistingCloudModule(expectedAppName).getLocalModule();

		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(expectedAppName, cloudServer,
				CloudServerEvent.EVENT_APP_CHANGED);

		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				cloudServer.getBehaviour().getRefreshHandler().fireRefreshEvent(module);
			}
		};

		asynchExecuteOperation(runnable);

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_APP_CHANGED);
	}

	public void testModuleRefreshModuleInstances() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh handler

		String prefix = "testModuleRefreshModuleInstances";
		createWebApplicationProject();

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		assertDeployApplicationStartMode(prefix);

		final IModule module = cloudServer.getExistingCloudModule(expectedAppName).getLocalModule();

		asynchExecuteOperationWaitForRefresh(cloudServer.getBehaviour().operations().refreshInstances(module), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

	}
}
