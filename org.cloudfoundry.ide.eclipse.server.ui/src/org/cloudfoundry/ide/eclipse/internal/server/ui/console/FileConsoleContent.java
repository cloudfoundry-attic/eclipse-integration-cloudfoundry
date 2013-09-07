package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.springframework.http.HttpStatus;

/**
 * Streams file content to the Cloud Foundry console. It continues to check for
 * new content indefinitely, until the Cloud Foundry manager decided to
 * terminate any further streaming (e.g., application is deleted or stopped, or
 * enough errors have been encountered)
 */
public class FileConsoleContent implements IConsoleContent {

	private final String path;

	private int swtColour;

	private final String appName;

	private final int instanceIndex;

	private final CloudFoundryServer server;

	/**
	 * 
	 * @param path relative path of content resource, relative to the
	 * application in the remove server
	 * @param swtColour valid constants would be something like SWT.COLOR_RED.
	 * Use -1 to use default console colour.
	 * @param server must not be null. Server where contents should be fetched.
	 * @param appName must not be null
	 * @param instanceIndex must be valid and greater than -1.
	 */
	public FileConsoleContent(String path, int swtColour, CloudFoundryServer server, String appName, int instanceIndex) {
		this.path = path;

		this.swtColour = swtColour;
		this.server = server;
		this.appName = appName;
		this.instanceIndex = instanceIndex;

	}

	public ICloudFoundryConsoleOutputStream getOutputStream(IOConsoleOutputStream outStream) {
		FileConsoleOutputStream cfOutStream = new FileConsoleOutputStream(outStream, path, server, swtColour, appName,
				instanceIndex);
		cfOutStream.initialiseStream();
		return cfOutStream;
	}

	public static class FileConsoleOutputStream extends ConsoleOutputStream {

		private final String path;

		protected int offset = 0;

		private final int swtColour;

		private final String appName;

		private final int instanceIndex;

		private String id;

		private static final int MAX_COUNT = 50;

		private int errorCount = MAX_COUNT;

		/**
		 * 
		 * @param path file path to be fetched from the server.
		 * @param isError true if the output should be sent to the console error
		 * output stream.
		 * @param server
		 * @param swtColour use -1 to use default console colour
		 */
		public FileConsoleOutputStream(IOConsoleOutputStream outStream, String path, CloudFoundryServer server,
				int swtColour, String appName, int instanceIndex) {
			super(outStream, server);
			this.path = path;
			this.swtColour = swtColour;
			this.appName = appName;
			this.instanceIndex = instanceIndex;

			this.id = appName + " - " + instanceIndex + " - " + path;

		}

		protected int getStreamColour() {
			return swtColour;
		}

		protected String getContent(IProgressMonitor monitor) throws CoreException {

			String content = null;
			CloudFoundryException cfe = null;
			CoreException error = null;
			try {

				content = server.getBehaviour().getFile(appName, instanceIndex, path, offset, monitor);
				if (content != null) {
					offset += content.length();
				}
				// NOte that if no error was thrown, reset the error count. The
				// stream should only terminate if N number of errors are met in
				// a row.
				errorCount = MAX_COUNT;
				return content;
			}
			catch (CoreException e) {
				error = e;
				Throwable t = e.getCause();

				if (t instanceof CloudFoundryException) {
					cfe = (CloudFoundryException) t;
				}
			}
			catch (CloudFoundryException cfex) {
				error = new CoreException(CloudFoundryPlugin.getErrorStatus(cfe));
				cfe = cfex;
			}

			// Do not log error if is is due to range not satisfied, or file is
			// not
			// found for instance
			if (cfe != null
					&& (HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(cfe.getStatusCode()) || CloudErrorUtil
							.isFileNotFoundForInstance(cfe))) {
				// These types of errors are "valid" meaning they don't indicate
				// a problem. Return null to let the caller know that there is
				// no further content at the moment.
				return null;

			}

			if (adjustCount()) {
				throw error;
			}
			else {
				return null;
			}

		}

		public String getID() {
			return id;
		}

		protected boolean adjustCount() {
			// Otherwise an error occurred
			errorCount--;
			return errorCount == 0;
		}
	}

}
