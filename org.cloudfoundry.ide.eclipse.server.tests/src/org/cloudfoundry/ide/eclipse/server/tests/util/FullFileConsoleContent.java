/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.io.IOException;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * For testing only. Obtains console content for both stdout and stderror for a
 * given app from the full log file.
 * 
 */
public class FullFileConsoleContent extends ConsoleContent {

	public FullFileConsoleContent(CloudFoundryServer cloudServer, MessageConsole console, CloudApplication app,
			int instanceIndex) {
		super(cloudServer, console, app, instanceIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContent
	 * #getStdErrorContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected String getStdErrorContent(IProgressMonitor monitor) throws CoreException {
		String content = getContent(stdError, stderrPath, stderrOffset, monitor);
		if (content != null) {
			stderrOffset = content.length();
		}
		return content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContent
	 * #getStdOurContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected String getStdOurContent(IProgressMonitor monitor) throws CoreException {
		String content = getContent(stdOut, stdoutPath, stdoutOffset, monitor);
		if (content != null) {
			stdoutOffset = content.length();
		}
		return content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContent
	 * #getAndWriteContentFromServer
	 * (org.eclipse.ui.console.IOConsoleOutputStream, java.lang.String, int,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected String getAndWriteContentFromServer(IOConsoleOutputStream stream, String path, int offset,
			IProgressMonitor monitor) throws CoreException, IOException {
		String content = cloudServer.getBehaviour().getFile(app.getName(), instanceIndex, path, monitor);
		if (stream != null && content != null && content.length() > offset) {
			stream.write(content.substring(offset));
		}
		return content;
	}

}
