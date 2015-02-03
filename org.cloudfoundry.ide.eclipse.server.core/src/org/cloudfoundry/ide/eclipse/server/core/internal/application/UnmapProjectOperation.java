/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.application;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.ModuleCache;
import org.cloudfoundry.ide.eclipse.server.core.internal.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * 
 * 
 */
public class UnmapProjectOperation implements ICloudFoundryOperation {

	private final CloudFoundryApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	public UnmapProjectOperation(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {

		if (appModule == null) {
			throw CloudErrorUtil.toCoreException("No Cloud module specified."); //$NON-NLS-1$
		}
		IProject project = CloudFoundryProjectUtil.getProject(appModule);
		if (project == null) {
			// No workspace project. Nothing to unmap
			return;
		}

		ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
		ServerData data = moduleCache.getData(cloudServer.getServerOriginal());

		// if it is being deployed, do not perform remap
		if (data.isUndeployed(appModule.getLocalModule())) {
			throw CloudErrorUtil
					.toCoreException("Unable to unmap module. It is currently being published. Please wait until publish operation is complete before remapping."); //$NON-NLS-1$
		}

		data.tagForRemap(appModule, project);
		IServer server = cloudServer.getServer();

		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.modifyModules(null, new IModule[] { appModule }, monitor);
		wc.save(true, monitor);
	}
}
