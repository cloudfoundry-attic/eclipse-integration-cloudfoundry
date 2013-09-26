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

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.ExternalModule;

/**
 * 
 * Representation of an application published to a Cloud Foundry server,
 * containing addition Cloud Foundry information like last deployment
 * information, application stats, instances, and staging that is not available
 * from the local WST {@link IModule}.
 * <p/>
 * 1. A Cloud module can be external, meaning that the deployed application does
 * not have an accessible workspace project.
 * <p/>
 * 2. A Cloud module can also be mapped to a local workspace project via a local
 * {@link IModule}, in which case it would not be classified as external.
 * 
 * <p/>
 * Note that the application name of this CF-aware module may differ from the
 * module name of the local WST {@link IModule}. The reason is that the module
 * name of the local WST {@link IModule} is typically the associated workspace
 * project, if the project is accessible, while the application name in the CF
 * Application Module is the user-specified CF app name, which may be different.
 * <p/>
 * To obtain the local WST module name, use {@link #getName()} or get it through
 * {@link #getLocalModule()}, although the latter may be null if no IModule
 * mapping has been made by the framework. Local names may be used for obtaining
 * workspace resources, like for example the application's corresponding
 * workspace project.
 * <p/>
 * To obtain the deployed application name, use
 * {@link #getDeployedApplicationName()}.
 * 
 * 
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class CloudFoundryApplicationModule extends ExternalModule {

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

	private ApplicationDeploymentInfo lastDeploymentInfo;

	private Staging staging;

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

	public Staging getStaging() {
		return staging;
	}

	public synchronized int getInstanceCount() {
		if (application != null) {
			return application.getInstances();
		}
		return 0;
	}

	/**
	 * @return lastDeploymentInfo the latest deployment of the application. Note
	 * that the application ID (i.e. the name) . If null, it means the
	 * application either has not yet been pushed to a CF server, or it has been
	 * deleted.
	 */
	public synchronized ApplicationDeploymentInfo getLastDeploymentInfo() {
		return lastDeploymentInfo;
	}

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

	/**
	 * Application name must not be null. This is the deployed application name.
	 * @param applicationName most not be null
	 */
	private void setDeployedApplicationName(String applicationName) {
		Assert.isNotNull(applicationName);
		if (!applicationName.equals(this.deployedAppName)) {
			this.deployedAppName = applicationName;
			if (localModule != null) {
				CloudFoundryServer cloudServer = (CloudFoundryServer) server
						.loadAdapter(CloudFoundryServer.class, null);

				// Since the deployment name changed, update the local module ->
				// deployed module cache in the server
				cloudServer.updateApplicationModule(this);
			}
		}
	}

	public void setStaging(Staging staging) {
		this.staging = staging;
	}

	public void setApplicationStats(ApplicationStats applicationStats) {
		this.applicationStats = applicationStats;
	}

	public void setInstancesInfo(InstancesInfo instancesInfo) {
		this.instancesInfo = instancesInfo;
	}

	public synchronized void setCloudApplication(CloudApplication cloudApplication) {
		this.application = cloudApplication;
		if (cloudApplication != null) {
			setDeployedApplicationName(cloudApplication.getName());
		}
	}

	/**
	 * 
	 * @param lastDeploymentInfo the latest deployment of the application. IF
	 * the application name in the latest deployment has changed, the current
	 * module name will also be updated. If setting null (e.g. application is
	 * being deleted), the current module name will remain unchanged.
	 */
	public synchronized void setLastDeploymentInfo(ApplicationDeploymentInfo lastDeploymentInfo) {
		this.lastDeploymentInfo = lastDeploymentInfo;
		// Note that last Deployment info may be null (e.g. when deleting an
		// application). Only update the appliation ID if setting a new last
		// deployment info, since
		// the module should match the application properties listed in the
		// latest deployment, including any app name changes.
		if (lastDeploymentInfo != null && lastDeploymentInfo.getDeploymentName() != null) {
			setDeployedApplicationName(lastDeploymentInfo.getDeploymentName());
		}
	}

	public synchronized void setErrorStatus(CoreException error) {
		this.error = error;
	}

	public String getErrorMessage() {
		if (error == null) {
			return null;
		}
		return error.getMessage();
	}

}
