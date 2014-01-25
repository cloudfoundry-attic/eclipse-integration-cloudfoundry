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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.swt.SWT;

public class StdConsoleContents implements IConsoleContents {

	public static final String STD_OUT_LOG = "logs/stdout.log";

	public static final String STD_ERROR_LOG = "logs/stderr.log";

	public List<ICloudFoundryConsoleStream> getContents(CloudFoundryServer cloudServer, CloudFoundryApplicationModule app,
			int instanceIndex) {
		String appName = app.getDeployedApplicationName();
		return getContents(cloudServer, appName, instanceIndex);
	}

	public List<ICloudFoundryConsoleStream> getContents(CloudFoundryServer cloudServer, String appName, int instanceIndex) {
		List<ICloudFoundryConsoleStream> contents = new ArrayList<ICloudFoundryConsoleStream>();
		contents.add(new StdLogFileConsoleStream(STD_ERROR_LOG, SWT.COLOR_RED, cloudServer, appName, instanceIndex));
		contents.add(new StdLogFileConsoleStream(STD_OUT_LOG, -1, cloudServer, appName, instanceIndex));
		return contents;
	}

}
