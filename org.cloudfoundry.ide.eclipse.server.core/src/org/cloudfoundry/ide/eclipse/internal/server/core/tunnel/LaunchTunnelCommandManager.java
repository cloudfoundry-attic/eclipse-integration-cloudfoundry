/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.PlatformUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.ProcessLauncher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

/**
 * 
 * Executes commands for launching external applications. This will create a
 * separate process where the external application are launched. Handling the
 * external application may vary depending on the underlying operating system.
 * 
 */
public class LaunchTunnelCommandManager {

	private final ServiceCommand serviceCommand;

	public LaunchTunnelCommandManager(ServiceCommand serviceCommand) {
		this.serviceCommand = serviceCommand;
	}

	protected String getLaunchName() {
		return serviceCommand.getDisplayName();
	}

	/**
	 * Throws CoreException if an error occurred while launching the external
	 * application.
	 * @param monitor
	 */
	public void run(IProgressMonitor monitor) throws CoreException {

		// For certain OSs, script files are create that contain the application
		// and options.
		File scriptFile = null;

		String appOptions = serviceCommand.getOptions() != null ? serviceCommand.getOptions().getOptions() : null;

		CommandTerminal terminalCommand = serviceCommand.getCommandTerminal();

		final List<String> processArguments = new ArrayList<String>();
		final Map<String, String> envVars = getEnvironmentVariables();

		try {

			if (terminalCommand != null) {

				// Parse the terminal, application, and its options into
				// separate
				// process arguments, based on the platform.

				// First add the terminal as the initial arguments to the
				// process.
				// Each terminal component needs
				// to be added as a separate process argument, and has to be
				// trimmed
				// of any leading/trailing whitespaces
				String terminalCommandValue = terminalCommand.getTerminal();
				List<String> terminalElements = parseElement(terminalCommandValue);
				processArguments.addAll(terminalElements);

				// If using a terminal, the application that is launched by a
				// terminal needs to be handled differently for
				// Mac OS X and Windows.

				if (Platform.OS_MACOSX.equals(PlatformUtil.getOS())) {
					// For launching Mac OS Terminal, the Terminal.app does not
					// take
					// arguments for the application that is being launched. It
					// only
					// takes
					// in a file name, therefore the external application that
					// should be
					// launched in Terminal.app needs to be converted to a
					// script
					// file.
					StringWriter optionsWr = new StringWriter();

					// For Mac OS, any environment variables should be
					// added as part of the script
					if (serviceCommand.getEnvironmentVariables() != null) {
						for (EnvironmentVariable var : serviceCommand.getEnvironmentVariables()) {
							optionsWr.append("export ");
							optionsWr.append(var.getVariable());
							optionsWr.append("=");
							optionsWr.append(var.getValue());
							optionsWr.append('\n');
						}
					}

					optionsWr.append(serviceCommand.getExternalApplication().getExecutableNameAndPath());

					if (appOptions != null) {
						optionsWr.append(' ');
						optionsWr.append(appOptions);
					}

					scriptFile = getScriptFile(optionsWr.toString());
					processArguments.add(scriptFile.getAbsolutePath());
				}
				else if (Platform.OS_WIN32.equals(PlatformUtil.getOS())) {

					// For Windows, in order for the process launcher to correct
					// handle the launch argument for the cmd.exe terminal, both
					// the
					// application and the
					// options need to be in ONE argument.
					StringBuffer windowsArgument = new StringBuffer();

					// 1. Surround the application file location with quotes, as
					// any
					// path value with spaces needs to be surrounded with quotes
					// in
					// Windows

					// This leading leading space is needed for any process
					// argument
					// that starts with a quote. The process launcher
					// checks whether the process argument starts with a quote,
					// and
					// if it does, it expects it to end with a quote. However
					// The options for the applications are not surrounded by
					// quotes, therefore to avoid this quote check, start with a
					// leading whitespace
					windowsArgument.append(' ');

					windowsArgument.append('"');
					windowsArgument.append(serviceCommand.getExternalApplication().getExecutableNameAndPath());
					windowsArgument.append('"');

					// 2. The options for the application and the application
					// location both need to be in ONE argument. However, the
					// options are not surrounded by quotes, and need to be
					// separated from the application by a whitespace
					if (appOptions != null) {
						windowsArgument.append(' ');
						windowsArgument.append(appOptions);
					}

					processArguments.add(windowsArgument.toString());

				}
				else {
					// For Linux, pass the application and its options in one
					// process argument
					String processArg = serviceCommand.getExternalApplication().getExecutableNameAndPath();

					if (appOptions != null) {
						processArg += " " + appOptions;
					}

					processArguments.add(processArg);
				}
			}
			else {
				// Otherwise no terminal is being used so parse each option
				// element into separate process arguments
				processArguments.add(serviceCommand.getExternalApplication().getExecutableNameAndPath());
				if (appOptions != null) {
					List<String> optionElements = parseElement(appOptions);
					processArguments.addAll(optionElements);
				}
			}
			launch(processArguments, envVars);

		}
		finally {

			if (scriptFile != null && scriptFile.exists()) {
				scriptFile.deleteOnExit();
			}
		}
	}

	protected Map<String, String> getEnvironmentVariables() {
		List<EnvironmentVariable> envVars = serviceCommand.getEnvironmentVariables();
		Map<String, String> vars = null;
		if (envVars != null && !envVars.isEmpty()) {
			vars = new HashMap<String, String>();
			for (EnvironmentVariable var : envVars) {
				vars.put(var.getVariable(), var.getValue());
			}
		}

		return vars;
	}

	protected void launch(final List<String> processArguments, final Map<String, String> enVars) throws CoreException {
		// If there are process arguments, launch the process
		if (!processArguments.isEmpty()) {

			// Launch the process from the same thread
			new ProcessLauncher() {
				protected String getLaunchName() {
					return serviceCommand.getDisplayName();
				}

				protected List<String> getProcessArguments() throws CoreException {
					return processArguments;
				}

				@Override
				protected Map<String, String> getEnvironmentVariables() throws CoreException {
					return enVars;
				}

			}.run();
		}
		else {
			throw new CoreException(
					CloudFoundryPlugin
							.getErrorStatus("Unable to launch process because no process arguments were resolved when launching process for "
									+ getLaunchName()));
		}
	}

	protected List<String> parseElement(String value) {

		StringBuffer buffer = new StringBuffer();
		List<String> elements = new ArrayList<String>();
		for (int i = 0; i < value.length(); i++) {

			if (!Character.isWhitespace(value.charAt(i))) {
				if (buffer == null) {
					buffer = new StringBuffer();
				}
				buffer.append(value.charAt(i));
			}
			// Flush the buffer on a whitespace char or the last
			// character
			if ((Character.isWhitespace(value.charAt(i)) || i == value.length() - 1) && buffer != null
					&& buffer.length() > 0) {
				elements.add(buffer.toString());
				buffer = null;
			}

		}
		return elements;
	}

	/**
	 * Creates a script file containing the specified command. Throws exception
	 * if it failed to create a File.
	 * @param options must not be null
	 * @return created script file
	 * @throws CoreException if failed to create script file
	 */
	protected File getScriptFile(String commandwithOptions) throws CoreException {

		FileOutputStream outStream = null;

		try {

			File scriptFile = CloudUtil.createTemporaryFile("tempScriptFileCFTunnelCommands", "tunnelCommand.sh");
			if (scriptFile != null && scriptFile.exists()) {

				outStream = new FileOutputStream(scriptFile);
				OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

				BufferedWriter writer = new BufferedWriter(outWriter);
				writer.write(commandwithOptions);
				writer.flush();
				new FilePermissionChangeProcess(scriptFile, serviceCommand.getDisplayName()).run();
				return scriptFile;
			}
			else {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus("Failed to create script file for: "
						+ serviceCommand.getDisplayName()));
			}
		}
		catch (IOException ioe) {
			throw CloudErrorUtil.toCoreException(ioe);
		}
		finally {
			if (outStream != null) {
				IOUtils.closeQuietly(outStream);
			}
		}

	}

	static class FilePermissionChangeProcess extends ProcessLauncher {

		private final File file;

		private final String launchName;

		public FilePermissionChangeProcess(File file, String launchName) {
			this.file = file;
			this.launchName = launchName;
		}

		@Override
		protected String getLaunchName() {
			return launchName;
		}

		@Override
		protected List<String> getProcessArguments() throws CoreException {
			if (file == null || !file.exists()) {
				return null;
			}

			List<String> permissionCommand = new ArrayList<String>();

			permissionCommand.add("chmod");
			permissionCommand.add("+x");
			permissionCommand.add(file.getAbsolutePath());
			return permissionCommand;
		}

		@Override
		protected Map<String, String> getEnvironmentVariables() throws CoreException {
			return null;
		}

	}

}
