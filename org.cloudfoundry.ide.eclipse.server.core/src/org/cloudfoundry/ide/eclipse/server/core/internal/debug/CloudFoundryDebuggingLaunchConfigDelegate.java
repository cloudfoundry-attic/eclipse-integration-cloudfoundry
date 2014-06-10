/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;


public class CloudFoundryDebuggingLaunchConfigDelegate extends AbstractJavaLaunchConfigurationDelegate {

	public static final String SOURCE_LOCATOR = "org.cloudfoundry.ide.eclipse.debug.sourcepathcomputer";

	public static final String LAUNCH_CONFIGURATION_ID = "org.cloudfoundry.ide.eclipse.launchconfig.debug";

	public static final String TIME_OUT = "timeout";

	public static final String HOST_NAME = "hostname";

	public static final String PORT = "port";

	public static final String DEBUGGER_CONNECTION_ID = "debuggerconnectionid";

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		// Use default for now
		final IVMConnector connector = JavaRuntime.getDefaultVMConnector();

		// Create the required arguments for the IVMConnector
		final Map<String, String> argMap = new HashMap<String, String>();
		String timeout = configuration.getAttribute(TIME_OUT, (String) null);
		if (timeout != null) {
			argMap.put("timeout", timeout);
		}

		String host = configuration.getAttribute(HOST_NAME, (String) null);
		String port = configuration.getAttribute(PORT, (String) null);
		if (host != null && port != null) {
			argMap.put("hostname", host);
			argMap.put("port", port);

			setSourceLocator(launch);
			connector.connect(argMap, monitor, launch);
			addDebuggerConnectionListener(configuration.getAttribute(DEBUGGER_CONNECTION_ID, (String) null), launch);
		}
		else {
			CloudFoundryPlugin
					.logError("Failed to launch debug configuration. IP and host for application instance cannot be resolved.");
		}

	}

	protected void addDebuggerConnectionListener(String connectionID, ILaunch launch) {

		DebugCommand command = DebugCommand.getDebugCommand(connectionID);
		if (command != null) {
			Object source = launch.getDebugTarget();
			ConnectToDebuggerListener debugListener = new ConnectToDebuggerListener(command, source);
			DebugPlugin.getDefault().addDebugEventListener(debugListener);
		}
	}

	protected void setSourceLocator(ILaunch launch) throws CoreException {
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		if (launch.getSourceLocator() == null) {
			ISourceLookupDirector sourceLocator = new JavaSourceLookupDirector();
			ISourcePathComputer locator = getLaunchManager().getSourcePathComputer(SOURCE_LOCATOR);
			if (locator != null) {
				sourceLocator.setSourcePathComputer(locator); //$NON-NLS-1$
				sourceLocator.initializeDefaults(configuration);
				launch.setSourceLocator(sourceLocator);
			}
		}
	}

}
