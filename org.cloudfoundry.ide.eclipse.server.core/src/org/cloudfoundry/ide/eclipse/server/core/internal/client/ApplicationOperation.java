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

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Deploys an application and or starts it in regular or debug mode. If deployed
 * in debug mode, an attempt will be made to connect the deployed application to
 * a debugger. An operation should performed atomically PER APPLICATION.
 * <p/>
 * The operation performs some common tasks like checking that the application's
 * deployment info is complete and valid, and that any refresh jobs running in
 * the background are stopped prior to starting the operation, and restarted
 * afterward.
 * 
 */
@SuppressWarnings("restriction")
public abstract class ApplicationOperation extends AbstractPublishApplicationOperation {

	/**
	 * 
	 */
	private DeploymentConfiguration configuration;

	protected ApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour, modules);
	}

	/**
	 * The local configuration for the app. It indicates what deployment mode
	 * the app should be launched in (e.g. START, STOP..). If a configuration
	 * cannot be resolved, a default one will be returned instead.
	 * @return deployment configuration. Never null.
	 */
	protected DeploymentConfiguration getDeploymentConfiguration() {
		if (configuration == null) {
			configuration = getDefaultDeploymentConfiguration();
		}
		return configuration;
	}

	@Override
	protected void doApplicationOperation(IProgressMonitor monitor) throws CoreException {

		// Given that we only look at the root module for generating the
		// appModule
		// ie: indicated by the following
		// getOrCreateCloudApplicationModule() call
		// we should ignore child modules of this root module so that
		// we don't prompt multiple wizards for the same root module during
		// deployment

		if (getModules().length != 1) {
			return;
		}

		CloudFoundryApplicationModule appModule = getOrCreateCloudApplicationModule(getModules());

		try {

			CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

			// Stop any consoles
			CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, cloudServer);

			SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

			configuration = prepareForDeployment(appModule, subMonitor.newChild(20));

			IStatus validationStatus = appModule.validateDeploymentInfo();
			if (!validationStatus.isOK()) {
				throw CloudErrorUtil.toCoreException(NLS.bind(Messages.ERROR_APP_DEPLOYMENT_VALIDATION_ERROR,
						appModule.getDeployedApplicationName(), validationStatus.getMessage()));

			}

			// NOTE: Only print to a console AFTER an application has been
			// prepared for deployment, as the application
			// name may have changed during the deployment preparation
			// stage, and consoles are mapped by application name.
			// This prevents two different consoles with different names
			// from appearing for the same application
			getBehaviour().clearAndPrintlnConsole(appModule,
					NLS.bind(Messages.CONSOLE_PREPARING_APP, appModule.getDeployedApplicationName()));

			performDeployment(appModule, subMonitor.newChild(60));

			// If deployment was successful, update the module
			appModule = getBehaviour().updateCloudModuleWithInstances(appModule.getDeployedApplicationName(),
					subMonitor.newChild(20));

		}
		catch (CoreException ce) {
			// Log the error in console
			getBehaviour().printErrorlnToConsole(appModule, ce.getMessage());
			throw ce;
		}

	}

	/**
	 * Prepares an application to either be deployed, started or restarted. The
	 * main purpose to ensure that the application's deployment information is
	 * complete. If incomplete, it will prompt the user for missing information.
	 * @param monitor
	 * @return Deployment configuration, or null if default configuration should
	 * be used.
	 * @throws CoreException if any failure during or after the operation.
	 * @throws OperationCanceledException if the user cancelled deploying or
	 * starting the application. The application's deployment information should
	 * not be modified in this case.
	 */
	protected DeploymentConfiguration prepareForDeployment(CloudFoundryApplicationModule appModule,
			IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		return null;
	}

	/**
	 * 
	 * @return default deployment configuration, that among other things
	 * determines the deployment mode of an application (for example, START,
	 * STOP, RESTART). Must not be null.
	 */
	protected abstract DeploymentConfiguration getDefaultDeploymentConfiguration();

	/**
	 * 
	 * @param appModule to be deployed or started
	 * @param monitor
	 * @throws CoreException if error occurred during deployment or starting the
	 * app, or resolving the updated cloud application from the client.
	 */
	protected abstract void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException;

}