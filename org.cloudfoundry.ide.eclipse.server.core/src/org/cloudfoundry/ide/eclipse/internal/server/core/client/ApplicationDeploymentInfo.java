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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.EnvironmentVariable;

/**
 * Describes the application that is to be pushed to a CF server, or already
 * exists in a server.
 * <p/>
 * This is the primary model of an application's metadata, and includes the
 * application's name, staging, URIs, and list of bound services. It mirrors a
 * {@link CloudApplication} , but unlike the latter, it is available for
 * applications that are not yet deployed. Note that properties that are NOT
 * part of an application deployment manifest (e.g. that are transient and only
 * applicable when an operation is being performed on the application, like
 * selecting its deployment mode) should not be defined here).
 */
public class ApplicationDeploymentInfo {

	private Staging staging;

	private List<EnvironmentVariable> envVars;

	private int instances;

	private String name;

	private List<String> uris;

	private List<CloudService> services;

	private int memory;

	private String archive;

	public ApplicationDeploymentInfo(String appName) {
		setDeploymentName(appName);
	}

	public void setEnvVariables(List<EnvironmentVariable> envVars) {
		this.envVars = envVars;
	}

	public List<EnvironmentVariable> getEnvVariables() {
		return envVars;
	}

	public int getInstances() {
		return instances;
	}

	public void setInstances(int instances) {
		this.instances = instances;
	}

	public Staging getStaging() {
		return staging;
	}

	public void setStaging(Staging staging) {
		this.staging = staging;
	}

	public String getDeploymentName() {
		return name;
	}

	public void setDeploymentName(String name) {
		this.name = name;
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	public List<String> getUris() {
		return uris;
	}

	public List<CloudService> getServices() {
		return services;
	}

	/**
	 * 
	 * @return never null, may be empty.
	 */
	public List<String> asServiceBindingList() {
		List<String> bindingList = new ArrayList<String>();

		if (services != null && !services.isEmpty()) {
			for (CloudService service : services) {
				bindingList.add(service.getName());
			}
		}
		return bindingList;
	}

	public void setServices(List<CloudService> services) {
		this.services = services;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}

	public String getArchive() {
		return this.archive;
	}

	public void setArchive(String archive) {
		this.archive = archive;
	}

	/**
	 * 
	 * Sets the values of the parameter info, if non-null, into this info. Any
	 * know mutable values (e.g. containers and arrays) are set as copies.
	 */
	public void setInfo(ApplicationDeploymentInfo info) {
		if (info == null) {
			return;
		}
		setDeploymentName(info.getDeploymentName());
		setMemory(info.getMemory());
		setStaging(info.getStaging());
		setInstances(info.getInstances());
		setArchive(info.getArchive());

		if (info.getServices() != null) {
			setServices(new ArrayList<CloudService>(info.getServices()));
		}
		else {
			setServices(null);
		}

		if (info.getUris() != null) {
			setUris(new ArrayList<String>(info.getUris()));
		}
		else {
			setUris(null);
		}

		if (info.getEnvVariables() != null) {
			setEnvVariables(new ArrayList<EnvironmentVariable>(info.getEnvVariables()));
		}
		else {
			setEnvVariables(null);
		}
	}

	/**
	 * Copy the deployment info, with any known mutable values set as copies as
	 * well. Therefore, if an info property is a list of values (e.g. list of
	 * bound services), modifying the list in the copy will not affect the list
	 * of values in the original version.
	 * @return non-null copy of this info.
	 */
	public ApplicationDeploymentInfo copy() {
		ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeploymentName());

		info.setMemory(getMemory());
		info.setStaging(getStaging());
		info.setInstances(getInstances());
		info.setArchive(getArchive());

		if (getServices() != null) {
			info.setServices(new ArrayList<CloudService>(getServices()));
		}

		if (getUris() != null) {
			info.setUris(new ArrayList<String>(getUris()));
		}

		if (getEnvVariables() != null) {
			info.setEnvVariables(new ArrayList<EnvironmentVariable>(getEnvVariables()));
		}

		return info;
	}
}
