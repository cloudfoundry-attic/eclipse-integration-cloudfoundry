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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.ide.eclipse.server.core.AbstractAppStateTracker;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

/**
 * 
 * Attempts to start an application. It does not create an application, or
 * incrementally or fully push the application's resources. It simply starts the
 * application in the server with the application's currently published
 * resources, regardless of local changes have occurred or not.
 * 
 */
@SuppressWarnings("restriction")
class RestartOperation extends ApplicationOperation {

	/**
	 * 
	 */

	public RestartOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour, modules);
	}


	@Override
	protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		final Server server = (Server) getBehaviour().getServer();
		final CloudFoundryApplicationModule cloudModule = appModule;

		try {
			cloudModule.setErrorStatus(null);

			final String deploymentName = cloudModule.getDeploymentInfo().getDeploymentName();

			server.setModuleState(getModules(), IServer.STATE_STARTING);

			if (deploymentName == null) {
				server.setModuleState(getModules(), IServer.STATE_STOPPED);

				throw CloudErrorUtil
						.toCoreException("Unable to start application. Missing application deployment name in application deployment information."); //$NON-NLS-1$
			}
			
			// Update the module with the latest CloudApplication from the client before starting the application
			getBehaviour().updateCloudModuleWithInstances(appModule, monitor);

			final ApplicationAction deploymentMode = getDeploymentConfiguration().getApplicationStartMode();
			if (deploymentMode != ApplicationAction.STOP) {
				// Start the application. Use a regular request rather than
				// a staging-aware request, as any staging errors should not
				// result in a reattempt, unlike other cases (e.g. get the
				// staging
				// logs or refreshing app instance stats after an app has
				// started).

				getBehaviour().printlnToConsole(cloudModule, Messages.CONSOLE_PRE_STAGING_MESSAGE);

				CloudFoundryPlugin.getCallback().startApplicationConsole(getBehaviour().getCloudFoundryServer(),
						cloudModule, 0, monitor);

				getBehaviour().new BehaviourRequest<Void>("Starting application " + deploymentName) { //$NON-NLS-1$
					@Override
					protected Void doRun(final CloudFoundryOperations client, SubMonitor progress) throws CoreException {
						CloudFoundryPlugin.trace("Application " + deploymentName + " starting"); //$NON-NLS-1$ //$NON-NLS-2$

						client.stopApplication(deploymentName);

						StartingInfo info = client.startApplication(deploymentName);
						if (info != null) {

							cloudModule.setStartingInfo(info);

							// Inform through callback that application
							// has started
							CloudFoundryPlugin.getCallback().applicationStarting(
									RestartOperation.this.getBehaviour().getCloudFoundryServer(), cloudModule);
						}
						return null;
					}
				}.run(monitor);

				// This should be staging aware, in order to reattempt on
				// staging related issues when checking if an app has
				// started or not
				getBehaviour().new StagingAwareRequest<Void>("Waiting for application to start: }" + deploymentName) { //$NON-NLS-1$
					@Override
					protected Void doRun(final CloudFoundryOperations client, SubMonitor progress) throws CoreException {

						// Now verify that the application did start
						try {
							if (!RestartOperation.this.getBehaviour().waitForStart(client, deploymentName, progress)) {
								server.setModuleState(getModules(), IServer.STATE_STOPPED);

								throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
										"Starting of " + cloudModule.getDeployedApplicationName() + " timed out")); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						catch (InterruptedException e) {
							server.setModuleState(getModules(), IServer.STATE_STOPPED);
							throw new OperationCanceledException();
						}

						AbstractAppStateTracker curTracker = CloudFoundryPlugin.getAppStateTracker(
								RestartOperation.this.getBehaviour().getServer().getServerType().getId(), cloudModule);
						if (curTracker != null) {
							curTracker.setServer(RestartOperation.this.getBehaviour().getServer());
							curTracker.startTracking(cloudModule);
						}

						CloudFoundryPlugin.trace("Application " + deploymentName + " started"); //$NON-NLS-1$ //$NON-NLS-2$

						CloudFoundryPlugin.getCallback().applicationStarted(
								RestartOperation.this.getBehaviour().getCloudFoundryServer(), cloudModule);

						if (curTracker != null) {
							// Wait for application to be ready or getting
							// out of the starting state.
							boolean isAppStarting = true;
							while (isAppStarting && !progress.isCanceled()) {
								if (curTracker.getApplicationState(cloudModule) == IServer.STATE_STARTING) {
									try {
										Thread.sleep(200);
									}
									catch (InterruptedException e) {
										// Do nothing
									}
								}
								else {
									isAppStarting = false;
								}
							}
							curTracker.stopTracking(cloudModule);
						}

						server.setModuleState(getModules(), IServer.STATE_STARTED);

						return null;
					}
				}.run(monitor);
			}
			else {
				// User has selected to deploy the app in STOP mode
				server.setModuleState(getModules(), IServer.STATE_STOPPED);
			}
		}
		catch (CoreException e) {
			appModule.setErrorStatus(e);
			server.setModulePublishState(getModules(), IServer.PUBLISH_STATE_UNKNOWN);
			throw e;
		}
	}

	@Override
	protected String getOperationName() {
		return Messages.CONSOLE_RESTARTING_APP;
	}

	@Override
	protected DeploymentConfiguration getDefaultDeploymentConfiguration() {
		return new DeploymentConfiguration(ApplicationAction.RESTART);
	}
}