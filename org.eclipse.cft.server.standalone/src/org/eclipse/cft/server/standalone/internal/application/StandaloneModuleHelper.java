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
 ********************************************************************************/
package org.eclipse.cft.server.standalone.internal.application;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.wst.server.core.IModule;

/**
 * Determines if a WST IModule corresponds to a Java standalone application.
 */
public class StandaloneModuleHelper {

	private final CloudFoundryApplicationModule appModule;

	private final IModule module;

	public StandaloneModuleHelper(CloudFoundryApplicationModule appModule) {
		this.appModule = appModule;
		this.module = appModule.getLocalModule();
	}

	public StandaloneModuleHelper(IModule module) {
		this.appModule = null;
		this.module = module;
	}

	public boolean isSupportedStandalone() {
		if (appModule == null && module == null) {
			return false;
		}

		boolean isStandalone = module != null
				&& StandaloneFacetHandler.ID_MODULE_STANDALONE.equals(module
						.getModuleType().getId());

		return isStandalone;
	}

	public Staging getStaging() {
		if (appModule == null) {
			return null;
		}
		Staging staging = appModule.getDeploymentInfo() != null  ? appModule.getDeploymentInfo().getStaging() : null;
		if (staging == null) {
			CloudApplication cloudApp = appModule.getApplication();
			if (cloudApp != null) {
				staging = cloudApp.getStaging();
			}
		}
		return staging;
	}

}
