/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.EnvironmentVariable;

/**
 * Describes the application that is to be pushed to a CF server, or already
 * exists in a server.
 * <p/>
 * This is the primary model of an application's metadata, and includes the
 * application's name, staging, URIs, and list of bound services. It mirrors a
 * {@link CloudApplication} , but unlike the latter, it is available for
 * applications that are not yet deployed. Additional information is contained
 * that is specific to the Eclipse plug-in, like deployment mode. Generally
 * speaking, the deployment info models an application's manifest file.
 * <p/>
 * However, values specific to the Eclipse plug-in should not be persisted in an
 * app's manifest file.
 */
public class ApplicationDeploymentInfo {

	private ApplicationAction deploymentMode;

	private boolean isIncrementalPublish;

	private Staging staging;

	private List<EnvironmentVariable> envVars;

	private int instances;

	private String name;

	private List<String> uris;

	private List<CloudService> services;

	private int memory;

	public ApplicationDeploymentInfo(String appName) {
		setDeploymentName(appName);
	}

	/**
	 * Eclipse-specific property that is not persisted in an application's
	 * manifest, or sent to the server. It indicates whether an application
	 * should be started or not. If null, it means an application should not be
	 * started when deployed.
	 * @return Application deployment mode. Null if an application should not be
	 * started in the CF server.
	 */
	public ApplicationAction getDeploymentMode() {
		return deploymentMode;
	}

	/**
	 * Eclipse-specific property that is not persisted in an application's
	 * manifest, or sent to the server. It indicates whether an application
	 * should be started or not. If null, it means an application should not be
	 * started when deployed.
	 */
	public void setDeploymentMode(ApplicationAction deploymentMode) {
		this.deploymentMode = deploymentMode;
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

	/**
	 * Eclipse-specific property. It is not persisted in an application's
	 * manifest, or sent to the server. If true, the application should be
	 * incrementally published (i.e. only changed resources are pushed to the
	 * server). If false, the entire application will be pushed to the server.
	 * @return true if the application should be incrementally published. False
	 * otherwise.
	 */
	public boolean isIncrementalPublish() {
		return isIncrementalPublish;
	}

	/**
	 * Eclipse-specific property. It is not persisted in an application's
	 * manifest, or sent to the server. If true, the application should be
	 * incrementally published (i.e. only changed resources are pushed to the
	 * server). If false, the entire application will be pushed to the server.
	 */
	public void setIncrementalPublish(boolean isIncrementalPublish) {
		this.isIncrementalPublish = isIncrementalPublish;
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

	/**
	 * 
	 * Sets the values of the parameter info, if non-null, into this info. Any
	 * know mutable values (e.g. containers and arrays) are set as copies.
	 */
	public void setInfo(ApplicationDeploymentInfo info) {
		if (info == null) {
			return;
		}
		setDeploymentMode(info.getDeploymentMode());
		setDeploymentName(info.getDeploymentName());
		setIncrementalPublish(info.isIncrementalPublish());
		setMemory(info.getMemory());
		setStaging(info.getStaging());
		setInstances(info.getInstances());

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
		info.setDeploymentMode(getDeploymentMode());
		info.setIncrementalPublish(isIncrementalPublish());
		info.setMemory(getMemory());
		info.setStaging(getStaging());
		info.setInstances(getInstances());

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
