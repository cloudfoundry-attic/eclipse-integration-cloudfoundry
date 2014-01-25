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
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public class StagingConsoleContents implements IConsoleContents {

	/**
	 * Return a list of File contents that should be shown to the user, like
	 * console logs. The list determines the order in which they appear to the
	 * user.
	 * @param cloudServer
	 * @param app
	 * @return
	 */
	public List<ICloudFoundryConsoleStream> getContents(final CloudFoundryServer cloudServer, String appName,
			final int instanceIndex) {

		List<ICloudFoundryConsoleStream> contents = new ArrayList<ICloudFoundryConsoleStream>();

		contents.add(new StagingFileConsoleStream(cloudServer, appName, instanceIndex));

		return contents;
	}

}
