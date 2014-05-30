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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

/**
 * Wraps around a module resource delta so that the delta is accessible to
 * operations that need to check if the resource has been removed
 * 
 */
public class ModuleResourceDeltaWrapper implements IModuleResource {

	private final IModuleResourceDelta resourceDelta;

	public ModuleResourceDeltaWrapper(IModuleResourceDelta resourceDelta) {
		this.resourceDelta = resourceDelta;
	}

	public Object getAdapter(Class clazz) {

		return resourceDelta.getModuleResource().getAdapter(clazz);
	}

	public IPath getModuleRelativePath() {

		return resourceDelta.getModuleResource().getModuleRelativePath();
	}

	public String getName() {
		return resourceDelta.getModuleResource().getName();
	}

	public IModuleResourceDelta getResourceDelta() {
		return resourceDelta;
	}
}