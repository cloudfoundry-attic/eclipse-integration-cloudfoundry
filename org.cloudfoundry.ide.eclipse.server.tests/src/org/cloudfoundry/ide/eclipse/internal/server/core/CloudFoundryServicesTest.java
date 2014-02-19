/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
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
		CloudFoundryApplicationModule appModule = deployApplicationStartMode(prefix);

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

		CloudFoundryApplicationModule appModule = deployApplicationStartMode(prefix);

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
