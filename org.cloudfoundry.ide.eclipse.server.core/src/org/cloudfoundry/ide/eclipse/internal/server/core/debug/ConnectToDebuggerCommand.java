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
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

import java.util.List;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.WaitWithProgressJob;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IModule;

/**
 * Connects a running app in debug mode to the debugger. The app must be running
 * in debug mode. Does not start or restart the app if it is stopped.
 * 
 * Also keeps track of debugger launches per command, and does self-cleanup on
 * any debugger termination.
 * 
 * Users have the option of specifying an optional listener that handles
 * debugger termination events;
 * 
 * @author Nieraj Singh
 * 
 */
public class ConnectToDebuggerCommand extends DebugCommand {

	protected static final String DEBUG_JOB = "Connecting to debugger";

	private final CloudFoundryDebugConnection connection;

	/**
	 * Invoke this constructor if the connect to debugger command is part of a
	 * larger composite command and the application action type that it should
	 * react to for debug events overrides the connect to debugger action.
	 * @param server
	 * @param modules
	 * @param connection
	 * @param compositeAction
	 */
	public ConnectToDebuggerCommand(CloudFoundryServer server, IModule[] modules,
			CloudFoundryDebugConnection connection, ICloudFoundryDebuggerListener listener) {
		super(server, modules, ApplicationAction.CONNECT_TO_DEBUGGER, listener);
		this.connection = connection;
	}

	protected String getLaunchLabel(int instanceIndex) {
		CloudFoundryApplicationModule appModule = getApplicationModule();
		if (appModule != null) {

			StringBuilder launchLabel = new StringBuilder();
			launchLabel.append(getCloudFoundryServer().getServer().getName());
			launchLabel.append(" - ");
			launchLabel.append(appModule.getDeployedApplicationName());

			ApplicationStats stats = appModule.getApplicationStats();
			if (stats != null) {
				List<InstanceStats> instanceStats = stats.getRecords();
				if (instanceStats != null && instanceIndex < instanceStats.size()) {
					InstanceStats stat = instanceStats.get(instanceIndex);
					launchLabel.append(" - ");
					launchLabel.append(stat.getId());
				}
			}

			return launchLabel.toString();

		}
		return getApplicationID();

	}

	protected ILaunchConfiguration getLaunchConfiguration(String host, int port, int timeout, String appName,
			String launchName) {
		try {
			ILaunchConfigurationType launchConfigType = DebugPlugin.getDefault().getLaunchManager()
					.getLaunchConfigurationType(CloudFoundryDebuggingLaunchConfigDelegate.LAUNCH_CONFIGURATION_ID);
			if (launchConfigType != null) {

				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(appName);

				// Create the launch configuration, whether the project exists
				// or not, as there may
				// not be a local project associated with the deployed app
				ILaunchConfiguration configuration = launchConfigType.newInstance(project, launchName);
				ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();

				if (project != null && project.isAccessible()) {
					wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());

				}

				// Convert all to String to make it consistent when reading the
				// attributes later.
				wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.HOST_NAME, host);
				wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.PORT, port + "");
				wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.TIME_OUT, timeout + "");
				wc.setAttribute(CloudFoundryDebuggingLaunchConfigDelegate.DEBUGGER_CONNECTION_ID,
						getDebuggerConnectionIdentifier());
				configuration = wc.doSave();

				DebugUITools.setLaunchPerspective(launchConfigType, ILaunchManager.DEBUG_MODE,
						IDebugUIConstants.ID_DEBUG_PERSPECTIVE);

				return configuration;
			}

		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	protected IStatus connect(String debugIP, int debugPort, final IProgressMonitor monitor, String launchLabel) {

		final ILaunchConfiguration launchConfiguration = getLaunchConfiguration(debugIP, debugPort, 5000,
				getApplicationID(), launchLabel);
		boolean successful = false;
		IStatus status = Status.OK_STATUS;
		if (launchConfiguration != null) {
			try {
				Boolean result = new WaitWithProgressJob(5, 1000) {

					private boolean firstTry = true;

					protected boolean internalRunInWait(IProgressMonitor monitor) {
						// If it is the first try, wait first as it may take a
						// while
						// to connect to the JVM.

						if (!firstTry) {
							DebugUITools.launch(launchConfiguration, ILaunchManager.DEBUG_MODE);
							return true;
						}
						else {
							firstTry = false;
						}

						// Failed to connect. Continue retrying.
						return false;
					}

				}.run(monitor);

				successful = result != null ? result.booleanValue() : false;
			}
			catch (CoreException e) {
				successful = false;
				status = CloudFoundryPlugin.getErrorStatus(e);
			}
		}

		if (!successful && status == null) {
			status = CloudFoundryPlugin.getErrorStatus("Failed to connect to Cloud Foundry server - IP: " + debugIP
					+ " Port: " + debugPort + " Application: " + getApplicationID());
		}

		return status;

	}

	public String getCommandName() {
		return DEBUG_JOB;
	}

	protected void connect(IProgressMonitor monitor) {
		List<DebugConnectionDescriptor> descriptors = connection.getDebugConnectionDescriptors(monitor);
		if (descriptors != null) {

			for (int i = 0; i < descriptors.size(); ++i) {
				DebugConnectionDescriptor descriptor = descriptors.get(i);
				if (descriptor != null) {
					connect(descriptor.getIp(), descriptor.getPort(), monitor, getLaunchLabel(i));
				}
			}
		}
	}

}
