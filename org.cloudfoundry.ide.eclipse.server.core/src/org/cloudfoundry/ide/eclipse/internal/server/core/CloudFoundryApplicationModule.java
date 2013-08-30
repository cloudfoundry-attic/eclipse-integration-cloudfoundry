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

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.ExternalModule;

/**
 * 
 * Representation of an application published to a Cloud Foundry server,
 * containing addition Cloud Foundry information like last deployment and
 * application information. Note that the application ID of this CF aware module
 * may differ from the module name of its corresponding WTP IModule. The reason
 * is that the module name of the WTP IModule is typically the associated
 * workspace project, while the application ID in the CF Application Module is
 * the user-specified CF app name, which may be different.
 * 
 * 
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
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

	private String applicationId;

	private ApplicationStats applicationStats;

	private InstancesInfo instancesInfo;

	private ApplicationInfo lastApplicationInfo;

	private DeploymentInfo lastDeploymentInfo;

	private Staging staging;

	private StartingInfo startingInfo;

	private IModule localModule;

	private final IServer server;

	private CoreException error;

	public CloudFoundryApplicationModule(IModule module, String name, IServer server) {
		super(name, name, MODULE_ID, MODULE_VERSION, null);
		Assert.isNotNull(name);
		Assert.isNotNull(server);
		this.localModule = (module != null) ? module : this;
		this.server = server;
		this.applicationId = name;
		CloudFoundryPlugin.trace("Created ApplicationModule " + name + " for module " + module);
	}

	public CloudApplication getApplication() {
		return application;
	}

	/**
	 * The Cloud Foundry application name. This may not necessarily be the same
	 * as the associated WTP IModule name or workspace project name, as users
	 * are allowed to enter a different name for the application when pushing
	 * the application to a Cloud Foundry server.
	 * @return Cloud Foundry application name.
	 */
	public synchronized String getApplicationId() {
		return applicationId;
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

	public synchronized ApplicationInfo getLastApplicationInfo() {
		return lastApplicationInfo;
	}

	public synchronized DeploymentInfo getLastDeploymentInfo() {
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

	public void setApplicationId(String applicationId) {
		Assert.isNotNull(applicationId);
		if (!this.applicationId.equals(applicationId)) {
			this.applicationId = applicationId;
			if (localModule != null) {
				CloudFoundryServer cloudServer = (CloudFoundryServer) server
						.loadAdapter(CloudFoundryServer.class, null);
				cloudServer.updateApplication(this);
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
			setApplicationId(cloudApplication.getName());
		}
	}

	public synchronized void setLastApplicationInfo(ApplicationInfo lastApplicationInfo) {
		Assert.isNotNull(lastApplicationInfo);
		this.lastApplicationInfo = lastApplicationInfo;
		setApplicationId(lastApplicationInfo.getAppName());
	}

	public synchronized void setLastDeploymentInfo(DeploymentInfo lastDeploymentInfo) {
		this.lastDeploymentInfo = lastDeploymentInfo;
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
