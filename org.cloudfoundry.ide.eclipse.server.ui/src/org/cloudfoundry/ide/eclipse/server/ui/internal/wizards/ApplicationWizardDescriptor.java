/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.server.core.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.eclipse.core.runtime.Assert;

/**
 * 
 * Descriptor that contains all the necessary information to push an application
 * to a Cloud Foundry server, such as the application's name, URL, framework,
 * and runtime
 * <p/>
 * This descriptor is shared by all the pages in the application deployment
 * wizard. Some values are required, and must always be set in order to push the
 * application to the server
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters. 
 * 
 */
public class ApplicationWizardDescriptor {

	private final ApplicationDeploymentInfo deploymentInfo;

	private List<CloudService> createdCloudServices;

	private boolean persistDeploymentInfo;

	private ApplicationAction applicationStartMode;

	public ApplicationWizardDescriptor(ApplicationDeploymentInfo deploymentInfo) {
		Assert.isNotNull(deploymentInfo);

		this.deploymentInfo = deploymentInfo;
	}

	public void setStartCommand(String startCommand) {
		Staging staging = deploymentInfo.getStaging();
		String buildpackUrl = staging != null ? staging.getBuildpackUrl() : null;
		staging = new Staging(startCommand, buildpackUrl);
		deploymentInfo.setStaging(staging);
	}

	public void setBuildpack(String buildpack) {
		Staging staging = deploymentInfo.getStaging();

		String existingStartCommand = staging != null ? staging.getCommand() : null;
		staging = new Staging(existingStartCommand, buildpack);
		deploymentInfo.setStaging(staging);
	}

	/**
	 * Optional value. List of services to be created. If a user does not create
	 * services in the Application wizard, return null or an empty list.
	 * @return Optional list of created services, or null/empty list if no
	 * services are to be created
	 */
	public List<CloudService> getCloudServicesToCreate() {
		return createdCloudServices;
	}

	public void setCloudServicesToCreate(List<CloudService> createdCloudServices) {
		this.createdCloudServices = createdCloudServices;
	}

	/**
	 * Sets the start mode for the application.
	 */
	public void setApplicationStartMode(ApplicationAction applicationStartMode) {
		this.applicationStartMode = applicationStartMode;
	}

	/**
	 * Get the start mode for the application.
	 */
	public ApplicationAction getApplicationStartMode() {
		return applicationStartMode;
	}

	/**
	 * Its never null. An application wizard descriptor always wraps around an
	 * actual deployment info.
	 * @return non-null deployment info
	 */
	public ApplicationDeploymentInfo getDeploymentInfo() {
		return deploymentInfo;
	}

	/**
	 * 
	 * @param persist true if the deployment descriptor should be persisted in
	 * the app's manifest file. If the manifest file already exists, it will be
	 * overwritten. False otherwise.
	 */
	public void persistDeploymentInfo(boolean persist) {
		this.persistDeploymentInfo = persist;
	}

	/**
	 * 
	 * @return true if the deployment info should be persisted in an app's
	 * manifest. False otherwise.
	 */
	public boolean shouldPersistDeploymentInfo() {
		return persistDeploymentInfo;
	}

}
