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

	private final boolean isError;

	private final CloudFoundryServer server;

	public static final String STD_OUT_LOG = "logs/stdout.log";

	public static final String STD_ERROR_LOG = "logs/stderr.log";

	public FileContent(String path, boolean isError, CloudFoundryServer server) {
		this.path = path;
		this.isError = isError;
		this.server = server;
	}

	public String getPath() {
		return path;
	}
	
	public CloudFoundryServer getServer() {
		return server;
	}

	/**
	 * 
	 * @return true if the file content should be displayed as error content in the console.
	 * False otherwise.
	 */
	public boolean isError() {
		return isError;
	}

	public String getContent(String appName, int instanceIndex, OutputStream stream, int offset,
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
		return content;
	}

}
