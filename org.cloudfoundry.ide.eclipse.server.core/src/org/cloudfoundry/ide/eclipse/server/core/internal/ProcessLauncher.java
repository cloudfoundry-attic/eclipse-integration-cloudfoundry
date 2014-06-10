/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

			List<String> processArguments = getProcessArguments();

			if (processArguments == null || processArguments.isEmpty()) {
				throw new CoreException(getErrorStatus("No process arguments were found"));
			}
			else {

				ProcessBuilder processBuilder = new ProcessBuilder(processArguments);

				// Set any environment variables
				Map<String, String> envVars = getEnvironmentVariables();
				if (envVars != null) {
					Map<String, String> actualVars = processBuilder.environment();
					if (actualVars != null) {
						for (Entry<String, String> entry : envVars.entrySet()) {
							actualVars.put(entry.getKey(), entry.getValue());
						}
					}
				}

				p = processBuilder.start();

				if (p == null) {
					throw new CoreException(getErrorStatus("No process was created."));
				}
				else {

					StringBuffer errorBuffer = new StringBuffer();
					// Clear the input and error streams to prevent the
					// process
					// from blocking
					handleProcessIOAsynch(p, null, errorBuffer);

					p.waitFor();

					if (errorBuffer.length() > 0) {
						throw new CoreException(getErrorStatus(errorBuffer.toString()));
					}
					else if (p.exitValue() != 0) {
						throw new CoreException(getErrorStatus("process exit value: " + p.exitValue()));
					}
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
			throw error instanceof CoreException ? (CoreException) error : CloudErrorUtil.toCoreException(error);
		}

	}

	/**
	 * The process IO needs to be handled in order to not block the process.
	 * @param p
	 * @return
	 * @throws IOException
	 */
	protected void handleProcessIOAsynch(Process p, StringBuffer inputBuffer, StringBuffer errorBuffer) {

		InputStream in = p.getInputStream();
		InputStream error = p.getErrorStream();

		if (in != null) {
			new ProcessStreamHandler(in, inputBuffer, getLaunchName()).start();
		}

		if (error != null) {
			new ProcessStreamHandler(error, errorBuffer, getLaunchName()).start();
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

	abstract protected List<String> getProcessArguments() throws CoreException;

	abstract protected Map<String, String> getEnvironmentVariables() throws CoreException;

	protected static class ProcessStreamHandler {

		private final InputStream processInput;

		private final StringBuffer outputBuffer;

		private final String processName;

		public ProcessStreamHandler(InputStream processInput, StringBuffer outputBuffer, String processName) {
			this.processInput = processInput;
			this.outputBuffer = outputBuffer;
			this.processName = processName;
		}

		public void start() {
			BufferedReader reader = new BufferedReader(new InputStreamReader(processInput));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (outputBuffer != null) {
						outputBuffer.append(line);
						outputBuffer.append(' ');
					}
					line = reader.readLine();
				}
			}
			catch (IOException e) {
				CloudFoundryPlugin.logError("Error while reading input from process for: " + processName, e);
			}
			finally {
				if (processInput != null) {
					IOUtils.closeQuietly(processInput);
				}
			}
		}
	}

}
