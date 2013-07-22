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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.FileContent;

public class ConsoleContent {

	private final List<FileContent> content;

	private final String initialContent;

	public ConsoleContent(List<FileContent> content, String initialContent) {
		this.content = content;
		this.initialContent = initialContent;
	}

	public List<FileContent> getFileContents() {
		return content;
	}

	public String getInitialContent() {
		return initialContent;
	}

	public static String getStagingInitialContent(CloudApplication cloudModule, CloudFoundryServer cloudServer) {
		StringBuffer initialContent = new StringBuffer();
		initialContent.append("Staging application ");
		initialContent.append(cloudModule.getName());
		initialContent.append(' ');
		initialContent.append("in server ");
		initialContent.append(cloudServer.getDeploymentName());
		initialContent.append('\n');
		initialContent.append("Please wait while staging completes...");
		initialContent.append('\n');
		return initialContent.toString();
	}

	/**
	 * Return a list of File contents that should be shown to the user, like
	 * console logs. The list determines the order in which they appear to the
	 * user.
	 * @param cloudServer
	 * @param app
	 * @return
	 */
	public static ConsoleContent getConsoleContent(CloudFoundryServer cloudServer, CloudApplication app) {
		List<FileContent> content = new ArrayList<FileContent>();

//		if (cloudServer.getBehaviour().supportsSpaces()) {
//			content.add(new FileContent("logs/staging_task.log", false, cloudServer));
//		}

		content.add(new FileContent(FileContent.STD_ERROR_LOG, true, cloudServer));
		content.add(new FileContent(FileContent.STD_OUT_LOG, false, cloudServer));
		return new ConsoleContent(content, null);
	}

	/**
	 * 
	 * @param cloudServer
	 * @param app
	 * @param logContents
	 * @return
	 */
	public static ConsoleContent getConsoleContent(List<FileContent> logContents, String initialContent) {

		return new ConsoleContent(logContents, initialContent);

	}
}
