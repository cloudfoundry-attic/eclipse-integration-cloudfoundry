/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.ide.eclipse.server.core.AbstractApplicationDelegate;
import org.cloudfoundry.ide.eclipse.server.core.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.server.core.ICloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ApplicationRegistry;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.ExternalModule;

/**
 * 
 * Representation of an application that either already exists in a Cloud
 * Foundry server, or is about to be pushed to a server, and contains additional
 * properties local to the plugin framework not found in a
 * {@link CloudApplication}. It contains additional Cloud Foundry information
 * like a deployment information ( {@link ApplicationDeploymentInfo} ),
 * application stats, instances, and staging that is not available from the
 * local WST {@link IModule}. A cloud foundry application module need not
 * necessarily indicate that the application exists and is deployed in a CF
 * server. Modules are also created by the framework PRIOR to deploying an app.
 * If the module has a corresponding {@link CloudApplication}, it means that the
 * module does indeed represent an actual deployed app in a CF server. In
 * addition:
 * <p/>
 * 1. A Cloud module can be external, meaning that the deployed application does
 * not have an accessible workspace project. If this is the case, generally the
 * module will also have a mapped {@link CloudApplication}, since only
 * applications that have already been deployed can ever be external. Note
 * although all external apps are apps that are deployed in a Cloud server, not
 * all deployed apps are external. "External" is not the only indication of
 * deployment, rather it is meant to indicate whether the application is linked
 * to a local, accessible workspace project.
 * <p/>
 * 2. A Cloud module can also be mapped to a local workspace project via a local
 * {@link IModule}, in which case it would not be classified as external. This
 * does NOT mean that the application is not deployed. The application may be
 * deployed in a CF server, but also have a link to a local workspace project.
 * An application may also have a link to a local workspace project, but NOT yet
 * be deployed (so it wouldn't have a mapped {@link CloudApplication}).
 * 
 * <p/>
 * The application name of this CF-aware module may differ from the module name
 * of the local WST {@link IModule}. The reason is that the module name of the
 * local WST {@link IModule} is typically the associated workspace project, if
 * the project is accessible, while the application name in the CF Application
 * Module is the user-specified CF app name, which may be different.
 * <p/>
 * To obtain the local WST module name, use {@link #getName()} or get it through
 * {@link #getLocalModule()}, although the latter may be null if no IModule
 * mapping has been created and linked by the framework. Local names may be used
 * for obtaining workspace resources, like for example the application's
 * corresponding workspace project.
 * <p/>
 * To obtain the deployed application name, use
 * {@link #getDeployedApplicationName()}.
 * <p/>
 * The application module may be shared by multiple threads, therefore changes
 * should be synchronised at the very least.
 * <p/>
 * The app module also contains a deployment information (
 * {@link ApplicationDeploymentInfo} ), which describes deployment properties of
 * the application (e.g., URLs, memory settings, etc..), as well as services
 * that are bound, or will be bound, to the application.
 * <p/>
 * If the application has already been deployed (i.e. has a corresponding
 * {@link CloudApplication}), the deployment information is kept in synch any
 * time the module mapping to a {@link CloudApplication} is changed.
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters. 
 * 
 * @author Nieraj Singh
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class CloudFoundryApplicationModule extends ExternalModule implements ICloudFoundryApplicationModule {

	public static String APPLICATION_STATE_DEPLOYABLE = "Deployable";

	public static String APPLICATION_STATE_DEPLOYED = "Deployed";

	public static String APPLICATION_STATE_UPLOADING = "Uploading";

	public static String DEPLOYMENT_STATE_LAUNCHED = "LAUNCHED";

	public static String DEPLOYMENT_STATE_LAUNCHING = "LAUNCHING";

	public static String DEPLOYMENT_STATE_STARTING_SERVICES = "STARTING_SERVICES";

	public static String DEPLOYMENT_STATE_STOPPED = "STOPPED";

	public static String DEPLOYMENT_STATE_STOPPING = "STOPPING";

	public static String DEPLOYMENT_STATE_WAITING_TO_LAUNCH = "WAITING_TO_LAUNCH";

	private static final String MODULE_ID = "org.cloudfoundry.ide.eclipse.server.core.CloudFoundryApplicationModule";

	private static final String MODULE_VERSION = "1.0";

	private CloudApplication application;

	private String deployedAppName;

	private ApplicationStats applicationStats;

	private InstancesInfo instancesInfo;

	private ApplicationDeploymentInfo deploymentInfo;

	private StartingInfo startingInfo;

	private IModule localModule;

	private final IServer server;

	private CoreException error;

	/**
	 * Creates a cloud module that has a corresponding local module. This should
	 * be used if there is an accessible workspace project for the deployed app
	 * (the presence of an IModule would indicate a possible accessible
	 * workspace resource for the application).
	 * @param module local module from the WST server. Must not be null.
	 * @param deployedApplicationName name of the deployed application. It may
	 * not match the local workspace project name, as users are allowed to
	 * specify a different deployment name when pushing an application. Must not
	 * be null
	 * @param server. Must not be null.
	 */
	public CloudFoundryApplicationModule(IModule module, String deployedApplicationName, IServer server) {
		this(module, deployedApplicationName, module.getName(), server);
	}

	/**
	 * Creates an external cloud module (a cloud module that corresponds to a
	 * deployed application with no accessible workspace project).
	 * @param deployedApplicationName. Must not be null.
	 * @param server. Must not be null.
	 */
	public CloudFoundryApplicationModule(String deployedApplicationName, IServer server) {
		this(null, deployedApplicationName, deployedApplicationName, server);
	}

	protected CloudFoundryApplicationModule(IModule module, String deployedApplicationName, String localName,
			IServer server) {
		super(localName, localName, MODULE_ID, MODULE_VERSION, null);
		Assert.isNotNull(deployedApplicationName);
		Assert.isNotNull(localName);
		Assert.isNotNull(server);
		this.localModule = (module != null) ? module : this;
		this.server = server;
		setDeployedApplicationName(deployedApplicationName);
		CloudFoundryPlugin.trace("Created ApplicationModule " + deployedApplicationName + " for module " + module);
	}

	/**
	 * A mapping to a cloud application representing a deployed application. A
	 * non-null cloud application means that the application is already deployed
	 * and exists in the CF server.
	 * <p/>
	 * If cloud application is null, it means the application module has not yet
	 * been deployed, or there was an error mapping the local application module
	 * with the actual deployed application (e.g. a connection error when trying
	 * to refresh the list of currently deployed applications).
	 * @return the actual cloud application obtained from the CF client library
	 * indicating a deployed application. It may be null.
	 */
	public CloudApplication getApplication() {
		return application;
	}

	/**
	 * The deployed application name. This may be different from the IModule
	 * name which is typically the project name (if accessible). Therefore to
	 * get the name of the actual app, always use this API. To get the local
	 * module name use {@link #getName()}, which matches the local workspace
	 * project, or get it through the IModule itself {@link #getLocalModule()}.
	 * @see IModule#getName()
	 * @see #getLocalModule()
	 */
	public synchronized String getDeployedApplicationName() {
		return deployedAppName;
	}

	public ApplicationStats getApplicationStats() {
		return applicationStats;
	}

	public StartingInfo getStartingInfo() {
		return startingInfo;
	}

	public void setStartingInfo(StartingInfo startingInfo) {
		this.startingInfo = startingInfo;
	}

	public InstancesInfo getInstancesInfo() {
		return instancesInfo;
	}

	public synchronized int getInstanceCount() {
		if (application != null) {
			return application.getInstances();
		}
		return 0;
	}

	/**
	 * Returns a copy of the application's deployment info describing deployment
	 * properties for the application like the application's memory settings,
	 * mapped URLs and bound services.
	 * <p/>
	 * Changes to the copy will have no effect. To make changes to the
	 * deployment information, request a working copy, and save it. See
	 * {@link #getDeploymentInfoWorkingCopy()}
	 * 
	 * <p/>
	 * If null, it means that the application is not currently deployed in the
	 * server, or the plugin has not yet determined if the application is
	 * deployed.
	 * <p/>
	 * If not null, it does NOT necessarily mean the application is deployed, as
	 * the application may be in the process of being deployed and will
	 * therefore have a deployment information.
	 * @return a copy of the application's deployment information. Changes to
	 * the copy will have no effect.
	 */
	public synchronized ApplicationDeploymentInfo getDeploymentInfo() {
		return deploymentInfo != null ? deploymentInfo.copy() : null;
	}

	/**
	 * Creates a working copy of the current deployment information. If the
	 * application does not have a current deployment information, a working
	 * copy will be generated from the app's deployment default values. A new
	 * copy is always returned. No changes take effect in the app modules'
	 * deployment info unless the working copy is saved.
	 * <p/>
	 * @return a new working copy with either existing deployment information,
	 * or default deployment information, if an deployment information does not
	 * exist.
	 */
	public synchronized DeploymentInfoWorkingCopy resolveDeploymentInfoWorkingCopy(IProgressMonitor monitor)
			throws CoreException {
		DeploymentInfoWorkingCopy wc = new ModuleDeploymentInfoWorkingCopy(this);
		wc.fill(monitor);
		return wc;
	}

	/**
	 * 
	 * @see AbstractApplicationDelegate#validateDeploymentInfo(ApplicationDeploymentInfo)
	 * @return OK status if deployment information is complete and valid. Error
	 * if failed to validate, or is invalid (i.e. it is missing information).
	 */
	public synchronized IStatus validateDeploymentInfo() {
		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(getLocalModule());
		if (delegate == null) {
			return AbstractApplicationDelegate.basicValidateDeploymentInfo(deploymentInfo);
		}
		return delegate.validateDeploymentInfo(deploymentInfo);
	}

	/**
	 * 
	 * Returns the local WST module mapping. If present (not null), it most
	 * likely means that there is an accessible Eclipse workspace project for
	 * the application. If null, it means the application is external, which
	 * indicates that it is deployed in a CF server but does not have an
	 * accessible workspace project.
	 * 
	 * @return local WST module. May be null if the application is external.
	 */
	public IModule getLocalModule() {
		return localModule;
	}

	public int getPublishState() {
		// if (isExternal()) {
		return IServer.PUBLISH_STATE_NONE;
		// }
		// return IServer.PUBLISH_STATE_UNKNOWN;
	}

	public String getServerTypeId() {
		return server.getServerType().getId();
	}

	/**
	 * 
	 * @return {@link IServer} state
	 */
	public synchronized int getState() {
		if (application != null) {
			AppState state = application.getState();
			switch (state) {
			case STARTED:
				return IServer.STATE_STARTED;
			case UPDATING:
				return IServer.STATE_STARTING;
			case STOPPED:
				return IServer.STATE_STOPPED;
			}
		}
		return IServer.STATE_UNKNOWN;
	}

	public boolean isExternal() {
		return localModule == this;
	}

	public synchronized void setErrorStatus(CoreException error) {
		this.error = error;
	}

	public synchronized String getErrorMessage() {
		if (error == null) {
			return null;
		}
		return error.getMessage();
	}

	public synchronized void setApplicationStats(ApplicationStats applicationStats) {
		this.applicationStats = applicationStats;
	}

	public synchronized void setInstancesInfo(InstancesInfo instancesInfo) {
		this.instancesInfo = instancesInfo;
	}

	/**
	 * Maps the application module to an actual deployed application in a CF
	 * server. It replaces any existing deployment info with one generated from
	 * the cloud application. The existing deployment descriptor remains
	 * unchanged if removing the cloud application mapping (i.e. setting to
	 * null)
	 * 
	 * @param cloudApplication the actual deployed application in a CF server.
	 * @throws CoreException if failure occurred while setting a cloud
	 * application, or the deployment info is currently being modified by some
	 * other component.
	 */
	public synchronized void setCloudApplication(CloudApplication cloudApplication) {
		this.application = cloudApplication;

		if (application != null) {
			// Update the deployment info so that it reflects the actual
			// deployed
			// application. Note that Eclipse-specific properties are retained
			// from
			// existing deployment infos.
			// Only the actual deployed app properties (e.g. name, services,
			// URLs)
			// are updated from the cloud application
			ApplicationDeploymentInfo cloudApplicationInfo = resolveDeployedApplicationInformation();
			if (cloudApplicationInfo != null) {
				internalSetDeploymentInfo(cloudApplicationInfo);
			}
		}
	}

	/**
	 * 
	 * @return true if the application is published to the Cloud Foundry server.
	 * False otherwise.
	 */
	public synchronized boolean isDeployed() {
		return getApplication() != null;
	}

	/**
	 * Sets a deployment information for the application. Note that if the
	 * application is already deployed (i.e. a {@link CloudApplication} mapping
	 * exists for this module), this will overwrite the deployment information
	 * for the {@link CloudApplication}.
	 * @param lastDeploymentInfo the latest deployment of the application. IF
	 * the application name in the latest deployment has changed, the current
	 * module name will also be updated. If setting null (e.g. application is
	 * being deleted), the current module name will remain unchanged.
	 */
	private void internalSetDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		this.deploymentInfo = deploymentInfo;
		// Note that last Deployment info may be null (e.g. when deleting an
		// application). Only update the appliation ID if setting a new last
		// deployment info, since
		// the module should match the application properties listed in the
		// latest deployment, including any app name changes.
		if (deploymentInfo != null && deploymentInfo.getDeploymentName() != null) {
			setDeployedApplicationName(deploymentInfo.getDeploymentName());
		}
	}

	/*
	 * 
	 * Internal helper methods. Non-synchronized
	 */

	/**
	 * Resolve deployment information from values in the corresponding deployed
	 * application ( {@link CloudApplication} ). If the application is not yet
	 * deployed (i.e., cloud application is null), null is returned.
	 * 
	 * @param appModule application currently deployed in CF server
	 * @param cloudServer server where app is deployed
	 * @return a new copy of the deployment info for the deployed app, or null
	 * if the cloud application is null
	 */
	protected ApplicationDeploymentInfo resolveDeployedApplicationInformation() {
		if (application == null) {
			return null;
		}

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(getLocalModule());
		ApplicationDeploymentInfo info = null;
		CloudFoundryServer cloudServer = getCloudFoundryServer();

		if (delegate != null) {
			info = delegate.resolveApplicationDeploymentInfo(this, cloudServer);
		}

		// If no info has been resolved yet, use a default parser
		if (info == null) {
			info = AbstractApplicationDelegate.parseApplicationDeploymentInfo(application);
		}

		return info;
	}

	/**
	 * Application name must not be null. This is the deployed application name.
	 * @param applicationName most not be null
	 */
	protected void setDeployedApplicationName(String applicationName) {
		Assert.isNotNull(applicationName);
		if (!applicationName.equals(this.deployedAppName)) {
			this.deployedAppName = applicationName;
			if (localModule != null) {
				CloudFoundryServer cloudServer = getCloudFoundryServer();

				// Since the deployment name changed, update the local module ->
				// deployed module cache in the server
				cloudServer.updateApplicationModule(this);
			}
		}
	}

	/**
	 * Returns a default deployment information, with basic information to
	 * deploy or start/restart an application. It is not guaranteed to be
	 * complete or valid, as in some cases missing information is acceptable
	 * since additional deployment steps may involve prompting for the missing
	 * values.
	 * <p/>
	 * Never null. At the very basic, it will set a simple default deployment
	 * information with just the application name and memory setting.
	 * @return non-null default deployment info. This default information is
	 * also set in the module as the module's current deployment information.
	 */
	protected ApplicationDeploymentInfo getDefaultDeploymentInfo(IProgressMonitor monitor) throws CoreException {

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(getLocalModule());
		ApplicationDeploymentInfo defaultInfo = null;

		if (delegate != null) {
			defaultInfo = delegate.getDefaultApplicationDeploymentInfo(this, getCloudFoundryServer(), monitor);
		}

		if (defaultInfo == null) {
			defaultInfo = createGeneralDefaultInfo();
		}

		return defaultInfo;
	}

	/**
	 * Creates a general deployment info that should be applicable to any
	 * application type. It will have an app name as well as memory setting.
	 * @return Non-null general deployment info with basic information for
	 * application deployment.
	 */
	protected ApplicationDeploymentInfo createGeneralDefaultInfo() {
		ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeployedApplicationName());
		info.setMemory(CloudUtil.DEFAULT_MEMORY);
		return info;
	}

	protected CloudFoundryServer getCloudFoundryServer() {
		return (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
	}

	/**
	 * Should not be instantiated outside of a Cloud Module, as it is coupled
	 * with the implementation of the module.
	 */
	protected class ModuleDeploymentInfoWorkingCopy extends DeploymentInfoWorkingCopy {

		protected ModuleDeploymentInfoWorkingCopy(CloudFoundryApplicationModule appModule) {
			super(appModule);
		}

		@Override
		public void save() {
			synchronized (appModule) {

				// Set the working copy as a regular deployment info, as to not
				// keeping
				// a reference to the working copy
				ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeployedApplicationName());
				info.setInfo(this);
				appModule.internalSetDeploymentInfo(info);
			}
		}
	}

}
