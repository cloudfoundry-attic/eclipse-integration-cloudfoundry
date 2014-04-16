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
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;


/**
 * Deploys an app to the Cloud foundry server using the specified debug start
 * action (deploy and debug, restart without updating, restart with updating)
 * 
 * @author Nieraj Singh
 * 
 */
public class DeployInDebugCommand extends DebugCommand {

	protected final String COMMAND_NAME = "Launching application in debug mode.";

	/**
	 * Valid actions are: {@literal ApplicationAction#DEBUG}
	 * {@literal ApplicationAction#RESTART}
	 * {@literal ApplicationAction#UPDATE_RESTART}
	 * @param server
	 * @param modules
	 * @param action
	 */
	public DeployInDebugCommand(CloudFoundryServer server, IModule[] modules, ApplicationAction action,
			ICloudFoundryDebuggerListener listener) {
		super(server, modules, action, listener);
	}

	public String getCommandName() {
		return COMMAND_NAME;
	}

	public void connect(IProgressMonitor monitor) {
		try {
			switch (getDebugApplicationAction()) {
			case DEBUG:
				getCloudFoundryServer().getBehaviour().debugModule(getModules(), monitor);
				break;
			case RESTART:
				getCloudFoundryServer().getBehaviour().restartDebugModule(getModules(), monitor);
				break;
			case UPDATE_RESTART:
				getCloudFoundryServer().getBehaviour().updateRestartDebugModule(getModules(), getIncrementalPublish(),
						monitor);
				break;
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	protected boolean getIncrementalPublish() {
		return CloudFoundryPlugin.getDefault().getIncrementalPublish();
	}

}
