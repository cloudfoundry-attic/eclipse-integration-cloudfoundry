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
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.swt.SWT;

public class ConsoleContents {

	private final List<IConsoleContent> content;

	public static final String STD_OUT_LOG = "logs/stdout.log";

	public static final String STD_ERROR_LOG = "logs/stderr.log";

	public static final String STAGING_LOG = "logs/staging_task.log";

	public static final long STD_LOG_INITIAL_WAIT = 1000;

	public ConsoleContents(List<IConsoleContent> content) {
		this.content = content;
	}

	public ConsoleContents(IConsoleContent singleContent) {
		this.content = Arrays.asList(singleContent);
	}

	public List<IConsoleContent> getContents() {
		return content;
	}

	/**
	 * Return a list of File contents that should be shown to the user, like
	 * console logs. The list determines the order in which they appear to the
	 * user.
	 * @param cloudServer
	 * @param app
	 * @return
	 */
	public static ConsoleContents getStandardLogContent(CloudFoundryServer cloudServer, CloudApplication app,
			int instanceIndex) {

		List<IConsoleContent> content = new ArrayList<IConsoleContent>();
		content.add(new FileConsoleContent(STAGING_LOG, SWT.COLOR_DARK_GREEN, cloudServer, app.getName(), instanceIndex));
		content.add(new FileConsoleContent(STD_ERROR_LOG, SWT.COLOR_RED, cloudServer, app.getName(), instanceIndex,
				STD_LOG_INITIAL_WAIT*2));
		content.add(new FileConsoleContent(STD_OUT_LOG, -1, cloudServer, app.getName(), instanceIndex,
				STD_LOG_INITIAL_WAIT*3));
		return new ConsoleContents(content);
	}

}
