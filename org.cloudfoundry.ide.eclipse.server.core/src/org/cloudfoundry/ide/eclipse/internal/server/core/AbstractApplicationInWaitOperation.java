/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudApplication.AppState;
import org.cloudfoundry.client.lib.CloudFoundryClient;
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
public abstract class AbstractApplicationInWaitOperation {
	private final CloudFoundryServer cloudServer;

	private final String jobName;

	public AbstractApplicationInWaitOperation(CloudFoundryServer cloudServer, String jobName) {
		this.cloudServer = cloudServer;
		this.jobName = jobName;
	}

	public boolean run(IProgressMonitor progress, final CloudFoundryClient client, CloudApplication cloudApplication)
			throws CoreException {

		if (cloudApplication == null) {
			return false;
		}
		int ticks = 10;
		long sleep = 3000;

		if (jobName != null) {
			progress.setTaskName(jobName);
		}

		if (!isInState(cloudApplication.getState())) {
			final String appName = cloudApplication.getName();

			ApplicationModule appModule = cloudServer.getApplicationModule(appName);
			if (appModule != null) {
				IModule module = appModule.getLocalModule();
				doOperation(cloudServer.getBehaviour(), module, progress);
				Boolean result = new WaitWithProgressJob<Boolean>(ticks, sleep) {

					@Override
					protected Boolean runInWait(IProgressMonitor monitor) throws CoreException {
						CloudApplication updatedCloudApp = cloudServer.getBehaviour().getApplication(appName, monitor);
						if (updatedCloudApp != null && isInState(updatedCloudApp.getState())) {
							return true;
						}

						// wait to check again the state of the app
						return null;
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