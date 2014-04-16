/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public static final String SERVICE_NAME = "cfEclipseRegressionTestService";

	public void testCreateAndDeleteMysqlService() throws Exception {
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

		bindServiceToApp(app, service);

		assertStartModule(appModule);

		assertServiceBound(service.getName(), app);

	}

	public void testServiceUnBinding() throws Exception {
		CloudService service = createService();

		String prefix = "testServiceUnbinding";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		CloudApplication app = appModule.getApplication();

		assertStopModule(appModule);
		bindServiceToApp(app, service);

		assertStartModule(appModule);

		assertServiceBound(service.getName(), app);

		assertStopModule(appModule);
		unbindServiceToApp(app, service);
		assertServiceNotBound(service.getName(), app);

	}

	public void testNoService() throws Exception {
		// There should be no service with this name. make sure there was proper
		// tear down
		assertServiceNotExist(SERVICE_NAME);
	}

	protected CloudService createService() throws Exception {
		return createCloudService(SERVICE_NAME, "mongolab");
	}
}
