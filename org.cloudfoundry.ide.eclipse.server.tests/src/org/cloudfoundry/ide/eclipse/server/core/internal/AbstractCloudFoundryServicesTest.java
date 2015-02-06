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
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AbstractCloudFoundryServicesTest extends AbstractRefreshCloudTest {

	protected void deleteService(CloudService service) throws CoreException {
		harness.deleteService(service);
	}

	protected CloudService createCloudService(String name, String label, String plan) throws CoreException {
		CloudService toCreate = getCloudServiceToCreate(name, label, plan);

		if (toCreate == null) {
			throw CloudErrorUtil.toCoreException("Unable to create service : " + label
					+ ". Service does not exist in the Cloud space.");
		}

		createService(toCreate);
		return toCreate;
	}

	protected CloudService getCloudServiceToCreate(String name, String label, String plan) throws CoreException {
		CloudServiceOffering serviceConfiguration = getServiceConfiguration(label);
		if (serviceConfiguration != null) {
			CloudService service = new CloudService();
			service.setName(name);
			service.setLabel(label);
			service.setVersion(serviceConfiguration.getVersion());

			boolean planExists = false;

			List<CloudServicePlan> plans = serviceConfiguration.getCloudServicePlans();

			for (CloudServicePlan pln : plans) {
				if (plan.equals(pln.getName())) {
					planExists = true;
					break;
				}
			}

			if (!planExists) {
				throw CloudErrorUtil.toCoreException("No plan: " + plan + " found for service :" + label);
			}
			service.setPlan(plan);

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

	protected ICloudFoundryOperation getBindServiceOp(CloudFoundryApplicationModule appModule, CloudService service)
			throws Exception {
		List<String> servicesToBind = new ArrayList<String>();
		servicesToBind.add(service.getName());

		return serverBehavior.operations().bindServices(appModule, servicesToBind);
	}

	protected ICloudFoundryOperation getUnbindServiceOp(CloudFoundryApplicationModule appModule, CloudService service)
			throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(appModule.getDeployedApplicationName());
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
		return serverBehavior.operations().bindServices(appModule, servicesToUpdate);
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
		serverBehavior.operations().createServices(new CloudService[] { service }).run(new NullProgressMonitor());
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

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

}
