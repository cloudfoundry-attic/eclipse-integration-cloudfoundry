/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/

package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.io.IOException;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;

public abstract class AbstractConsoleContent implements IConsoleContent {

	private int swtColour;

	protected final String appName;

	protected final int instanceIndex;

	private IOConsoleOutputStream outputStream;

	protected final CloudFoundryServer server;

	public AbstractConsoleContent(CloudFoundryServer server, int swtColour, String appName, int instanceIndex) {
		this.server = server;
		this.appName = appName;
		this.instanceIndex = instanceIndex;
		this.swtColour = swtColour;
	}

	public CloudFoundryServer getServer() {
		return server;
	}

	public String write(IProgressMonitor monitor) throws CoreException {
		final String content = getContent(monitor);

		if (content != null && content.length() > 0) {
			try {
				outputStream.write(content);
				return content;
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}

		return null;
	}

	public void initialiseStream(IOConsoleOutputStream outputStream) {
		this.outputStream = outputStream;
		if (this.outputStream != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					AbstractConsoleContent.this.outputStream.setColor(Display.getDefault().getSystemColor(swtColour));
				}
			});
		}
	}

	public void close() {
		if (outputStream != null && !outputStream.isClosed()) {
			try {
				outputStream.close();
			}
			catch (IOException e) {
				CloudFoundryPlugin.logError("Failed to close console output stream due to: " + e.getMessage(), e);
			}
		}
	}

	public boolean isClosed() {
		return outputStream == null || outputStream.isClosed();
	}

	abstract protected String getContent(IProgressMonitor monitor) throws CoreException;

}
