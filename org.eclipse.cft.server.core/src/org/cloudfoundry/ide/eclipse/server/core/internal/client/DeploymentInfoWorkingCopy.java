/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import org.cloudfoundry.ide.eclipse.server.core.ApplicationDeploymentInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A working copy of an application's {@link ApplicationDeploymentInfo}. If the
 * app module contains a deployment info, it will create a working copy of it.
 * If the app module does not contain a deployment info, it will create a
 * working copy with the app's default deployment values. The working copy is
 * intended to be short-lived, and not shared, and changes only take effect in
 * the cloud app module's deployment info if {@link #save()} is invoked.
 */
public abstract class DeploymentInfoWorkingCopy extends ApplicationDeploymentInfo {

	protected final CloudFoundryApplicationModule appModule;

	protected DeploymentInfoWorkingCopy(CloudFoundryApplicationModule appModule) {
		super(appModule.getDeployedApplicationName());
		this.appModule = appModule;

	}

	/**
	 * Fill the working copy with either values of an existing deployment
	 * information in the associated application module, or default values if
	 * the no existing deployment information exists for the application module.
	 */
	public void fill(IProgressMonitor monitor) throws CoreException {
		if (appModule.getDeploymentInfo() != null) {
			setInfo(appModule.getDeploymentInfo());
		}
		else {
			setInfo(appModule.getDefaultDeploymentInfo(monitor));
		}
	}

	/**
	 * Saves the working copy in the associated
	 * {@link CloudFoundryApplicationModule}.
	 */
	abstract public void save();

}
