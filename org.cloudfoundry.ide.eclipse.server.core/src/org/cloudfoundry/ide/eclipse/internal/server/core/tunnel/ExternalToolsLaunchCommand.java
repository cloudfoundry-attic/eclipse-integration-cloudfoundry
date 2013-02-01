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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.PlatformUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.ProcessLauncher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class ExternalToolsLaunchCommand {

	private final ServiceCommand serviceCommand;

	public ExternalToolsLaunchCommand(ServiceCommand serviceCommand) {
		this.serviceCommand = serviceCommand;
	}

	protected String getLaunchName() {
		return serviceCommand.getDisplayName();
	}

	public IStatus run(IProgressMonitor monitor) {

		IStatus status = Status.OK_STATUS;
		try {

			final List<String> cmdArguments = new ArrayList<String>();

			String appOptions = serviceCommand.getOptions() != null ? serviceCommand.getOptions().getOptions() : null;

			CommandTerminal terminalCommand = serviceCommand.getCommandTerminal();

			// First add the terminal as the initial arguments to the process
			if (terminalCommand != null) {
				String terminalCommandValue = terminalCommand.getTerminal();
				
				StringBuffer value = new StringBuffer(terminalCommandValue);
				StringBuffer buffer = new StringBuffer();
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
						cmdArguments.add(buffer.toString());
						buffer = null;
					}

				}
			}

			// Check platform , as some require a script file to be created

			if (terminalCommand != null && Platform.OS_MACOSX.equals(PlatformUtil.getOS())) {

				// For launching Mac OS Terminal, the Terminal.app does not take
				// arguments for the application that is being launched. It only
				// takes
				// in a file name, therefore the external application that
				// should be
				// launched in Terminal.app needs to be converted to a script
				// file.
				StringWriter optionsWr = new StringWriter();

				optionsWr.append(serviceCommand.getExternalApplication().getExecutableNameAndPath());

				if (appOptions != null) {
					optionsWr.append(' ');
					optionsWr.append(appOptions);
				}
				File scriptFile = getScriptFile(optionsWr.toString());
				if (scriptFile != null && scriptFile.exists()) {
					cmdArguments.add(scriptFile.getAbsolutePath());
				}
				else {
					throw new CoreException(CloudFoundryPlugin.getErrorStatus("Failed to create script file for: "
							+ serviceCommand.getDisplayName()));
				}

			}
			else {
				// Otherwise if not using a terminal, or if using a non-Mac
				// terminal, append
				// the application and it's options as process arguments
				cmdArguments.add(serviceCommand.getExternalApplication().getExecutableNameAndPath());
				if (appOptions != null) {
					cmdArguments.add(appOptions);
				}
			}

			new ProcessLauncher() {
				protected String getLaunchName() {
					return serviceCommand.getDisplayName();
				}

				protected List<String> getCommandArguments() throws CoreException {
					return cmdArguments;
				}

			}.run();
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
			return CloudFoundryPlugin.getErrorStatus(e);
		}

		return status;
	}

	/**
	 * Creates a script file containing the specified command that exists in the
	 * file system. Returns null if the file wasn't created.
	 * @param options
	 * @return
	 * @throws CoreException
	 */
	protected File getScriptFile(String commandwithOptions) throws CoreException {

		if (commandwithOptions == null || commandwithOptions.length() == 0) {
			return null;
		}
		File scriptFile = null;
		FileOutputStream outStream = null;

		try {
			scriptFile = CloudUtil.createTemporaryFile("tempScriptFileCFTunnelCommands", "tunnelCommand.sh");
			if (scriptFile != null && scriptFile.exists()) {

				outStream = new FileOutputStream(scriptFile);
				OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

				BufferedWriter writer = new BufferedWriter(outWriter);
				writer.write(commandwithOptions);
				writer.flush();
				new FilePermissionChangeProcess(scriptFile, serviceCommand.getDisplayName()).run();
			}
		}
		catch (IOException ioe) {
			throw CloudUtil.toCoreException(ioe);
		}
		finally {
			if (outStream != null) {
				IOUtils.closeQuietly(outStream);
			}
		}

		return scriptFile;
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
		protected List<String> getCommandArguments() throws CoreException {
			if (file == null || !file.exists()) {
				return null;
			}

			List<String> permissionCommand = new ArrayList<String>();

			permissionCommand.add("chmod");
			permissionCommand.add("+x");
			permissionCommand.add(file.getAbsolutePath());
			return permissionCommand;
		}

	}

}
