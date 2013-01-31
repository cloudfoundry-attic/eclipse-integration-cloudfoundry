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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.PlatformUtil;
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
		launchExternalCommand();

		return status;
	}

	protected boolean addExternalToolCommand(List<String> processCommands) throws IOException {
		String appOptions = serviceCommand.getOptions() != null ? serviceCommand.getOptions().getOptions() : null;
		if (Platform.OS_MACOSX.equals(PlatformUtil.getOS())) {
			// For launching Mac OS Terminal, the Terminal.app does not take
			// arguments for the application that is being launched. It only
			// takes
			// in a file name, therefore the external application that should be
			// launched in Terminal.app needs to be converted to a script file.
			StringWriter optionsWr = new StringWriter();

			optionsWr.append(serviceCommand.getExternalApplication().getExecutableNameAndPath());

			if (appOptions != null) {
				optionsWr.append(' ');
				optionsWr.append(appOptions);
			}
			File scriptFile = getScriptFile(optionsWr.toString());
			if (scriptFile != null && scriptFile.exists()) {
				processCommands.add(scriptFile.getAbsolutePath());
				return true;
			}

		}
		else {
			// Otherwise just append the external application and its arguments
			// directly to the process command
			processCommands.add(serviceCommand.getExternalApplication().getExecutableNameAndPath());
			if (appOptions != null) {
				processCommands.add(appOptions);
			}
			return true;
		}
		return false;
	}

	protected File getScriptFile(String options) throws IOException {

		if (options == null || options.length() == 0) {
			return null;
		}
		File scriptFile = getTempScriptFile();
		if (scriptFile != null && scriptFile.exists()) {

			FileOutputStream outStream = null;
			try {
				outStream = new FileOutputStream(scriptFile);
				OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

				BufferedWriter writer = new BufferedWriter(outWriter);
				writer.write(options);
				writer.flush();

				if (!changePermission(scriptFile)) {
					CloudFoundryPlugin.logError("Failed to make script : " + scriptFile.getAbsolutePath()
							+ " executable. Unable to launch external tools.");
					return null;
				}

			}
			catch (IOException e) {
				CloudFoundryPlugin.logError(e);
			}
			finally {
				if (outStream != null) {
					IOUtils.closeQuietly(outStream);
				}
			}

		}
		return scriptFile;
	}

	protected List<String> handleProcessInput(Process p) throws IOException {
		List<String> lines;
		InputStream in = p.getInputStream();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			lines = new ArrayList<String>();
			while (line != null) {

				lines.add(line);
				line = reader.readLine();
			}
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		return lines;
	}

	/**
	 * 
	 * @param file
	 * @return true iff file exists and permission successfully changed. False
	 * otherwise.
	 */
	protected boolean changePermission(File file) throws IOException {

		if (file == null || !file.exists()) {
			return false;
		}

		List<String> permissionCommand = new ArrayList<String>();

		permissionCommand.add("chmod");
		permissionCommand.add("+x");
		permissionCommand.add(file.getAbsolutePath());

		ProcessBuilder builder = new ProcessBuilder(permissionCommand);
		Process p = builder.start();

		if (p == null) {
			return false;
		}

		handleProcessInput(p);

		// Keep the input stream flushed as to not block the process

		try {
			// Wait for process to finish
			p.waitFor();

			if (p.exitValue() != 0) {
				CloudFoundryPlugin.logError("Failed to change script file permission to executable due to: "
						+ p.exitValue());
				return false;
			}
			else {
				return true;
			}
		}
		catch (InterruptedException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;

	}

	protected void launchExternalCommand() {
		if (serviceCommand.getCommandTerminal() != null) {

			Process p = null;
			try {

				// Now launch the application in Terminal
				CommandTerminal terminalCommand = serviceCommand.getCommandTerminal();

				String terminalCommandValue = terminalCommand.getTerminal();

				String[] terminalCommands = terminalCommandValue.split(" ");
				List<String> cmdArgs = new ArrayList<String>();

				for (String terminalCom : terminalCommands) {
					cmdArgs.add(terminalCom);
				}

				if (!addExternalToolCommand(cmdArgs)) {
					CloudFoundryPlugin.logError("Failed to add external tool to process command");
					return;

				}
				p = new ProcessBuilder(cmdArgs).start();

				if (p == null) {
					CloudFoundryPlugin.logError("Failed to launch "
							+ serviceCommand.getExternalApplication().getExecutableNameAndPath()
							+ ". No process was created.");
				}
				else {

					handleProcessInput(p);

					p.waitFor();

					if (p.exitValue() != 0) {

						CloudFoundryPlugin.logError("Failed to launch external tool in process due to: "
								+ p.exitValue());
					}
				}
			}
			catch (InterruptedException ex) {
				CloudFoundryPlugin.logError("Command line threw an InterruptedException ", ex);
			}
			catch (IOException ioe) {

			}
			finally {

				if (p != null) {
					p.destroy();
				}
			}
		}
	}

	private static File getTempScriptFile() {
		File targetFile = null;
		try {
			File tempFolder = File.createTempFile("tempScriptFileCFTunnelCommands", null);
			tempFolder.setExecutable(true);
			tempFolder.delete();
			tempFolder.mkdirs();
			targetFile = new File(tempFolder, "tunnelCommand.sh");

			// delete existing files
			targetFile.delete();
			targetFile.setExecutable(true);
			targetFile.setWritable(true);
			targetFile.createNewFile();
		}
		catch (IOException e) {
			CloudFoundryPlugin.logError(e);
		}

		return targetFile;
	}

}
