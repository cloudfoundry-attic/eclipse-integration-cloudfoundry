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

import java.io.IOException;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Performs general stream initialisation (like setting up the text code in the
 * output stream) as well as writing to the console output stream if content was
 * obtained.
 */
public abstract class ConsoleOutputStream implements ICloudFoundryConsoleOutputStream {

	protected final CloudFoundryServer server;

	private final IOConsoleOutputStream outputStream;

	private boolean shouldCloseStream = false;

	public ConsoleOutputStream(IOConsoleOutputStream outputStream, CloudFoundryServer server) {

		this.server = server;
		this.outputStream = outputStream;

	}

	public void initialiseStream() {

		if (outputStream != null && getStreamColour() != -1) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					outputStream.setColor(Display.getDefault().getSystemColor(getStreamColour()));
				}
			});
		}
	}

	public CloudFoundryServer getServer() {
		return server;
	}

	public String write(IProgressMonitor monitor) throws CoreException {

		final String content = getContent(monitor);

		if (content != null && content.length() > 0) {

			try {
				outputStream.write(content);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}

		return null;
	}

	abstract protected String getContent(IProgressMonitor monitor) throws CoreException;

	protected void  requestStreamClose(boolean close) {
		shouldCloseStream = close;
	}
	
	public boolean shouldCloseStream() {
		return shouldCloseStream;
	}

	/**
	 * Return an SWT constant for a particular colour
	 * @return e.g SWT.COLOR_RED
	 */
	protected int getStreamColour() {
		return -1;
	}

	public void close() throws IOException {
		if (outputStream != null) {
			outputStream.close();
		}
	}

}