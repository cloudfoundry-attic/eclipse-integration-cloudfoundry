/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * @author Steffen Pingel
 */
public class TestCallback extends CloudFoundryCallback {

	private final String appName;

	private final String url;

	private final boolean deployStopped;

	public TestCallback(String appName, boolean deployStopped) {
		this.appName = appName;
		this.url = null;
		this.deployStopped = deployStopped;
	}

	public TestCallback(String appName, String url) {
		this.appName = appName;
		this.url = url;
		deployStopped = false;
	}

	@Override
	public void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// ignore
	}

	@Override
	public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
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
	public void prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		// NOTE: This section here is a substitute for the Application
		// Deployment wizard
		// where deployment info is modified by the user
		DeploymentInfoWorkingCopy copy = module.resolveDeploymentInfoWorkingCopy(monitor);
		copy.setDeploymentName(appName);
		copy.setMemory(CloudUtil.DEFAULT_MEMORY);
		copy.setDeploymentMode(deployStopped ? ApplicationAction.STOP : ApplicationAction.START);

		if (url != null) {
			copy.setUris(Collections.singletonList(url));
		}
		else {
			// Derive the URL from the app name specified in the test call back.
			// NOTE that although the working copy SHOULD have a default URL
			// generated from a default application name
			// (generally, the project name), since the appname and project name
			// can be different,
			// and such difference would be specified manually by the user in
			// the deployment wizard,
			// be sure to generate a URL from the actual app name specified in
			// this Test call back, to be sure
			// the URL is built off the app name rather than the project name,
			// as the test case may have specified
			// a different app name than the default app name from the project
			// name.

			ApplicationUrlLookupService urlLookup = ApplicationUrlLookupService.getCurrentLookup(server);
			urlLookup.refreshDomains(monitor);
			CloudApplicationURL url = urlLookup.getDefaultApplicationURL(copy.getDeploymentName());
			if (url != null) {
				copy.setUris(Arrays.asList(url.getUrl()));
			}
		}

		copy.save();
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
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		// ignore
	}

	@Override
	public void displayCaldecottTunnelConnections(CloudFoundryServer server, List<CaldecottTunnelDescriptor> descriptors) {
		// ignore
	}

	@Override
	public void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// TODO Auto-generated method stub
	}

}
