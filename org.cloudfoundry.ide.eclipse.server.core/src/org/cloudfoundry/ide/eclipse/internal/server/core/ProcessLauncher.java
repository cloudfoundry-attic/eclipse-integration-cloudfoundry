/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Launches a native process that blocks the current thread it is launched from
 * until the process exists. Throws CoreException if errors are generated during
 * the launch process, or there are no arguments to the process.
 */
public abstract class ProcessLauncher {

	/**
	 * Returns when the process has exited without errors. This will wait for
	 * the process to exist, therefore will block the current thread in which it
	 * is running. If any errors occur, throws CoreException CoreException
	 * @throws CoreException if any errors occur while the process is being
	 * launched
	 */
	public void run() throws CoreException {
		Exception error = null;
		Process p = null;
		try {

			List<String> cmdArgs = getCommandArguments();

			if (cmdArgs == null || cmdArgs.isEmpty()) {
				throw new CoreException(getErrorStatus("No process arguments were found"));
			}

			p = new ProcessBuilder(cmdArgs).start();

			if (p == null) {
				throw new CoreException(getErrorStatus("No process was created."));
			}
			else {

				StringBuffer errorBuffer = new StringBuffer();
				try {
					// Clear the input and error streams to prevent the process
					// from blocking
					handleProcessIO(p, null, errorBuffer);
				}
				catch (CoreException e) {
					error = e;
				}
				// Handle errors after the process has exited
				p.waitFor();

				if (errorBuffer.length() > 0) {
					throw new CoreException(getErrorStatus(errorBuffer.toString()));
				}
				else if (p.exitValue() != 0) {
					throw new CoreException(getErrorStatus("process exit value: " + p.exitValue()));
				}
			}
		}
		catch (InterruptedException ex) {
			error = ex;
		}
		catch (IOException ioe) {
			error = ioe;
		}
		catch (SecurityException se) {
			error = se;
		}
		finally {
			if (p != null) {
				p.destroy();
			}
		}

		if (error != null) {
			throw error instanceof CoreException ? (CoreException) error : CloudUtil.toCoreException(error);
		}

	}

	/**
	 * The process IO needs to be handled in order to not block the process
	 * @param p
	 * @return
	 * @throws IOException
	 */
	protected void handleProcessIO(Process p, StringBuffer inputBuffer, StringBuffer errorBuffer) throws CoreException {

		InputStream in = p.getInputStream();
		InputStream error = p.getErrorStream();
		try {
			if (in != null) {

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line = reader.readLine();
				while (line != null) {
					if (inputBuffer != null) {
						inputBuffer.append(line);
						inputBuffer.append(' ');
					}
					line = reader.readLine();
				}
			}

			if (error != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(error));
				String line = reader.readLine();

				while (line != null) {
					if (errorBuffer != null) {
						errorBuffer.append(line);
						errorBuffer.append(' ');
					}
					line = reader.readLine();
				}
			}

		}
		catch (IOException ioe) {
			throw new CoreException(getErrorStatus("IO failure when handling process IO stream", ioe));
		}
		finally {
			if (in != null) {
				IOUtils.closeQuietly(in);
			}
			if (error != null) {
				IOUtils.closeQuietly(error);
			}
		}

	}

	protected IStatus getErrorStatus(String body) {
		return getErrorStatus(body, null);
	}

	protected IStatus getErrorStatus(String body, Exception e) {
		String errorMessage = "Failure when launching " + getLaunchName() + " due to: " + body;
		return e != null ? CloudFoundryPlugin.getErrorStatus(errorMessage, e) : CloudFoundryPlugin
				.getErrorStatus(errorMessage);
	}

	abstract protected String getLaunchName();

	abstract protected List<String> getCommandArguments() throws CoreException;
}
