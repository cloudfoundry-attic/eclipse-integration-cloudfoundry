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
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.wst.server.core.IModule;


/**
 * Builds debug commands based on a specified application action, like starting
 * in debug mode, restarting in debug mode, update and restarting in debug mode,
 * and connecting a running app in debug mode to a debugger. Different commands
 * can be built for the same module running on the same cloud foundry, but the
 * caller is responsible to validate the command, handle error conditions, and
 * make sure that these commands do not clash with one another (for example, if
 * an app is already connected to a debugger, creating a new connect to debugger
 * command may either not do anything or result in an error).
 * 
 * A default deploy in debug mode command is always returned, and all debug
 * commands will attempt to connect the deployed application to a debugger.
 * Callers have the option of just specifying a debugger connection command for
 * applications that are already deployed and running in debug mode.
 * 
 * @author Nieraj Singh
 * 
 */
public class DebugCommandBuilder {

	final private IModule[] modules;

	final private CloudFoundryServer cloudFoundryServer;

	public DebugCommandBuilder(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
		this.modules = modules;
		this.cloudFoundryServer = cloudFoundryServer;
	}

	/**
	 * Returns a non-null debug command for launching an application in debug
	 * mode. If no action is specified, a default deploy in debug command will
	 * be returned. All debug commands attempt to connect the application to a
	 * debugger, and an optional listener can be specified to handle debugger
	 * events. The command is not guaranteed to run, as there is no validation
	 * to whether the server and module are valid or correct, and can be
	 * accessed. Callers need to handle error conditions associated with running
	 * the command.
	 * @param action specifying the type of debug action to take. If none given,
	 * a default deploy in debug mode action will be used
	 * @param listener optional, and is invoked only on debugger events after an
	 * application is deployed in debug mode
	 * @param connection describes a connection to a module running on a server.
	 * If none given, a default one will be used.
	 * @return non null debug command
	 */
	public DebugCommand getDebugCommand(ApplicationAction action, ICloudFoundryDebuggerListener listener,
			CloudFoundryDebugConnection connection) {
		if (connection == null) {
			connection = new CloudFoundryDebugConnection(modules, cloudFoundryServer);
		}
		DebugCommand command = null;

		if (action != null) {
			switch (action) {
			case CONNECT_TO_DEBUGGER:
				command = new ConnectToDebuggerCommand(cloudFoundryServer, modules, connection, listener);
				break;
			case DEBUG:
			case RESTART:
			case UPDATE_RESTART:
				command = new DeployInDebugCommand(cloudFoundryServer, modules, action, listener);
				break;

			}
		}

		if (command == null) {
			command = new DeployInDebugCommand(cloudFoundryServer, modules, ApplicationAction.DEBUG, listener);
		}

		return command;
	}

	/**
	 * @see #getDebugCommand(ApplicationAction, ICloudFoundryDebuggerListener,
	 * CloudFoundryDebugConnection)
	 */
	public DebugCommand getDebugCommand(ApplicationAction action, ICloudFoundryDebuggerListener listener) {
		return getDebugCommand(action, listener, null);
	}

	/**
	 * Returns the default command to deploy an application in debug mode and
	 * connect to debugger.
	 * @return non-null default deploy and connect command
	 */
	public DebugCommand getDefaultDeployInDebugModeCommand() {
		return getDebugCommand(null, null, null);
	}

}
