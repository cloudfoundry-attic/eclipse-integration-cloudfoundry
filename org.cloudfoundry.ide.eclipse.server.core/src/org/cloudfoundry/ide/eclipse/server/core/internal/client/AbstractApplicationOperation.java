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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Performs an operation on a specific module, and then updates that module if
 * the operation is successful.
 * <p/>
 * Performs a refresh event after the operation and module update is completed.
 *
 */
public abstract class AbstractApplicationOperation extends AbstractCloudOperation {

	public static final String NO_MODULE_ERROR = "Unable to refresh module - Module not found in Cloud server instance - ";//$NON-NLS-1$

	private final IModule module;

	protected AbstractApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule module) {
		super(behaviour);
		this.module = module;
	}

	protected void refresh(IProgressMonitor monitor) throws CoreException {
		if (module == null) {
			throw CloudErrorUtil
					.toCoreException(NO_MODULE_ERROR + getBehaviour().getCloudFoundryServer().getServerId());
		}
		getBehaviour().getRefreshHandler().fireRefreshEvent(module);
	}
	
	protected IModule getModule() {
		return module;
	}

	@Override
	protected void performOperation(IProgressMonitor monitor) throws CoreException {
		performApplicationOperation(monitor);
		// Update any mappings
		updateCloudModule(module, monitor);
	}
	
	protected void updateCloudModule(IModule module, IProgressMonitor monitor) throws CoreException {
		getBehaviour().updateCloudModule(module, monitor);
	}

	protected abstract void performApplicationOperation(IProgressMonitor monitor) throws CoreException;
}
