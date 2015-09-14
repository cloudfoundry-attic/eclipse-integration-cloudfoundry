/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc.
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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.WaitWithProgressJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Waits until a deployed application reaches an expected state.
 */
public abstract class AbstractWaitForStateOperation extends WaitWithProgressJob {
	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	public AbstractWaitForStateOperation(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
		this(cloudServer, appModule, 10, 3000);
	}

	public AbstractWaitForStateOperation(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule,
			int attempts, long sleep) {
		super(attempts, sleep);
		this.cloudServer = cloudServer;
		this.appModule = appModule;
	}

	@Override
	protected boolean internalRunInWait(IProgressMonitor progress) throws CoreException {

		CloudApplication updatedCloudApp = cloudServer.getBehaviour().getCloudApplication(
				appModule.getDeployedApplicationName(), progress);

		if (updatedCloudApp == null) {
			throw CloudErrorUtil
					.toCoreException("No cloud application found while attempting to check application state."); //$NON-NLS-1$
		}

		return isInState(updatedCloudApp.getState());

	}

	@Override
	protected boolean shouldRetryOnError(Throwable t) {
		// If cloud application cannot be resolved due to any errors, stop any
		// further attempts to check app state.
		return false;
	}

	protected abstract boolean isInState(AppState state);

}