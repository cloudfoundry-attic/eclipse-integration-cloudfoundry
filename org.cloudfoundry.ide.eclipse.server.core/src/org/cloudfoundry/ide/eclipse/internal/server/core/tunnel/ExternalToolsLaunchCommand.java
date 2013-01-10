/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.io.StringWriter;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.WaitWithProgressJob;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
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

public class ExternalToolsLaunchCommand {

	private final ServiceCommand serviceCommand;

	public ExternalToolsLaunchCommand(ServiceCommand serviceCommand) {
		this.serviceCommand = serviceCommand;
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

				// If a command needs to be launched in a terminal, the terminal
				// launch app is the actual executable for the process (it has
				// to be a file that exists).
				// the command executable + terminal options are ALL options for
				// the terminal launch app.
				String executable = null;
				StringWriter options = new StringWriter();
				if (serviceCommand.usesTerminal()) {
					CommandTerminal terminalCommand = serviceCommand.getCommandTerminal();
					executable = terminalCommand.getTerminalLaunchCommand();
					options.append(' ');
					options.append(serviceCommand.getExternalApplicationLaunchInfo().getExecutableName());

				}
				else {
					executable = serviceCommand.getExternalApplicationLaunchInfo().getExecutableName();
				}

				wc.setAttribute(IExternalToolConstants.ATTR_LOCATION, executable);

				if (serviceCommand.getOptions() != null && !serviceCommand.getOptions().isEmpty()) {
					options.append(' ');
					String appOptions = ServiceCommand.getSerialisedOptions(serviceCommand);
					options.append(appOptions);
				}

				if (options.getBuffer().length() > 0) {
					wc.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, options.toString());
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
					+ serviceCommand.getExternalApplicationLaunchInfo().getDisplayName());
		}

		return status;
	}

}
