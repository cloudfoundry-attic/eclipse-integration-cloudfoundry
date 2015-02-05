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
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudRefreshEvent;
import org.cloudfoundry.ide.eclipse.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public static final String SERVICE_NAME = "cfEclipseRegressionTestService";

	public void testCreateAndDeleteService() throws Exception {
		CloudService service = createDefaultService();
		assertServiceExists(service);

		List<CloudService> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existingServices.size() == 1);
		assertEquals(SERVICE_NAME, existingServices.get(0).getName());
		assertEquals("elephantsql", existingServices.get(0).getLabel());
		assertEquals("turtle", existingServices.get(0).getPlan());

		assertServiceExists(existingServices.get(0));

		deleteService(service);
		assertServiceNotExist(SERVICE_NAME);

		existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", existingServices.isEmpty());
	}

	public void testServiceBindingInDeploymentInfo() throws Exception {
		CloudService serviceToCreate = createDefaultService();
		String prefix = "testServiceBindingInDeploymentInfo";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		CloudApplication app = appModule.getApplication();

		asynchExecuteOperationWaitForRefresh(getBindServiceOp(appModule, serviceToCreate), prefix,
				CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		assertServiceBound(serviceToCreate.getName(), app);

		// Cloud Module should also now be updated
		CloudFoundryApplicationModule updatedModule = cloudServer.getExistingCloudModule(app.getName());

		List<CloudService> boundServices = updatedModule.getDeploymentInfo().getServices();
		assertEquals(1, boundServices.size());
		assertEquals(serviceToCreate.getName(), boundServices.get(0).getName());
	}

	public void testServiceBindingUnbindingAppStarted() throws Exception {
		CloudService service = createDefaultService();

		String prefix = "testServiceBindingUnbindingAppStarted";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		asynchExecuteOperationWaitForRefresh(getBindServiceOp(appModule, service), prefix,
				CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceBound(service.getName(), appModule.getApplication());
		assertEquals(1, appModule.getDeploymentInfo().getServices().size());
		assertEquals(SERVICE_NAME, appModule.getDeploymentInfo().getServices().get(0).getName());

		asynchExecuteOperationWaitForRefresh(getUnbindServiceOp(appModule, service), prefix,
				CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceNotBound(service.getName(), appModule.getApplication());
		assertEquals(0, appModule.getDeploymentInfo().getServices().size());

	}

	public void testServiceBindingUnbindingAppStopped() throws Exception {
		CloudService service = createDefaultService();

		String prefix = "testServiceBindingUnbindingAppStopped";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = deployAndWaitForAppStart(prefix);

		serverBehavior.operations().applicationDeployment(appModule, ApplicationAction.STOP)
				.run(new NullProgressMonitor());

		waitForAppToStop(appModule);

		asynchExecuteOperationWaitForRefresh(getBindServiceOp(appModule, service), prefix,
				CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceBound(service.getName(), appModule.getApplication());
		assertEquals(1, appModule.getDeploymentInfo().getServices().size());
		assertEquals(SERVICE_NAME, appModule.getDeploymentInfo().getServices().get(0).getName());

		asynchExecuteOperationWaitForRefresh(getUnbindServiceOp(appModule, service), prefix,
				CloudServerEvent.EVENT_APPLICATION_REFRESHED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceNotBound(service.getName(), appModule.getApplication());
		assertEquals(0, appModule.getDeploymentInfo().getServices().size());
	}

	public void testServiceCreationEvent() throws Exception {
		// Test service creation operation and that asynchronous service
		// creation triggers a service change event

		CloudService service = getCloudServiceToCreate(SERVICE_NAME, "elephantsql", "turtle");
		asynchExecuteOperationWaitForRefresh(
				serverBehavior.operations().createServices(new CloudService[] { service }), null,
				CloudServerEvent.EVENT_UPDATE_SERVICES);
		assertServiceExists(service);

		List<CloudService> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertEquals(SERVICE_NAME, existingServices.get(0).getName());
	}

	public void testServiceDeletionEvent() throws Exception {
		// Test service deletion operation and that asynchronous service
		// creation triggers a service change event

		final CloudService service = createDefaultService();
		assertServiceExists(service);

		String serviceName = service.getName();
		List<String> services = new ArrayList<String>();
		services.add(serviceName);

		asynchExecuteOperationWaitForRefresh(serverBehavior.operations().deleteServices(services), null,
				CloudServerEvent.EVENT_UPDATE_SERVICES);

		assertServiceNotExist(SERVICE_NAME);

		List<CloudService> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", existingServices.isEmpty());
	}

	public void testServiceCreationOp() throws Exception {

		CloudService toCreate = getCloudServiceToCreate("testServiceCreationOp", "elephantsql", "turtle");
		CloudService[] services = new CloudService[] { toCreate };

		serverBehavior.operations().createServices(services).run(new NullProgressMonitor());

		List<CloudService> existing = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existing.size() == 1);
		assertEquals("testServiceCreationOp", existing.get(0).getName());
		assertEquals("elephantsql", existing.get(0).getLabel());
		assertEquals("turtle", existing.get(0).getPlan());

		assertServiceExists(existing.get(0));
	}

	public void testServiceDeletionOp() throws Exception {

		CloudService toCreate = getCloudServiceToCreate("testServiceDeletionOp", "elephantsql", "turtle");
		CloudService[] services = new CloudService[] { toCreate };

		serverBehavior.operations().createServices(services).run(new NullProgressMonitor());

		List<CloudService> existing = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existing.size() == 1);
		assertEquals("testServiceDeletionOp", existing.get(0).getName());
		assertEquals("elephantsql", existing.get(0).getLabel());
		assertEquals("turtle", existing.get(0).getPlan());

		assertServiceExists(existing.get(0));

		List<String> toDelete = new ArrayList<String>();
		toDelete.add("testServiceDeletionOp");

		serverBehavior.operations().deleteServices(toDelete).run(new NullProgressMonitor());

		List<CloudService> remainingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", remainingServices.isEmpty());
	}

	public void testServiceCreationServicesInEvent() throws Exception {

		CloudService toCreate = getCloudServiceToCreate("testServiceCreationServicesInEvent", "elephantsql", "turtle");
		CloudService[] services = new CloudService[] { toCreate };

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_UPDATE_SERVICES);

		serverBehavior.operations().createServices(services).run(new NullProgressMonitor());

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_UPDATE_SERVICES, refreshListener.getMatchedEvent().getType());

		assertTrue("Expected " + CloudRefreshEvent.class,
				refreshListener.getMatchedEvent() instanceof CloudRefreshEvent);
		CloudRefreshEvent cloudEvent = (CloudRefreshEvent) refreshListener.getMatchedEvent();
		List<CloudService> existing = cloudEvent.getServices();
		assertTrue("Expected 1 created service in cloud refresh event", existing.size() == 1);
		assertEquals("testServiceCreationServicesInEvent", existing.get(0).getName());
		assertEquals("elephantsql", existing.get(0).getLabel());
		assertEquals("turtle", existing.get(0).getPlan());

		assertServiceExists(existing.get(0));

		refreshListener.dispose();
	}

	public void testServiceDeletionServicesInEvent() throws Exception {

		CloudService service = createCloudService("testServiceDeletionServicesInEvent", "elephantsql", "turtle");
		assertServiceExists(service);
		service = createCloudService("testAnotherService", "elephantsql", "turtle");
		List<CloudService> services = serverBehavior.getServices(new NullProgressMonitor());
		assertEquals("Expected 2 services", 2, services.size());

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_UPDATE_SERVICES);

		serverBehavior.operations()
				.deleteServices(Arrays.asList(new String[] { "testServiceDeletionServicesInEvent" }))
				.run(new NullProgressMonitor());

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_UPDATE_SERVICES, refreshListener.getMatchedEvent().getType());
		assertTrue("Expected " + CloudRefreshEvent.class,
				refreshListener.getMatchedEvent() instanceof CloudRefreshEvent);

		CloudRefreshEvent cloudEvent = (CloudRefreshEvent) refreshListener.getMatchedEvent();
		List<CloudService> eventServices = cloudEvent.getServices();

		assertTrue("Expected 1 service in cloud refresh event", eventServices.size() == 1);
		assertEquals("testAnotherService", eventServices.get(0).getName());
		assertEquals("elephantsql", eventServices.get(0).getLabel());
		assertEquals("turtle", eventServices.get(0).getPlan());

		assertServiceExists(eventServices.get(0));

		refreshListener.dispose();
	}

	public void testExternalCreatedServiceRefresh() throws Exception {
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		CloudService service = getCloudServiceToCreate("testExternalCreatedServiceRefresh", "elephantsql", "turtle");

		client.createService(service);

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		serverBehavior.getRefreshHandler().scheduleRefreshAll();

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));

		assertEquals(CloudServerEvent.EVENT_SERVER_REFRESHED, refreshListener.getMatchedEvent().getType());
		assertTrue("Expected " + CloudRefreshEvent.class,
				refreshListener.getMatchedEvent() instanceof CloudRefreshEvent);

		CloudRefreshEvent cloudEvent = (CloudRefreshEvent) refreshListener.getMatchedEvent();
		List<CloudService> eventServices = cloudEvent.getServices();
		assertTrue("Expected 1 service in cloud refresh event", eventServices.size() == 1);
		assertEquals("testExternalCreatedServiceRefresh", eventServices.get(0).getName());
		assertEquals("elephantsql", eventServices.get(0).getLabel());
		assertEquals("turtle", eventServices.get(0).getPlan());
	}

	public void testExternalCreatedServiceBehaviour() throws Exception {
		CloudFoundryOperations client = harness.createExternalClient();
		client.login();
		CloudService service = getCloudServiceToCreate("testExternalCreatedServiceBehaviour", "elephantsql", "turtle");

		client.createService(service);

		List<CloudService> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existingServices.size() == 1);
		assertEquals("testExternalCreatedServiceBehaviour", existingServices.get(0).getName());
		assertEquals("elephantsql", existingServices.get(0).getLabel());
		assertEquals("turtle", existingServices.get(0).getPlan());
	}

	public void testNoService() throws Exception {
		// There should be no service with this name. make sure there was proper
		// tear down
		assertServiceNotExist(SERVICE_NAME);
	}

	protected CloudService createDefaultService() throws Exception {
		return createCloudService(SERVICE_NAME, "elephantsql", "turtle");
	}
}
