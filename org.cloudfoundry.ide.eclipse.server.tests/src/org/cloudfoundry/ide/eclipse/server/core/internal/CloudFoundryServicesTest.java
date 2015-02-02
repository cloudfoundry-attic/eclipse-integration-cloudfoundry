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
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public static final String SERVICE_NAME = "cfEclipseRegressionTestService";

	public void testCreateAndDeleteService() throws Exception {
		CloudService service = createService();
		assertServiceExists(service);
		deleteService(service);
		assertServiceNotExist(SERVICE_NAME);
	}

	public void testServiceBinding() throws Exception {
		CloudService service = createService();
		String prefix = "testServiceBinding";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		CloudApplication app = appModule.getApplication();
		assertStopModule(appModule);

		getBindServiceOp(appModule, service).run(new NullProgressMonitor());

		assertStartModule(appModule);

		assertServiceBound(service.getName(), app);

	}

	public void testServiceBindingInDeploymentInfo() throws Exception {
		CloudService serviceToCreate = createService();
		String prefix = "testServiceBindingInDeploymentInfo";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		CloudApplication app = appModule.getApplication();

		getBindServiceOp(appModule, serviceToCreate).run(new NullProgressMonitor());

		assertServiceBound(serviceToCreate.getName(), app);

		// Cloud Module should also now be updated
		CloudFoundryApplicationModule updatedModule = cloudServer.getExistingCloudModule(app.getName());

		List<CloudService> boundServices = updatedModule.getDeploymentInfo().getServices();
		assertEquals(1, boundServices.size());
		assertEquals(serviceToCreate.getName(), boundServices.get(0).getName());
	}

	public void testServiceUnBinding() throws Exception {
		CloudService service = createService();

		String prefix = "testServiceUnbinding";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		assertStopModule(appModule);
		getBindServiceOp(appModule, service).run(new NullProgressMonitor());

		assertStartModule(appModule);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceBound(service.getName(), appModule.getApplication());

		assertStopModule(appModule);
		getUnbindServiceOp(appModule, service).run(new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceNotBound(service.getName(), appModule.getApplication());
	}

	public void testAsynchRefreshOnServiceCreation() throws Exception {
		// Test service creation operation and that asynchronous service
		// creation triggers a service change event

		CloudService service = getCloudServiceToCreate(SERVICE_NAME, "elephantsql", "turtle");
		asynchExecuteOperationWaitForRefresh(
				serverBehavior.operations().createServices(new CloudService[] { service }), null,
				CloudServerEvent.EVENT_UPDATE_SERVICES);
		assertServiceExists(service);
	}

	public void testAsynchRefreshOnServiceDeletion() throws Exception {
		// Test service deletion operation and that asynchronous service
		// creation triggers a service change event

		final CloudService service = createService();
		assertServiceExists(service);

		String serviceName = service.getName();
		List<String> services = new ArrayList<String>();
		services.add(serviceName);

		asynchExecuteOperationWaitForRefresh(serverBehavior.operations().deleteServices(services), null,
				CloudServerEvent.EVENT_UPDATE_SERVICES);

		assertServiceNotExist(SERVICE_NAME);
	}

	public void testAsynchRefreshOnServiceBinding() throws Exception {
		// Test that asynchronous service binding triggers a module refresh
		// event
		final CloudService service = createService();
		String prefix = "testAsynchRefreshOnServiceBinding";
		createWebApplicationProject();
		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		asynchExecuteOperationWaitForRefresh(getBindServiceOp(appModule, service), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

		// Get updated module
		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());
		assertServiceBound(service.getName(), appModule.getApplication());
	}

	public void testAsynchServerRefreshOnServiceUnbinding() throws Exception {
		// Tests that unbinding a service triggers a module refresh
		final CloudService service = createService();

		String prefix = "testAsynchServerRefreshOnServiceUnbinding";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		getBindServiceOp(appModule, service).run(new NullProgressMonitor());

		assertServiceBound(service.getName(), appModule.getApplication());

		asynchExecuteOperationWaitForRefresh(getUnbindServiceOp(appModule, service), prefix,
				CloudServerEvent.EVENT_APP_CHANGED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertServiceNotBound(service.getName(), appModule.getApplication());

	}

	public void testNoService() throws Exception {
		// There should be no service with this name. make sure there was proper
		// tear down
		assertServiceNotExist(SERVICE_NAME);
	}

	protected CloudService createService() throws Exception {
		return createCloudService(SERVICE_NAME, "elephantsql", "turtle");
	}
}
