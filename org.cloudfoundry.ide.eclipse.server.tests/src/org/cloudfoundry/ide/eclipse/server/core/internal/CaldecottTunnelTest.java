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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ProxyHandler;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CaldecottTunnelTest extends AbstractCloudFoundryServicesTest {
	public static final String MYSQL_SERVICE_NAME = "mysqlCaldecottTestService";

	public static final String MONGODB_SERVICE_NAME = "mongodbCaldecottTestService";

	public static final String POSTGRESQL_SERVICE_NAME = "postgresqlCaldecottTestService";

	public static final String LOCAL_HOST = "127.0.0.1";

	public void testCreateMysqlTunnel() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		String expectedURL = "jdbc:mysql://" + LOCAL_HOST + ":" + descriptor.tunnelPort() + "/"
				+ descriptor.getDatabaseName();
		assertEquals(expectedURL, descriptor.getURL());

		stopTunnel(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	public void testCreateTunnelInvalidProxy() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		final boolean[] ran = { false };

		new ProxyHandler("invalid.proxy.test", 8080) {

			@Override
			protected void handleProxyChange() throws CoreException {
				CoreException ce = null;
				try {
					CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);

					assertNull(descriptor);
				}
				catch (CoreException e) {
					ce = e;
				}
				assertTrue(ce.getCause().getMessage().contains("I/O error: invalid.proxy.test"));
				ran[0] = true;
			}

		}.run();

		assertTrue(ran[0]);

		// Try again, it should work now, as proxy settings would have been
		// restored.
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);

		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		stopTunnel(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);

	}

	public void testCreateMongodbTunnel() throws Exception {
		CloudService service = getMongodbService();
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MONGODB_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MONGODB_SERVICE_NAME);

		stopTunnel(MONGODB_SERVICE_NAME);
		assertNoTunnel(MONGODB_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MONGODB_SERVICE_NAME);
	}

	public void testCreatePostgresqlTunnel() throws Exception {
		CloudService service = getPostgresqlService();
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(POSTGRESQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(POSTGRESQL_SERVICE_NAME);

		String expectedURL = "jdbc:postgresql://" + LOCAL_HOST + ":" + descriptor.tunnelPort() + "/"
				+ descriptor.getDatabaseName();
		assertEquals(expectedURL, descriptor.getURL());
		stopTunnel(POSTGRESQL_SERVICE_NAME);
		assertNoTunnel(POSTGRESQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(POSTGRESQL_SERVICE_NAME);
	}

	public void testCaldecottTunnelCloseOnServiceDeletion() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);
	}

	/*
	 * Test that when a non caldecott app has a service unbound, and that
	 * service also has a Caldecott tunnel, the tunnel does not get closed.
	 */
	public void testNonCaldecottServiceUnbinding_STS_2767() throws Exception {
		String prefix = "nonCaldecottServiceUnbinding";
		createWebApplicationProject();

		CloudFoundryApplicationModule appModule = assertDeployApplicationStartMode(prefix);

		CloudApplication nonCaldecottApp = appModule.getApplication();

		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		assertStopModule(appModule);

		bindServiceToApp(nonCaldecottApp, service);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		assertStartModule(appModule);
		assertServiceBound(service.getName(), nonCaldecottApp);

		assertStopModule(appModule);
		unbindServiceToApp(nonCaldecottApp, service);
		assertServiceNotBound(service.getName(), nonCaldecottApp);

		assertTunnel(MYSQL_SERVICE_NAME);

		stopTunnel(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);

		assertRemoveApplication(nonCaldecottApp);

	}

	public void testTunnelCloseOnCaldecottDeletion() throws Exception {

		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		CloudApplication caldecottApp = getCaldecottApplication();
		assertNotNull(caldecottApp);

		assertRemoveApplication(caldecottApp);

		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	public void testTunnelCloseOnCaldecottServiceUnbinding() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		CloudApplication caldecottApp = getCaldecottApplication();
		assertNotNull(caldecottApp);

		unbindServiceToApp(caldecottApp, service);
		assertServiceNotBound(service.getName(), caldecottApp);

		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	/**
	 * This should be run LAST to ensure that the Caldecott application is
	 * removed from the server after all Caldecott tests complete
	 * @throws Exception
	 */
	public void testCaldecottApplicationIsRemoved() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		CloudApplication caldecottApp = getCaldecottApplication();
		assertNotNull(caldecottApp);

		assertRemoveApplication(caldecottApp);

		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);

		caldecottApp = getCaldecottApplication();
		assertNull(caldecottApp);

	}

	/*
	 * 
	 * HELPERS
	 */

	/**
	 * 
	 * @return Application if found, or null if application is no longer in
	 * server
	 * @throws CoreException
	 */
	protected CloudApplication getCaldecottApplication() throws CoreException {
		return getUpdatedApplication(TunnelHelper.getTunnelAppName());
	}

	protected void stopTunnel(String serviceName) throws CoreException {
		TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
		handler.stopAndDeleteCaldecottTunnel(serviceName, new NullProgressMonitor());

	}

	protected void assertNoTunnel(String serviceName) throws Exception {
		TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
		assertFalse(handler.hasCaldecottTunnel(serviceName));
	}

	protected void assertTunnel(String serviceName) throws Exception {
		TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
		assertTrue(handler.hasCaldecottTunnel(serviceName));
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
		TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
		return handler.startCaldecottTunnel(serviceName, new NullProgressMonitor(), false);
	}

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

}
