/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 *     Keith Chong, IBM - Add additional properties for enabling commands based on org.eclipse.ui.menus
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;


import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Defines properties used to test various aspects of debugging an application.
 * Each property should implement an property testing method. The property name
 * must exactly match the defined property name in the property tester extension
 * point.
 * 
 * @author Nieraj Singh
 * 
 */
public enum CloudFoundryProperties {

	isServerSupported {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (cloudFoundryServer != null) {
				return CloudServerUtil.isCloudFoundryServer(cloudFoundryServer.getServer());
			}
			return false;
		}	
	},
	
	isServerStarted {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (cloudFoundryServer != null) {
				IServer server = cloudFoundryServer.getServer();
				if (server != null) {
					return (server.getServerState() == IServer.STATE_STARTED);
				}
			}
			return false;
		}
	},
	
	isServerStopped {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (cloudFoundryServer != null) {
				IServer server = cloudFoundryServer.getServer();
				if (server != null) {
					return (server.getServerState() == IServer.STATE_STOPPED);
				}
			}
			return false;
		}
    },
	
	isDebugEnabled {

		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			// If the server is stopped, it's not possible to check it for
			// debug
			// enablement
			if (cloudFoundryServer.getServer().getServerState() == IServer.STATE_STOPPED
					|| cloudFoundryServer.getServer().getServerState() == IServer.STATE_STOPPING) {
				return false;
			}

			CloudFoundryServerBehaviour behaviour = cloudFoundryServer.getBehaviour();

			if (behaviour == null) {
				return false;
			}
			return behaviour.isServerDebugModeAllowed();
		}
	},

	isModuleStopped {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {

			return getDeployedAppState(modules, cloudFoundryServer.getServer()) == IServer.STATE_STOPPED;
		}
	},

	isModuleStarted {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			return getDeployedAppState(modules, cloudFoundryServer.getServer()) == IServer.STATE_STARTED;
		}
	},

	isCloudModuleStarted {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (modules != null && modules.length > 0) {
				// Selection is limited to one module
				int moduleState = cloudFoundryServer.getServer().getModuleState(modules);
				return IServer.STATE_STARTED == moduleState;
			}
			return false;
		}
	},

	isCloudModuleLocal {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (modules != null && modules.length > 0) {
				
				// Selection is limited to one module
				CloudFoundryApplicationModule cloudModule = cloudFoundryServer.getExistingCloudModule(modules[0]);
				if (cloudModule != null) {
					return !cloudModule.isExternal();
				}				
			}
			return false;
		}
	},

	isApplicationRunningInDebugMode {

		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			int appState = getDeployedAppState(modules, cloudFoundryServer.getServer());
			if (appState == IServer.STATE_STARTED && modules != null && modules.length > 0) {
				DebugModeType modeType = cloudFoundryServer.getBehaviour().getDebugModeType(modules[0], null);
				return modeType != null;
			}
			return false;
		}
	},

	isConnectedToDebugger {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {

			return DebugCommand.isConnectedToDebugger(cloudFoundryServer, modules);

		}
	},

	/**
	 * Determines is the module's workspace project is accessible. True if it
	 * is. False, if the associated workspace project is inaccessible or cannot
	 * be resolved
	 */
	isModuleProjectAccessible {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {

			if (modules == null || modules.length == 0) {
				return false;
			}

			IProject project = modules[0].getProject();
			return project != null && project.isAccessible();

		}
	};

	// Should be overridden by each property
	public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
		return false;
	}

	protected static int getDeployedAppState(IModule[] modules, IServer server) {
		return modules != null && modules.length > 0 ? server.getModuleState(modules) : -1;
	}
}