/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
 *     IBM - wait for all module publish complete before finish up publish operation.
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Operation that updates the mapping of a {@link CloudFoundryApplicationModule}
 * to the actual published application in Cloud Foundry (
 * {@link CloudApplication} ). The mapping refresh occurs regardless to whether
 * the operation succeeds or not.
 * <p/>
 * This operation should be used primarily if only one application module needs
 * to be updated, as opposed to a server-wide application module refresh, which
 * may be slow for servers with many published applications.
 *
 */
public abstract class UpdateApplicationOperation extends ModifyOperation {

	private final String appName;

	public UpdateApplicationOperation(String appName, CloudFoundryServerBehaviour behaviour) {
		super(behaviour);
		this.appName = appName;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		// Deployment operations may be long running so stop refresh
		// until operation completes
		behaviour.getRefreshHandler().stop();
		try {
			performOperation(monitor);
		}
		finally {
			updateCloudApplicationMapping(monitor);
			refresh(monitor);
		}
	}

	protected void updateCloudApplicationMapping(IProgressMonitor monitor) {
		try {
			CloudFoundryApplicationModule appModule = behaviour.getCloudFoundryServer().getExistingCloudModule(appName);
			CloudApplication actualApp = behaviour.getApplication(appName, monitor);
			if (appModule != null && actualApp != null) {
				appModule.setCloudApplication(actualApp);
			}
			else {
				String missing = appModule == null ? "No Cloud appplication module found. The application may not be correctly created in the local Cloud server instance. " //$NON-NLS-1$ 
						: "No corresponding Cloud application found for the application module. The application may not exist in the Cloud server."; //$NON-NLS-1$
				CloudFoundryPlugin
						.logError("Unable to refresh the mapping between the Cloud application module and the published Cloud appplication for " + appName + " - " + missing); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}
}
