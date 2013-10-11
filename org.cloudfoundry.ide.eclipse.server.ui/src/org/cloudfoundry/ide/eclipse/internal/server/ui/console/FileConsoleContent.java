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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

import org.eclipse.ui.console.IOConsoleOutputStream;

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

}
