/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.springframework.http.HttpStatus;

/**
 * Streams application staging content to the console, as long as any staging
 * content is available. As soon as staging content is no longer available,
 * including due to errors, the console content will terminate any further
 * output to the console.
 * 
 */
public class StagingLogConsoleContent implements IConsoleContent {

	private final CloudFoundryServer server;

	private final StartingInfo startingInfo;

	public StagingLogConsoleContent(StartingInfo startingInfo, CloudFoundryServer server) {
		this.server = server;
		this.startingInfo = startingInfo;
	}

	public ICloudFoundryConsoleOutputStream getOutputStream(IOConsoleOutputStream consoleOutputStream) {
		StagingLogConsoleOutputStream cfStream = new StagingLogConsoleOutputStream(consoleOutputStream, startingInfo,
				server);
		cfStream.initialiseStream();
		return cfStream;
	}

	public static class StagingLogConsoleOutputStream extends ConsoleOutputStream {

		private final StartingInfo startingInfo;

		private int offset = 0;

		/**
		 * 
		 * @param path file path to be fetched from the server.
		 * @param isError true if the output should be sent to the console error
		 * output stream.
		 * @param server
		 * @param swtColour use -1 to use default console colour
		 */
		public StagingLogConsoleOutputStream(IOConsoleOutputStream outStream, StartingInfo startingInfo,
				CloudFoundryServer server) {
			super(outStream, server);
			this.startingInfo = startingInfo;

		}

		protected int getStreamColour() {
			return SWT.COLOR_DARK_GREEN;
		}

		protected String getContent(IProgressMonitor monitor) throws CoreException {

			CloudFoundryException cfe = null;
			CoreException error = null;
			try {

				// Once null is received, it means no more staging logs are
				// available, so operation should now halt.
				String content = server.getBehaviour().getStagingLogs(startingInfo, offset, monitor);
				if (content != null) {
					offset += content.length();
					return content;
				}

			}
			catch (CoreException e) {
				error = e;
				Throwable t = e.getCause();

				if (t instanceof CloudFoundryException) {
					cfe = (CloudFoundryException) t;
				}
			}
			catch (CloudFoundryException cfex) {
				cfe = cfex;
			}

			// Reaching here means some error condition was encountered,
			// therefore close the stream.
			requestStreamClose(true);

			// Do not log error if file is not found for instance
			if (cfe != null && !HttpStatus.NOT_FOUND.equals(cfe.getStatusCode())) {
				throw CloudErrorUtil.toCoreException(cfe);
			}
			else if (error != null) {
				throw error;
			}
			return null;
		}
	}

}
