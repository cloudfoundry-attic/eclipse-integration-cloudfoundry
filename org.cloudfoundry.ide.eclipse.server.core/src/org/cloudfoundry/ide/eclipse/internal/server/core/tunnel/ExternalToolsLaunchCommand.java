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
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.WaitWithProgressJob;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryDebuggingLaunchConfigDelegate;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
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

public class ExternalToolsLaunchCommand {

	private final ServiceCommand serviceCommand;

	private final CaldecottTunnelDescriptor descriptor;

	public ExternalToolsLaunchCommand(ServiceCommand serviceCommand, CaldecottTunnelDescriptor descriptor) {
		this.serviceCommand = serviceCommand;
		this.descriptor = descriptor;
	}

	protected String getLaunchName() {
		return serviceCommand.getExternalApplicationLaunchInfo().getDisplayName();
	}

	protected ILaunchConfiguration getLaunchConfiguration() {
		try {
			ILaunchConfigurationType launchConfigType = DebugPlugin.getDefault().getLaunchManager()
					.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
			if (launchConfigType != null) {

				// No project associated with external app launch, as the app is
				// associated with
				// a Cloud Foundry service, not an CF app.
				ILaunchConfiguration configuration = launchConfigType.newInstance(null, getLaunchName());
				ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();

				// Convert all to String to make it consistent when reading the
				// attributes later.
				wc.setAttribute(IExternalToolConstants.ATTR_LOCATION, serviceCommand.getExternalApplicationLaunchInfo()
						.getExecutableName());

				if (serviceCommand.getOptions() != null) {
					wc.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, serviceCommand.getOptions()
							.getOptions());

				}
				configuration = wc.doSave();

				return configuration;
			}

		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	public IStatus run(IProgressMonitor monitor) {
		final ILaunchConfiguration launchConfiguration = getLaunchConfiguration();
		boolean successful = false;
		IStatus status = Status.OK_STATUS;
		if (launchConfiguration != null) {
			try {
				Boolean result = new WaitWithProgressJob(5, 1000) {

					private boolean firstTry = true;

					protected boolean internalRunInWait(IProgressMonitor monitor) {

						if (!firstTry) {
							DebugUITools.launch(launchConfiguration, ILaunchManager.RUN_MODE);
							return true;
						}
						else {
							firstTry = false;
						}

						// Failed to connect. Continue retrying.
						return false;
					}

				}.run(monitor);

				successful = result.booleanValue();
			}
			catch (CoreException e) {
				successful = false;
				status = CloudFoundryPlugin.getErrorStatus(e);
			}
		}

		if (!successful && status == null) {
			status = CloudFoundryPlugin.getErrorStatus("Failed to launch external tool: "
					+ serviceCommand.getExternalApplicationLaunchInfo().getDisplayName()
					+ " for the following service: " + serviceCommand.getServiceInfo().getServiceName());
		}

		return status;
	}

}
