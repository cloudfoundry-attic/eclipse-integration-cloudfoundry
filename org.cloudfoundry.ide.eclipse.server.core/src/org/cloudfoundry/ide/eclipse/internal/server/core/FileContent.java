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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.IOException;
import java.io.OutputStream;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springframework.http.HttpStatus;

/**
 * Content of a file in a remote Cloud server, whose contents should be streamed
 * to the console.
 */
public class FileContent {

	private final String path;

	private final boolean shouldWaitForFile;

	private final boolean isError;

	private final CloudFoundryServer server;

	public static final String STD_OUT_LOG = "logs/stdout.log";

	public static final String STD_ERROR_LOG = "logs/stderr.log";

	public FileContent(String path, boolean isError, CloudFoundryServer server) {
		this(path, isError, server, false);
	}

	public FileContent(String path, boolean isError, CloudFoundryServer server, boolean shouldWaitForFile) {
		this.path = path;
		this.isError = isError;
		this.server = server;
		this.shouldWaitForFile = shouldWaitForFile;
	}

	public String getPath() {
		return path;
	}

	public CloudFoundryServer getServer() {
		return server;
	}

	/**
	 * 
	 * @return true if the file content should be displayed as error content in
	 * the console. False otherwise.
	 */
	public boolean isError() {
		return isError;
	}

	public String getContent(final String appName, final int instanceIndex, final OutputStream stream, final int offset,
			final IProgressMonitor monitor) throws CoreException {
		if (shouldWaitForFile) {
			return new AbstractWaitWithProgressJob<String>(5, 500) {

				@Override
				protected String runInWait(IProgressMonitor monitor) throws CoreException {
					return performGetContent(appName, instanceIndex, stream, offset, monitor);
				}
				
				protected boolean shouldRetryOnError(Throwable t) {
					return true;
				}
				
			}.run(monitor);
		} else {
			return performGetContent(appName, instanceIndex, stream, offset, monitor);
		}

	}

	protected String performGetContent(String appName, int instanceIndex, OutputStream stream, int offset,
			IProgressMonitor monitor) throws CoreException {
		String content = null;
		try {
			content = server.getBehaviour().getFile(appName, instanceIndex, path, offset, monitor);
			if (stream != null && content != null && content.length() > 0) {
				stream.write(content.getBytes());
			}
		}
		catch (CoreException e) {
			Throwable t = e.getCause();
			// Ignore errors due to specified start position being past the
			// content length (i.e there is no new content). Otherwise rethrow
			// error
			if (t == null || !(t instanceof CloudFoundryException)
					|| !HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(((CloudFoundryException) t).getStatusCode())) {
				throw e;
			}
		}
		catch (IOException ioe) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(ioe));
		}
		catch (CloudFoundryException cfe) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(cfe));
		}
		return content;
	}

}
