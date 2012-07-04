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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CaldecottTunnelTest extends AbstractCloudFoundryServicesTest {
	public static final String MYSQL_SERVICE_NAME = "mysqlCaldecottTestService";

	public static final String MONGODB_SERVICE_NAME = "mongodbCaldecottTestService";

	public static final String POSTGRESQL_SERVICE_NAME = "postgresqlCaldecottTestService";

	public static final String LOCAL_HOST = "127.0.0.1";

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Make sure services used in this test are deleted first.
		List<CloudService> toDelete = new ArrayList<CloudService>();
		CloudService service = getMysqlService();
		if (service != null) {
			toDelete.add(service);
		}
		service = getMongodbService();
		if (service != null) {
			toDelete.add(service);
		}
		service = getPostgresqlService();
		if (service != null) {
			toDelete.add(service);
		}
		for (CloudService serviceToDelete : toDelete) {
			deleteService(serviceToDelete);
		}
	}

	public void testCreateMysqlTunnel() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		String expectedURL = "jdbc:mysql://" + LOCAL_HOST + ":" + descriptor.tunnelPort() + "/"
				+ descriptor.getDatabaseName();
		assertEquals(expectedURL, descriptor.getURL());
		stopTunnel(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);
		deleteService(service);
	}

	public void testCreateMongodbTunnel() throws Exception {
		CloudService service = getMongodbService();
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MONGODB_SERVICE_NAME);
		assertNotNull(descriptor);
		stopTunnel(MONGODB_SERVICE_NAME);
		assertNoTunnel(MONGODB_SERVICE_NAME);
		deleteService(service);
	}

	public void testCreatePostgresqlTunnel() throws Exception {
		CloudService service = getPostgresqlService();
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(POSTGRESQL_SERVICE_NAME);
		assertNotNull(descriptor);
		String expectedURL = "jdbc:postgresql://" + LOCAL_HOST + ":" + descriptor.tunnelPort() + "/"
				+ descriptor.getDatabaseName();
		assertEquals(expectedURL, descriptor.getURL());
		stopTunnel(POSTGRESQL_SERVICE_NAME);
		assertNoTunnel(POSTGRESQL_SERVICE_NAME);
		deleteService(service);
	}

	protected void stopTunnel(String serviceName) throws CoreException {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		handler.stopAndDeleteCaldecottTunnel(serviceName, new NullProgressMonitor());

	}

	protected void assertNoTunnel(String serviceName) throws Exception {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		assertFalse(handler.hasCaldecottTunnel(serviceName));
	}

	protected CloudService getMysqlService() throws Exception {
		CloudService service = getCloudService(MYSQL_SERVICE_NAME);
		if (service == null) {
			service = createCloudService(MYSQL_SERVICE_NAME, "mysql");
		}
		return service;
	}

	protected CloudService getMongodbService() throws Exception {
		CloudService service = getCloudService(MONGODB_SERVICE_NAME);
		if (service == null) {
			service = createCloudService(MONGODB_SERVICE_NAME, "mongodb");
		}
		return service;
	}

	protected CloudService getPostgresqlService() throws Exception {
		CloudService service = getCloudService(POSTGRESQL_SERVICE_NAME);
		if (service == null) {
			service = createCloudService(POSTGRESQL_SERVICE_NAME, "postgresql");
		}
		return service;
	}

	protected CaldecottTunnelDescriptor createCaldecottTunnel(String serviceName) throws CoreException {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		return handler.startCaldecottTunnel(serviceName, new NullProgressMonitor());
	}

}
