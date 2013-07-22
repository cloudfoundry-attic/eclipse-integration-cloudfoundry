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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Performs an application operation and waits until it reaches an expected
 * state. Concrete classes need to specify the operation, like start or stopping
 * the application, as well as the state that the app needs to be in. If an
 * application is already in the expected state, the operation will not perform
 * 
 */
public abstract class AbstractWaitForStateOperation {
	private final CloudFoundryServer cloudServer;

	private final String jobName;

	private int attempts;

	private long sleep;

	public AbstractWaitForStateOperation(CloudFoundryServer cloudServer, String jobName) {
		this(cloudServer, jobName, 10, 3000);
	}

	public AbstractWaitForStateOperation(CloudFoundryServer cloudServer, String jobName, int attempts, long sleep) {
		this.cloudServer = cloudServer;
		this.jobName = jobName;
		this.attempts = attempts;
		this.sleep = sleep;
	}

	public boolean run(IProgressMonitor progress, CloudApplication cloudApplication) throws CoreException {

		if (cloudApplication == null) {
			return false;
		}

		if (jobName != null) {
			progress.setTaskName(jobName);
		}

		if (!isInState(cloudApplication.getState())) {
			final String appName = cloudApplication.getName();

			CloudFoundryApplicationModule appModule = cloudServer.getApplicationModule(appName);
			if (appModule != null) {
				IModule module = appModule.getLocalModule();
				doOperation(cloudServer.getBehaviour(), module, progress);
				Boolean result = new WaitWithProgressJob(attempts, sleep) {

					@Override
					protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException {
						CloudApplication updatedCloudApp = cloudServer.getBehaviour().getApplication(appName, monitor);

						return updatedCloudApp != null && isInState(updatedCloudApp.getState());
					}

					@Override
					protected boolean shouldRetryOnError(Throwable t) {
						return true;
					}

				}.run(progress);
				return result.booleanValue();
			}
			return false;
		}
		else {
			return true;
		}
	}

	protected abstract void doOperation(CloudFoundryServerBehaviour behaviour, IModule module, IProgressMonitor progress)
			throws CoreException;

	protected abstract boolean isInState(AppState state);

}