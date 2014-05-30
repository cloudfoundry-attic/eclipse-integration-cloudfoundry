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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Operation that modifies the server and triggers a refresh event. Note that
 * this should not be used for operations that simply fetch information from the
 * server. It should only be used for operations that change values, like for
 * instance scaling an application, changing mapped application URLs, as it may
 * fire a server changed event.
 * 
 */
public abstract class ModifyOperation implements ICloudFoundryOperation {

	protected final CloudFoundryServerBehaviour behaviour;

	public ModifyOperation(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		performOperation(monitor);
		// Only trigger a refresh IF the operation succeeded.
		refresh(monitor);
	}

	/**
	 * Gets invoked after the operation completes. Does not get called if an
	 * operation failed.
	 * @param monitor
	 * @throws CoreException
	 */
	protected void refresh(IProgressMonitor monitor) throws CoreException {

		behaviour.getRefreshHandler().fireRefreshEvent(monitor);
	}

	protected abstract void performOperation(IProgressMonitor monitor) throws CoreException;

}