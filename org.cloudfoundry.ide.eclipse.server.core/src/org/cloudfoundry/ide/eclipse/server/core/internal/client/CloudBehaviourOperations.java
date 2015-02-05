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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * 
 * Creates Cloud operations defined by {@link ICloudFoundryOperation} for start,
 * stopping, publishing, scaling applications, as well as creating, deleting,
 * and binding services.
 * <p/>
 * {@link ICloudFoundryOperation} should be used for performing Cloud operations
 * that require firing server and module refresh events.
 */
public class CloudBehaviourOperations {

	public static String INTERNAL_ERROR_NO_WST_MODULE = "Internal Error: No WST IModule specified - Unable to deploy or start application"; //$NON-NLS-1$

	private final CloudFoundryServerBehaviour behaviour;

	public CloudBehaviourOperations(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	/**
	 * Get operation to create a list of services
	 * @param services
	 * @param monitor
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation createServices(final CloudService[] services) throws CoreException {
		return new UpdateServicesOperation(behaviour.getCreateServicesRequest(services), behaviour);
	}

	/**
	 * Gets an operation to delete Services
	 * @param services
	 * @throws CoreException if operation was not created.
	 */
	public ICloudFoundryOperation deleteServices(final List<String> services) throws CoreException {
		return new UpdateServicesOperation(behaviour.getDeleteServicesRequest(services), behaviour);
	}

	/**
	 * Gets an operation to update the number of application instances. The
	 * operation does not restart the application if the application is already
	 * running. The CF server does allow instance scaling to occur while the
	 * application is running.
	 * @param module representing the application. must not be null or empty
	 * @param instanceCount must be 1 or higher.
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation instancesUpdate(final CloudFoundryApplicationModule appModule, final int instanceCount)
			throws CoreException {

		return new BehaviourOperation(behaviour, appModule.getLocalModule()) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				String appName = appModule.getDeployedApplicationName();

				// Update the instances in the Cloud space
				getBehaviour().updateApplicationInstances(appName, instanceCount, monitor);

				// Refresh the module with the new instances information
				getBehaviour().updateCloudModuleWithInstances(appModule.getLocalModule(), monitor);

				// Fire a separate instances update event to notify listener who
				// are specifically listening
				// to instance changes that do not require a full application
				// refresh event.
				ServerEventHandler.getDefault().fireAppInstancesChanged(behaviour.getCloudFoundryServer(), getModule());

				// Schedule another refresh application operation as instances
				// may take
				// time to be updated (the new instances may have to be
				// restarted in the Cloud Space)
				getBehaviour().getRefreshHandler().schedulesRefreshApplication(getModule());
			}

		};
	}

	/**
	 * Gets an operation that updates an application's memory. The operation
	 * does not restart an application if the application is currently running.
	 * The CF server does allow memory scaling to occur while the application is
	 * running.
	 * @param module must not be null or empty
	 * @param memory must be above zero.
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation memoryUpdate(final CloudFoundryApplicationModule appModule, final int memory)
			throws CoreException {
		return new ApplicationUpdateOperation(behaviour.getUpdateApplicationMemoryRequest(appModule, memory),
				behaviour, appModule);
	}

	/**
	 * Gets an operation to update the application's URL mapping.
	 * @throws CoreException if failed to create the operation
	 */
	public ICloudFoundryOperation mappedUrlsUpdate(final String appName, final List<String> urls) throws CoreException {

		final CloudFoundryApplicationModule appModule = behaviour.getCloudFoundryServer().getExistingCloudModule(
				appName);

		if (appModule != null) {
			return new ApplicationUpdateOperation(behaviour.getUpdateAppUrlsRequest(appName, urls), behaviour,
					appModule.getLocalModule());
		}
		else {
			throw CloudErrorUtil
					.toCoreException("Expected an existing Cloud application module but found none. Unable to update application URLs"); //$NON-NLS-1$
		}
	}

	/**
	 * Gets an operation to update the service bindings of an application
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation bindServices(final CloudFoundryApplicationModule appModule,
			final List<String> services) throws CoreException {
		return new ApplicationUpdateOperation(behaviour.getUpdateServicesRequest(
				appModule.getDeployedApplicationName(), services), behaviour, appModule.getLocalModule());
	}

	/**
	 * Gets an operation that updates the application's environment variables.
	 * Note that the application needs to first exist in the server, and be in a
	 * state that will accept environment variable changes (either stopped, or
	 * running after staging has completed). WARNING: The
	 * {@link CloudApplication} mapping in the module WILL be updated if the
	 * environment variable update is successful, which will replace any
	 * existing deployment info in the app module.
	 * @param appModule
	 * @param monitor
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation environmentVariablesUpdate(final CloudFoundryApplicationModule appModule)
			throws CoreException {
		BaseClientRequest<Void> request = behaviour.getUpdateEnvVarRequest(appModule);
		return new ApplicationUpdateOperation(request, behaviour, appModule.getLocalModule());
	}

	/**
	 * Returns an executable application operation based on the given Cloud
	 * Foundry application module and an application start mode (
	 * {@link ApplicationAction} ).
	 * <p/>
	 * Throws error if failure occurred while attempting to resolve an
	 * operation. If no operation is resolved and no errors occurred while
	 * attempting to resolve an operation, null is returned, meaning that no
	 * operation is currently defined for the given deployment mode.
	 * <p/>
	 * It does NOT execute the operation.
	 * @param application
	 * @param action
	 * @return resolved executable operation associated with the given
	 * deployment mode, or null if an operation could not be resolved.
	 * @throws CoreException
	 */
	public ICloudFoundryOperation applicationDeployment(CloudFoundryApplicationModule application,
			ApplicationAction action) throws CoreException {
		IModule[] modules = new IModule[] { application.getLocalModule() };

		return applicationDeployment(modules, action);
	}

	/**
	 * Resolves an {@link ICloudFoundryOperation} that performs a start, stop,
	 * restart or push operation for the give modules and specified
	 * {@link ApplicationAction}.
	 * <p/>
	 * If no operation can be specified, throws {@link CoreException}
	 * @param modules
	 * @param action
	 * @return Non-null application operation.
	 * @throws CoreException if operation cannot be resolved.
	 */
	public ICloudFoundryOperation applicationDeployment(IModule[] modules, ApplicationAction action)
			throws CoreException {

		if (modules == null || modules.length == 0) {
			throw CloudErrorUtil.toCoreException(INTERNAL_ERROR_NO_WST_MODULE);
		}
		ICloudFoundryOperation operation = null;
		// Set the deployment mode
		switch (action) {
		case START:
			boolean incrementalPublish = false;
			// A start operation that always performs a full publish
			operation = new StartOperation(behaviour, incrementalPublish, modules);
			break;
		case STOP:
			operation = new StopApplicationOperation(behaviour, modules);
			break;
		case RESTART:
			operation = new RestartOperation(behaviour, modules);
			break;
		case UPDATE_RESTART:
			// Check the full publish preference to determine if full or
			// incremental publish should be done when starting an application
			operation = new StartOperation(behaviour, CloudFoundryPlugin.getDefault().getIncrementalPublish(), modules);
			break;
		case PUSH:
			operation = new PushApplicationOperation(behaviour, modules);
			break;
		}

		if (operation == null) {
			throw CloudErrorUtil.toCoreException("Internal Error: Unable to resolve a Cloud application operation."); //$NON-NLS-1$
		}
		return operation;
	}

	/**
	 * Refreshes all modules, services, and the instance info and stats for the
	 * given optional module. If null is passed only the list of modules and
	 * services is refreshed.
	 * <p/>
	 * This may be a long running operation
	 * @return Non-null operation
	 */
	public BehaviourOperation refreshAll(final IModule module) {
		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				if (getModule() != null) {
					getBehaviour().updateCloudModuleWithInstances(getModule(), monitor);
				}
				// Get updated list of cloud applications from the server
				List<CloudApplication> applications = getBehaviour().getApplications(monitor);

				// update applications and deployments from server
				Map<String, CloudApplication> deployedApplicationsByName = new LinkedHashMap<String, CloudApplication>();

				for (CloudApplication application : applications) {
					deployedApplicationsByName.put(application.getName(), application);
				}

				getBehaviour().getCloudFoundryServer().updateModules(deployedApplicationsByName);

				List<CloudService> services = getBehaviour().getServices(monitor);

				ServerEventHandler.getDefault().fireServerEvent(
						new CloudRefreshEvent(getBehaviour().getCloudFoundryServer(), getModule(),
								CloudServerEvent.EVENT_SERVER_REFRESHED, services));
			}
		};
	}

	public BehaviourOperation refreshForDeploymentChange(final IModule module) {
		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				if (module == null) {
					throw CloudErrorUtil.toCoreException("Internal Error: No module to refresh - " + //$NON-NLS-1$
							getBehaviour().getCloudFoundryServer().getServerId());
				}
				getBehaviour().updateCloudModuleWithInstances(module, monitor);
				ServerEventHandler.getDefault().fireAppDeploymentChanged(behaviour.getCloudFoundryServer(), module);
			}
		};
	}

	/**
	 * Refreshes the instances of given module. The module must not be null.
	 * @param module
	 * @return Non-null operation.
	 */
	public BehaviourOperation refreshApplication(final IModule module) {

		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				if (module == null) {
					throw CloudErrorUtil.toCoreException("Internal Error: No module to refresh - " + //$NON-NLS-1$
							getBehaviour().getCloudFoundryServer().getServerId());
				}
				getBehaviour().updateCloudModuleWithInstances(module, monitor);
				ServerEventHandler.getDefault().fireApplicationRefreshed(behaviour.getCloudFoundryServer(), module);
			}
		};
	}

	public ICloudFoundryOperation deleteModules(IModule[] modules, final boolean deleteServices) {
		return new DeleteModulesOperation(behaviour, modules, deleteServices);
	}

}
