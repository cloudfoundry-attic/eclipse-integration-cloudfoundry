/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.application;

import java.util.Arrays;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.server.core.AbstractApplicationDelegate;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Contains a default implementation for generating a resources archive of all
 * the resources that are to be pushed to the Cloud Foundry server.
 * 
 */
public abstract class ModuleResourceApplicationDelegate extends AbstractApplicationDelegate {

	public ModuleResourceApplicationDelegate() {

	}

	public boolean providesApplicationArchive(IModule module) {
		return true;
	}

	/**
	 * NOTE: For INTERNAL use only. Framework adopters should not override or invoke.
	 * @param appModule
	 * @return true if requires URL. False otherwise
	 */
	public boolean requiresURL(CloudFoundryApplicationModule appModule) {
		return requiresURL();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.core.internal.application.
	 * AbstractApplicationDelegate
	 * #getApplicationArchive(org.cloudfoundry.ide.eclipse.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.eclipse.wst.server.core.model.IModuleResource[])
	 */
	public ApplicationArchive getApplicationArchive(CloudFoundryApplicationModule module,
			IModuleResource[] moduleResources) throws CoreException {
		return new ModuleResourceApplicationArchive(module.getLocalModule(), Arrays.asList(moduleResources));
	}
}
