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

import org.cloudfoundry.client.lib.CloudService;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public static final String MYSQL_SERVICE_NAME = "mysqlTestService";

	public void testCreateMysqlService() throws Exception {
		CloudService service = createMysqlService();
		assertServiceExists(service);
		deleteService(service);
	}

	public void testDeleteMysqlService() throws Exception {
		CloudService mysqlService = createMysqlService();
		assertServiceExists(mysqlService);
		deleteService(mysqlService);
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
