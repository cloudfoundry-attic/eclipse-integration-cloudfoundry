/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public class ConsoleContents {

	private final List<IConsoleContent> content;

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
	public static ConsoleContents getStandardLogContent(final CloudFoundryServer cloudServer,
			final CloudApplication app, final int instanceIndex) {

		List<IConsoleContent> content = new ArrayList<IConsoleContent>();
		content.add(new StagingFileConsoleContent(cloudServer, app.getName(), instanceIndex));

		return new ConsoleContents(content);
	}

}
