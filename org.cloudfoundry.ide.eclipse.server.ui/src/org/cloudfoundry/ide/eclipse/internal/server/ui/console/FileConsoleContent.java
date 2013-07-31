package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.springframework.http.HttpStatus;

public class FileConsoleContent implements IConsoleContent {

	private final String path;

	private int swtColour;

	private final String appName;

	private final int instanceIndex;

	private CloudFoundryServer server;

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
		ICloudFoundryConsoleOutputStream cfOutStream = new FileConsoleOutputStream(outStream, path, server, swtColour,
				appName, instanceIndex);
		if (cfOutStream instanceof AbstractConsoleOutputStream) {
			((AbstractConsoleOutputStream) cfOutStream).initialiseStream();
		}
		return cfOutStream;
	}

	public static class FileConsoleOutputStream extends AbstractConsoleOutputStream {

		private final String path;

		protected int offset = 0;

		private final int swtColour;

		private final String appName;

		private final int instanceIndex;

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

		}

		protected int getStreamColour() {
			return swtColour;
		}

		protected String getContent(IProgressMonitor monitor) throws CoreException {

			String content = null;
			CloudFoundryException cfe = null;
			try {

				content = server.getBehaviour().getFile(appName, instanceIndex, path, offset, monitor);
				if (content != null) {
					offset += content.length();
				}

			}
			catch (CoreException e) {
				Throwable t = e.getCause();

				if (t instanceof CloudFoundryException) {
					cfe = (CloudFoundryException) t;
				}
				else {
					throw e;
				}

			}
			catch (CloudFoundryException cfex) {
				cfe = cfex;
			}

			// Do not log error if is is due to range not satisfied, or file is
			// not
			// found for instance
			if (cfe != null && !HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(cfe.getStatusCode())
					&& !CloudErrorUtil.isFileNotFoundForInstance(cfe)) {
				throw CloudErrorUtil.toCoreException(cfe);

			}
			return content;
		}

	}

}
