/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AbstractCloudFoundryServicesTest extends AbstractCloudFoundryTest {

	protected void deleteService(CloudService service) throws CoreException {
		harness.deleteService(service);
	}

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

	protected CloudService createCloudService(String name, String vendor) throws CoreException {
		CloudServiceOffering serviceConfiguration = getServiceConfiguration(vendor);
		if (serviceConfiguration != null) {
			CloudService service = new CloudService();
			service.setName(name);
			service.setLabel(vendor);
			service.setVersion(serviceConfiguration.getVersion());

			createService(service);
			return service;
		}
		return null;
	}

	protected CloudServiceOffering getServiceConfiguration(String vendor) throws CoreException {
		List<CloudServiceOffering> serviceConfigurations = serverBehavior
				.getServiceOfferings(new NullProgressMonitor());
		if (serviceConfigurations != null) {
			for (CloudServiceOffering serviceConfiguration : serviceConfigurations) {
				if (vendor.equals(serviceConfiguration.getLabel())) {
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
		List<String> servicesToUpdate = new ArrayList<String>();

		// Must iterate rather than passing to constructor or using
		// addAll, as some
		// of the entries in existing services may be null
		for (String existingService : boundServices) {
			if (existingService != null) {
				servicesToUpdate.add(existingService);
			}
		}

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

	protected void assertServiceNotExist(String expectedServicename) throws Exception {

		CloudService foundService = getCloudService(expectedServicename);

		assertNull(foundService);
	}

	protected void assertServiceEquals(CloudService expectedService, CloudService actualService) throws Exception {
		assertEquals(actualService.getName(), expectedService.getName());
		assertEquals(actualService.getLabel(), expectedService.getLabel());
	}

}
