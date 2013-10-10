/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springframework.util.Assert;

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
		Assert.notNull(appModule);
		this.cloudServer = cloudServer;
		this.appModule = appModule;
	}

	protected boolean internalRunInWait(IProgressMonitor progress) throws CoreException {

		CloudApplication updatedCloudApp = cloudServer.getBehaviour().getApplication(
				appModule.getDeployedApplicationName(), progress);

		if (updatedCloudApp == null) {
			throw CloudErrorUtil
					.toCoreException("No cloud application found while attempting to check application state.");
		}

		return isInState(updatedCloudApp.getState());

	}

	protected boolean shouldRetryOnError(Throwable t) {
		// If cloud application cannot be resolved due to any errors, stop any
		// further attempts to check app state.
		return false;
	}

	protected abstract boolean isInState(AppState state);

}