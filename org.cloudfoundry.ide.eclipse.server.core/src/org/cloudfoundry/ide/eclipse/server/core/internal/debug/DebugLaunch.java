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
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Creates a debug launch configuration.
 * @see ILaunchConfiguration
 */
public class DebugLaunch {

	private final CloudFoundryServer server;

	private final CloudFoundryApplicationModule appModule;

	private String debugConnectionID;

	private final IDebugProvider provider;

	private ILaunchConfigurationType launchConfigType;

	DebugLaunch(CloudFoundryServer server, CloudFoundryApplicationModule appModule, IDebugProvider provider) {
		this.server = server;
		this.appModule = appModule;
		this.provider = provider;
	}

	public boolean isDebugEnabled() {
		return provider.isDebugSupported(getApplicationModule(), getCloudFoundryServer());
	}

	public CloudFoundryServer getCloudFoundryServer() {
		return server;
	}

	public CloudFoundryApplicationModule getApplicationModule() {
		return appModule;
	}

	/**
	 * Determines if the application associated with the given module and server
	 * is connected to a debugger. Note that this does not check if the
	 * application is currently running in the server. It only checks if the
	 * application is connected to a debugger, although implicitly any
	 * application connected to a debugger is also running, but not vice-versa.
	 * 
	 * @return true if associated application is connected to a debugger. False
	 * otherwise
	 */
	public boolean isConnectedToDebugger() {
		String id = getDebuggerConnectionIdentifier();
		return DebugOperations.getActiveLaunch(id) != null;
	}

	/**
	 * Computes a new debugger connection identifier based on the given server
	 * and modules.
	 * @return non-null debugger connection identifier
	 */
	public String getDebuggerConnectionIdentifier() {

		if (debugConnectionID == null) {
			StringBuilder idBuffer = new StringBuilder();

			idBuffer.append(getCloudFoundryServer().getUrl());
			idBuffer.append(getCloudFoundryServer().getUsername());
			idBuffer.append(getApplicationModule().getDeployedApplicationName());
			debugConnectionID = idBuffer.toString();
		}
		return debugConnectionID;

	}

	protected String getLaunchLabel() {
		StringBuilder launchLabel = new StringBuilder();
		launchLabel.append(getCloudFoundryServer().getServer().getName());
		launchLabel.append(" - "); //$NON-NLS-1$
		launchLabel.append(getApplicationModule().getDeployedApplicationName());
		return launchLabel.toString();
	}

	/**
	 * Returns a non-null launch configuration, or throws {@link CoreException}
	 * if it failed to resolve a launch configuration
	 * 
	 * @param monitor
	 * @return Non-null launch configuration.
	 * @throws CoreException if error occurred while resolving launch
	 * configuration
	 */
	public ILaunchConfiguration resolveLaunchConfiguration(IProgressMonitor monitor) throws CoreException {

		DebugConnectionDescriptor connectionDescriptor = resolveDescriptor(monitor);

		return resolveLaunchConfiguration(connectionDescriptor.getIp(), connectionDescriptor.getPort(), 60000);
	}

	/**
	 * 
	 * @param host
	 * @param port
	 * @param timeout
	 * @param appName
	 * @param launchName
	 * @return non-null launch configuration to debug the given application
	 * name.
	 * @throws CoreException if unable to resolve launch configuration.
	 */
	protected ILaunchConfiguration resolveLaunchConfiguration(String host, int port, int timeout) throws CoreException {

		String launchLabel = getLaunchLabel();

		String configID = provider.getLaunchConfigurationID() != null ? provider.getLaunchConfigurationID()
				: CloudFoundryDebuggingLaunchConfigDelegate.LAUNCH_CONFIGURATION_ID;
		launchConfigType = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(configID);

		if (launchConfigType != null) {

			IProject project = getApplicationModule().getLocalModule().getProject();

			// Create the launch configuration, whether the project exists
			// or not, as there may
			// not be a local project associated with the deployed app
			ILaunchConfiguration launchConfiguration = launchConfigType.newInstance(project, launchLabel);
			ILaunchConfigurationWorkingCopy wc = launchConfiguration.getWorkingCopy();

			if (project != null && project.isAccessible()) {
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			}

			// Convert all to String to make it consistent when reading the
			// attributes later.
			wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.HOST_NAME, host);
			wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.PORT, port + ""); //$NON-NLS-1$
			wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.TIME_OUT, timeout + ""); //$NON-NLS-1$
			wc.setAttribute(DebugOperations.CLOUD_DEBUG_LAUNCH_ID, getDebuggerConnectionIdentifier());
			wc.setAttribute(DebugOperations.CLOUD_DEBUG_SERVER, getCloudFoundryServer().getServerId());
			wc.setAttribute(DebugOperations.CLOUD_DEBUG_APP_NAME, getApplicationModule().getDeployedApplicationName());

			return launchConfiguration = wc.doSave();

		}
		else {
			throw CloudErrorUtil
					.toCoreException("No debug launch configuration found for - " + provider.getLaunchConfigurationID()); //$NON-NLS-1$
		}

	}

	public boolean configure(final IProgressMonitor monitor) throws CoreException {
		return provider.configureApp(getApplicationModule(), getCloudFoundryServer(), monitor);
	}

	protected DebugConnectionDescriptor resolveDescriptor(IProgressMonitor monitor) throws CoreException {

		DebugConnectionDescriptor descriptor = null;
		Throwable error = null;
		try {
			descriptor = provider
					.getDebugConnectionDescriptor(getApplicationModule(), getCloudFoundryServer(), monitor);
		}
		catch (Throwable t) {
			error = t;
		}
		if (descriptor == null || !descriptor.areValidIPandPort()) {
			throw CloudErrorUtil
					.toCoreException(
							"Failed to connect debugger to Cloud application - Timed out resolving port and IP for application: " + getApplicationModule().getDeployedApplicationName(), error); //$NON-NLS-1$
		}
		return descriptor;
	}

}
