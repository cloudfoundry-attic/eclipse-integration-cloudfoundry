/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.console.file;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.swt.SWT;

public class StdConsoleContents implements IConsoleContents {

	public static final String STD_OUT_LOG = "logs/stdout.log"; //$NON-NLS-1$

	public static final String STD_ERROR_LOG = "logs/stderr.log"; //$NON-NLS-1$

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
