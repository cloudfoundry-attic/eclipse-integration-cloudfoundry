/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudService;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public static final String MYSQL_SERVICE_NAME = "mysqlTestService";

	public void testCreateAndDeleteMysqlService() throws Exception {
		CloudService service = createMysqlService();
		assertServiceExists(service);
		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	public void testServiceBinding() throws Exception {
		CloudService service = createMysqlService();

		CloudApplication app = createAndAssertTestApp();
		stopAndAssertApplication(app);

		bindServiceToApp(app, service);
		startAndAssertApplication(app);
		assertServiceBound(service.getName(), app);

		removeAndAssertApplication(app);
		deleteService(service);

		assertServiceNotExist(MYSQL_SERVICE_NAME);

	}

	public void testServiceUnBinding() throws Exception {
		CloudService service = createMysqlService();

		CloudApplication app = createAndAssertTestApp();
		stopAndAssertApplication(app);
		bindServiceToApp(app, service);

		startAndAssertApplication(app);
		assertServiceBound(service.getName(), app);

		stopAndAssertApplication(app);
		unbindServiceToApp(app, service);
		assertServiceNotBound(service.getName(), app);

		removeAndAssertApplication(app);
		deleteService(service);

		assertServiceNotExist(MYSQL_SERVICE_NAME);

	}

	public void testNoService() throws Exception {
		// There should be no service with this name. make sure there was proper
		// tear down
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	protected CloudService createMysqlService() throws Exception {
		return createCloudService(MYSQL_SERVICE_NAME, "mysql");
	}
}
