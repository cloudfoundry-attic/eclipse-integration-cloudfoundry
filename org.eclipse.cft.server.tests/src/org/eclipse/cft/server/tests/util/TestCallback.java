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
package org.eclipse.cft.server.tests.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryCallback;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentConfiguration;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * @author Steffen Pingel
 */
public class TestCallback extends CloudFoundryCallback {

	private final String appName;

	private final String url;

	private final int memory;

	private final boolean deployStopped;

	private List<EnvironmentVariable> variables;

	private List<CloudService> services;

	public TestCallback(String appName, int memory, boolean deployStopped, List<EnvironmentVariable> variables,
			List<CloudService> services) {
		this.appName = appName;
		this.url = null;
		this.deployStopped = deployStopped;
		this.memory = memory;
		this.variables = variables;
		this.services = services;
	}

	public TestCallback(String appName, int memory, boolean deployStopped) {
		this.appName = appName;
		this.url = null;
		this.deployStopped = deployStopped;
		this.memory = memory;
	}

	public TestCallback(String appName, String url, int memory) {
		this.appName = appName;
		this.url = url;
		deployStopped = false;
		this.memory = memory;
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
	public DeploymentConfiguration prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) throws CoreException {

		// NOTE:
		// This
		// section
		// here
		// is
		// a
		// substitute
		// for
		// the
		// Application

		DeploymentInfoWorkingCopy copy = module.resolveDeploymentInfoWorkingCopy(monitor);
		copy.setDeploymentName(appName);
		copy.setMemory(memory);
		if (variables != null) {
			copy.setEnvVariables(variables);
		}
		if (services != null) {
			copy.setServices(services);
		}

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

			ApplicationUrlLookupService urlLookup = ApplicationUrlLookupService.update(server, monitor);

			CloudApplicationURL url = urlLookup.getDefaultApplicationURL(copy.getDeploymentName());
			if (url != null) {
				copy.setUris(Arrays.asList(url.getUrl()));
			}
		}

		copy.save();

		ApplicationAction mode = deployStopped ? ApplicationAction.STOP : ApplicationAction.START;

		return new DeploymentConfiguration(mode);
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
	public void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// TODO Auto-generated method stub
	}

}
