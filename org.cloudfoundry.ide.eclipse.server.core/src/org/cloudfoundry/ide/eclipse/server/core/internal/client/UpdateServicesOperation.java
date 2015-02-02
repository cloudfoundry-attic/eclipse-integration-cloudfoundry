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

import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class UpdateServicesOperation extends AbstractCloudOperation {

	private final BaseClientRequest<?> request;

	protected UpdateServicesOperation(BaseClientRequest<?> request, CloudFoundryServerBehaviour behaviour) {
		super(behaviour);
		this.request = request;
	}

	@Override
	protected void refresh(IProgressMonitor monitor) throws CoreException {
		// Do not refresh modules for service changes. Just notify that
		// services have changed
		ServerEventHandler.getDefault().fireServicesUpdated(getBehaviour().getCloudFoundryServer());
	}

	@Override
	protected void performOperation(IProgressMonitor monitor) throws CoreException {
		request.run(monitor);
	}

}
