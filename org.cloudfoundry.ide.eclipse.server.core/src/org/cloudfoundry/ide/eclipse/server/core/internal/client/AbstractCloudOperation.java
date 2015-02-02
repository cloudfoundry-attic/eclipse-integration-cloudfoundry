/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An operation that performs an operation in a
 * {@link CloudFoundryServerBehaviour} target Cloud space. Performs a refresh
 * operation after the behaviour operation is completed.
 */
public abstract class AbstractCloudOperation implements ICloudFoundryOperation {

	private final CloudFoundryServerBehaviour behaviour;

	public AbstractCloudOperation(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	protected CloudFoundryServerBehaviour getBehaviour() {
		return behaviour;
	}

	public void run(IProgressMonitor monitor) throws CoreException {

		performOperation(monitor);

		// Only trigger a refresh IF the operation succeeded.
		refresh(monitor);
	}

	protected abstract void refresh(IProgressMonitor monitor) throws CoreException;

	protected abstract void performOperation(IProgressMonitor monitor) throws CoreException;

}
