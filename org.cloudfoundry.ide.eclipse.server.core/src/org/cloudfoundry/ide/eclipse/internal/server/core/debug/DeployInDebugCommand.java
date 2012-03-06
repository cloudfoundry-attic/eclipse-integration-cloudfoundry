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
