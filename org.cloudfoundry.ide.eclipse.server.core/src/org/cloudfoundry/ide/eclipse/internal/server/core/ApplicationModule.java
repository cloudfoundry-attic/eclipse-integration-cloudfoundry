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
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class ApplicationModule extends ExternalModule {

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

	private IModule localModule;

	private final IServer server;

	private CoreException error;

	public ApplicationModule(IModule module, String name, IServer server) {
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

	public synchronized String getApplicationId() {
		return applicationId;
	}

	public ApplicationStats getApplicationStats() {
		return applicationStats;
	}

	public InstancesInfo getInstancesInfo() {
		return instancesInfo;
	}

	public Staging getStaging() {
		return staging;
	}

	public String getDefaultLaunchUrl() {
		return getLaunchUrl(getName());
	}

	public String getLaunchUrl(String appName) {
		// replace first segment of server url with app name
		appName = appName.toLowerCase();
		String url = server.getAttribute(CloudFoundryServer.PROP_URL, "");
		url = url.replace("http://", "");
		String prefix = url.split("\\.")[0];
		return url.replace(prefix, appName);
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
