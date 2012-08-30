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

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.ServiceConfiguration;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AbstractCloudFoundryServicesTest extends AbstractCloudFoundryTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Make sure services used in this test are deleted first.
		List<CloudService> services = getAllServices();
		for (CloudService service : services) {
			deleteService(service);
			CloudFoundryTestUtil.waitIntervals(2000);
		}

	}

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

	protected CloudService createCloudService(String name, String vendor) throws CoreException {
		ServiceConfiguration serviceConfiguration = getServiceConfigration(vendor);
		if (serviceConfiguration != null) {
			CloudService service = new CloudService();
			service.setName(name);
			service.setType(serviceConfiguration.getType());
			service.setVendor(vendor);
			service.setVersion(serviceConfiguration.getVersion());
			List<ServiceConfiguration.Tier> tiers = serviceConfiguration.getTiers();
			if (tiers != null && !tiers.isEmpty()) {
				service.setTier(tiers.get(0).getType());
			}

			createService(service);
			return service;
		}
		return null;
	}

	protected ServiceConfiguration getServiceConfigration(String vendor) throws CoreException {
		List<ServiceConfiguration> serviceConfigurations = serverBehavior
				.getServiceConfigurations(new NullProgressMonitor());
		if (serviceConfigurations != null) {
			for (ServiceConfiguration serviceConfiguration : serviceConfigurations) {
				if (vendor.equals(serviceConfiguration.getVendor())) {
					return serviceConfiguration;
				}
			}
		}
		return null;
	}

	protected void bindServiceToApp(CloudApplication application, CloudService service) throws Exception {
		List<String> servicesToBind = new ArrayList<String>();
		servicesToBind.add(service.getName());
		serverBehavior.updateServices(application.getName(), servicesToBind, new NullProgressMonitor());
	}

	protected void unbindServiceToApp(CloudApplication application, CloudService service) throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(application.getName());
		List<String> boundServices = updatedApplication.getServices();
		List<String> servicesToUpdate = new ArrayList<String>(boundServices);

		if (servicesToUpdate.contains(service.getName())) {
			servicesToUpdate.remove(service.getName());
		}
		serverBehavior.updateServicesAndCloseCaldecottTunnels(application.getName(), servicesToUpdate,
				new NullProgressMonitor());

	}

	protected void assertServiceBound(String serviceName, CloudApplication application) throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(application.getName());
		assertNotNull(updatedApplication);
		String foundService = findService(serviceName, updatedApplication);
		assertNotNull(foundService);
	}

	protected void assertServiceNotBound(String serviceName, CloudApplication application) throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(application.getName());
		assertNotNull(updatedApplication);
		String foundService = findService(serviceName, updatedApplication);
		assertNull(foundService);
	}

	protected String findService(String serviceName, CloudApplication app) {
		List<String> boundServices = app.getServices();
		String foundService = null;
		for (String boundService : boundServices) {
			if (serviceName.equals(boundService)) {
				foundService = boundService;
				break;
			}
		}
		return foundService;
	}

	protected void createService(CloudService service) throws CoreException {
		serverBehavior.createService(new CloudService[] { service }, new NullProgressMonitor());
	}

	protected void deleteService(CloudService serviceToDelete) throws CoreException {
		String serviceName = serviceToDelete.getName();
		List<String> services = new ArrayList<String>();
		services.add(serviceName);

		serverBehavior.deleteServices(services, new NullProgressMonitor());
	}

	protected void assertServiceExists(CloudService expectedService) throws Exception {
		String expectedServicename = expectedService.getName();
		CloudService foundService = getCloudService(expectedServicename);
		assertNotNull(foundService);
		assertServiceEquals(expectedService, foundService);
	}

	protected void assertServiceExists(String serviceName) throws Exception {
		CloudService foundService = getCloudService(serviceName);
		assertNotNull(foundService);
	}

	protected CloudService getCloudService(String serviceName) throws CoreException {

		List<CloudService> services = serverBehavior.getServices(new NullProgressMonitor());
		CloudService foundService = null;
		if (services != null) {
			for (CloudService service : services) {
				if (serviceName.equals(service.getName())) {
					foundService = service;
					break;
				}
			}
		}
		return foundService;
	}

	protected List<CloudService> getAllServices() throws CoreException {
		List<CloudService> services = serverBehavior.getServices(new NullProgressMonitor());
		if (services == null) {
			services = new ArrayList<CloudService>(0);
		}
		return services;
	}

	protected void assertServiceNotExist(String expectedServicename) throws Exception {

		CloudService foundService = getCloudService(expectedServicename);

		assertNull(foundService);
	}

	protected void assertServiceEquals(CloudService expectedService, CloudService actualService) throws Exception {
		assertEquals(actualService.getName(), expectedService.getName());
		assertEquals(actualService.getType(), expectedService.getType());
		assertEquals(actualService.getVendor(), expectedService.getVendor());
	}

}
