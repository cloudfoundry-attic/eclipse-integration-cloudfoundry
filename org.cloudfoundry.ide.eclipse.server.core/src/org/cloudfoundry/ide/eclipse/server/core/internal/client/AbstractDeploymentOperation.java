/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Operations for deploying applications. Performs refresh operations common to
 * deploying apps.
 */
public abstract class AbstractDeploymentOperation implements ICloudFoundryOperation {

	protected final CloudFoundryServerBehaviour behaviour;

	public AbstractDeploymentOperation(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		// Deployment operations may be long running so stop refresh
		// until operation completes
		behaviour.getRefreshHandler().stop();
		try {
			performOperation(monitor);
		}
		finally {
			// For application operations, always refresh modules and fire event
			// even
			// if an exception is thrown. It may, for example, allow listeners
			// to update the UI in case an app failed to deploy
			behaviour.refreshModules(monitor);
			behaviour.getRefreshHandler().fireRefreshEvent(monitor);
		}
	}

	protected abstract void performOperation(IProgressMonitor monitor) throws CoreException;

}
