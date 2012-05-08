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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * @author Steffen Pingel
 */
public class TestCallback extends CloudFoundryCallback {

	private final String appName;

	private final String url;

	public TestCallback() {
		this.appName = null;
		this.url = null;
	}

	public TestCallback(String appName) {
		this.appName = appName;
		this.url = null;
	}

	public TestCallback(String appName, String url) {
		this.appName = appName;
		this.url = url;
	}

	@Override
	public void applicationStarted(CloudFoundryServer server, ApplicationModule cloudModule) {
		// ignore
	}

	@Override
	public void applicationStopping(CloudFoundryServer server, ApplicationModule cloudModule) {
		// ignore
	}

	@Override
	public void disconnecting(CloudFoundryServer server) {
		// ignore
	}

	@Override
	public void getCredentials(CloudFoundryServer server) {
		throw new OperationCanceledException();
	}

	@Override
	public DeploymentDescriptor prepareForDeployment(CloudFoundryServer server, ApplicationModule module,
			IProgressMonitor monitor) {
		DeploymentDescriptor descriptor = new DeploymentDescriptor();
		String appName;

		if (this.appName != null) {
			appName = this.appName;
		}
		else {
			appName = module.getName();
		}

		descriptor.applicationInfo = new ApplicationInfo(appName);
		descriptor.deploymentInfo = new DeploymentInfo();
		descriptor.deploymentInfo.setMemory(128);
		descriptor.deploymentInfo.setDeploymentName(appName);
		descriptor.deploymentMode = ApplicationAction.START;

		if (url != null) {
			descriptor.deploymentInfo.setUris(Collections.singletonList(url));
		}
		else {
			descriptor.deploymentInfo.setUris(Collections.singletonList(module.getDefaultLaunchUrl()));
		}

		return descriptor;
	}

	@Override
	public void deleteServices(List<String> services, CloudFoundryServer cloudServer) {
		// ignore
	}

	private boolean autoDeployEnabled;

	@Override
	public boolean isAutoDeployEnabled() {
		return autoDeployEnabled;
	}

	public void setAutoDeployEnabled(boolean autoDeployEnabled) {
		this.autoDeployEnabled = autoDeployEnabled;
	}

	@Override
	public void deleteApplication(ApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		// ignore
	}

	@Override
	public void displayCaldecottTunnelConnections(CloudFoundryServer server) {
		// ignore
	}

}
