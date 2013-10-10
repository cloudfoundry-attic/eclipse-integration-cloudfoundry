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

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;

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
public class ApplicationDeploymentInfo extends DeploymentInfo {

	private ApplicationAction deploymentMode;

	private boolean isIncrementalPublish;

	private Staging staging;

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

	/*
	 * TODO: Staging should pulled up to the parent, as they are client-defined.
	 */
	public Staging getStaging() {
		return staging;
	}

	public void setStaging(Staging staging) {
		this.staging = staging;
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

		if (info.getServices() != null) {
			setServices(new ArrayList<String>(info.getServices()));
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

		if (getServices() != null) {
			info.setServices(new ArrayList<String>(getServices()));
		}

		if (getUris() != null) {
			info.setUris(new ArrayList<String>(getUris()));
		}
		return info;
	}
}
