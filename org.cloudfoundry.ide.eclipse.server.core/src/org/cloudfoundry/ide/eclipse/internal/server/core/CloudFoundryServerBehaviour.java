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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudApplication.AppState;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.InstancesInfo;
import org.cloudfoundry.client.lib.ServiceConfiguration;
import org.cloudfoundry.client.lib.Staging;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback.DeploymentDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommandBuilder;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugModeType;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneUtil;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class CloudFoundryServerBehaviour extends ServerBehaviourDelegate {

	private static final long DEFAULT_INTERVAL = 60 * 1000;

	private static final long DEPLOYMENT_TIMEOUT = 10 * 60 * 1000;

	private static final long SHORT_INTERVAL = 5 * 1000;

	private static final long UPLOAD_TIMEOUT = 60 * 1000;

	private CloudFoundryClient client;

	private RefreshJob refreshJob;

	private DebugSupportCheck isDebugModeSupported = DebugSupportCheck.UNCHECKED;

	private IServerListener serverListener = new IServerListener() {

		public void serverChanged(ServerEvent event) {
			if (event.getKind() == ServerEvent.SERVER_CHANGE) {
				// reset client to consume updated credentials
				resetClient();
			}
		}
	};

	protected enum DebugSupportCheck {
		// Initial state of the debug support check. used so that further checks
		// are not necessary in a given session
		UNCHECKED,
		// Server supports debug mode
		SUPPORTED,
		// Server does not support debug mode
		UNSUPPORTED,
	}

	@Override
	public boolean canControlModule(IModule[] module) {
		return module.length == 1;
	}

	public void connect(IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();

		new Request<Void>(NLS.bind("Loggging in to {0}", cloudServer.getUrl())) {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.login();
				doRefreshModules(cloudServer, client, progress);

				return null;
			}
		}.run(monitor);

		Server server = (Server) cloudServer.getServerOriginal();
		server.setServerState(IServer.STATE_STARTED);
		server.setServerPublishState(IServer.PUBLISH_STATE_NONE);

		CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);
	}

	/**
	 * Determine if server supports debug mode, if necessary by sending a
	 * request to the server. The information is cached for quicker, subsequent
	 * checks.
	 * 
	 */
	protected synchronized void requestAllowDebug(CloudFoundryClient client) throws CoreException {
		// Check the debug support of the server once per working copy of server
		if (isDebugModeSupported == DebugSupportCheck.UNCHECKED) {
			isDebugModeSupported = client.getCloudInfo().getAllowDebug() ? DebugSupportCheck.SUPPORTED
					: DebugSupportCheck.UNSUPPORTED;
		}
	}

	/**
	 * Creates the given list of services
	 * @param services
	 * @param monitor
	 * @throws CoreException
	 */
	public void createService(final CloudService[] services, IProgressMonitor monitor) throws CoreException {

		new Request<Void>(services.length == 1 ? NLS.bind("Creating service {0}", services[0].getName())
				: "Creating services") {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {

				for (CloudService service : services) {
					client.createService(service);
				}

				return null;
			}
		}.run(monitor);
		CloudFoundryPlugin.getDefault().fireServicesUpdated(getCloudFoundryServer());
	}

	public void deleteModules(final IModule[] modules, final boolean deleteServices, IProgressMonitor monitor)
			throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();
		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				for (IModule module : modules) {
					final ApplicationModule appModule = cloudServer.getApplication(module);

					List<String> servicesToDelete = new ArrayList<String>();

					List<CloudApplication> applications = client.getApplications();

					for (CloudApplication application : applications) {
						if (application.getName().equals(appModule.getApplicationId())) {
							// Fix for STS-2416: Get the CloudApplication from
							// the client again, as the CloudApplication
							// associate with the WTP ApplicationModule may be
							// out of date and have an out of date list of
							// services.
							List<String> actualServices = application.getServices();
							if (actualServices != null) {
								servicesToDelete.addAll(actualServices);
							}

							// Close any Caldecott tunnels before deleting app
							if (CaldecottTunnelHandler.isCaldecottApp(appModule.getApplicationId())) {
								// Delete all tunnels if the Caldecott app is
								// removed
								new CaldecottTunnelHandler(cloudServer).stopAndDeleteAllTunnels(progress);
							}

							client.deleteApplication(appModule.getApplicationId());

							break;
						}
					}
					cloudServer.removeApplication(appModule);
					appModule.setLastDeploymentInfo(null);
					appModule.setCloudApplication(null);

					if (deleteServices && !servicesToDelete.isEmpty()) {
						CloudFoundryPlugin.getCallback().deleteServices(servicesToDelete, cloudServer);
						CloudFoundryPlugin.getDefault().fireServicesUpdated(cloudServer);
					}

				}
				return null;
			}
		}.run(monitor);
	}

	public void deleteServices(final List<String> services, IProgressMonitor monitor) throws CoreException {
		new Request<Void>("Deleting services") {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				CaldecottTunnelHandler handler = new CaldecottTunnelHandler(getCloudFoundryServer());
				for (String service : services) {
					client.deleteService(service);

					// Also delete any existing Tunnels
					handler.stopAndDeleteCaldecottTunnel(service, progress);
				}
				return null;
			}
		}.run(monitor);
	}

	private CloudApplication doDeployApplication(CloudFoundryClient client, final ApplicationModule appModule,
			final DeploymentDescriptor descriptor, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(descriptor.applicationInfo);

		ApplicationInfo applicationInfo = descriptor.applicationInfo;
		String applicationId = applicationInfo.getAppName();

		appModule.setLastApplicationInfo(applicationInfo);
		appModule.setLastDeploymentInfo(descriptor.deploymentInfo);

		// publish application
		try {
			List<CloudApplication> existingApps = client.getApplications();
			boolean found = false;
			for (CloudApplication existingApp : existingApps) {
				if (existingApp.getName().equals(applicationId)) {
					found = true;
					break;
				}
			}

			if (!found) {
				Staging staging = descriptor.staging;
				if (StandaloneUtil.isStandaloneApp(appModule) && StandaloneUtil.isValidStaging(staging)) {
					List<String> uris = descriptor.deploymentInfo.getUris() != null ? descriptor.deploymentInfo
							.getUris() : new ArrayList<String>();
					List<String> services = descriptor.deploymentInfo.getServices() != null ? descriptor.deploymentInfo
							.getServices() : new ArrayList<String>();
					client.createApplication(applicationId, staging, descriptor.deploymentInfo.getMemory(), uris,
							services);
				}
				else {

					client.createApplication(applicationId, applicationInfo.getFramework(),
							descriptor.deploymentInfo.getMemory(), descriptor.deploymentInfo.getUris(),
							descriptor.deploymentInfo.getServices());
				}
			}
			File warFile = applicationInfo.getWarFile();

			if (warFile != null) {
				client.uploadApplication(applicationId, warFile);
			}
			else {
				ApplicationArchive archive = descriptor.applicationArchive;
				if (archive instanceof ModuleResourceApplicationArchive) {
					final ModuleResourceApplicationArchive moduleArchive = (ModuleResourceApplicationArchive) archive;
					client.uploadApplication(applicationId, archive, new UploadStatusCallback() {

						public void onProcessMatchedResources(int length) {

						}

						public void onMatchedFileNames(Set<String> matchedFileNames) {
							moduleArchive.generatePartialWarFile(matchedFileNames);
						}

						public void onCheckResources() {

						}
					});

					// Once the application has run, do a clean up of the sha1
					// cache for deleted resources

				}
				else {
					client.uploadApplication(applicationId, archive);
				}
			}

		}
		catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
					"Failed to deploy application from {0}", applicationInfo.getWarFile()), e));
		}

		try {
			CloudApplication application = waitForUpload(client, applicationId, monitor);
			appModule.setCloudApplication(application);
			return application;
		}
		catch (InterruptedException e) {
			throw new OperationCanceledException();
		}
	}

	protected List<IModuleResource> getChangedResources(IModuleResourceDelta[] deltas) {
		List<IModuleResource> changed = new ArrayList<IModuleResource>();
		if (deltas != null) {
			findNonChangedResources(deltas, changed);
		}
		return changed;

	}

	protected void findNonChangedResources(IModuleResourceDelta[] deltas, List<IModuleResource> changed) {
		if (deltas == null || deltas.length == 0) {
			return;
		}
		for (IModuleResourceDelta delta : deltas) {
			// Only handle file resources
			IModuleResource resource = delta.getModuleResource();
			if (resource instanceof IModuleFile && delta.getKind() != IModuleResourceDelta.NO_CHANGE) {
				changed.add(new ModuleResourceDeltaWrapper(delta));
			}

			findNonChangedResources(delta.getAffectedChildren(), changed);
		}
	}

	public void disconnect(IProgressMonitor monitor) throws CoreException {
		CloudFoundryPlugin.getCallback().disconnecting(getCloudFoundryServer());

		Server server = (Server) getServer();
		server.setServerState(IServer.STATE_STOPPING);

		setRefreshInterval(-1);

		CloudFoundryServer cloudServer = getCloudFoundryServer();

		Set<ApplicationModule> deletedModules = new HashSet<ApplicationModule>(cloudServer.getApplications());
		cloudServer.clearApplications();

		// update state for cloud applications
		server.setExternalModules(new IModule[0]);
		for (ApplicationModule module : deletedModules) {
			server.setModuleState(new IModule[] { module.getLocalModule() }, IServer.STATE_UNKNOWN);
		}

		server.setServerState(IServer.STATE_STOPPED);
		server.setServerPublishState(IServer.PUBLISH_STATE_NONE);
		closeCaldecottTunnels(monitor);
	}

	@Override
	public void dispose() {
		super.dispose();
		getServer().removeServerListener(serverListener);
		closeCaldecottTunnelsAsynch();
	}

	/**
	 * This method is API used by CloudFoundry Code.
	 */
	public CloudFoundryServer getCloudFoundryServer() throws CoreException {
		Server server = (Server) getServer();

		CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		if (cloudFoundryServer == null) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, "Fail to load server"));
		}
		return cloudFoundryServer;
	}

	public CloudApplication getApplication(final String applicationId, IProgressMonitor monitor) throws CoreException {
		return new Request<CloudApplication>() {
			@Override
			protected CloudApplication doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				return client.getApplication(applicationId);
			}
		}.run(monitor);
	}

	public List<CloudApplication> getApplications(IProgressMonitor monitor) throws CoreException {
		return new Request<List<CloudApplication>>("Getting applications") {
			@Override
			protected List<CloudApplication> doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				return client.getApplications();
			}
		}.run(monitor);
	}

	public ApplicationStats getApplicationStats(final String applicationId, IProgressMonitor monitor)
			throws CoreException {
		return new Request<ApplicationStats>(NLS.bind("Getting application statistics for {0}", applicationId)) {
			@Override
			protected ApplicationStats doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				return client.getApplicationStats(applicationId);
			}
		}.run(monitor);
	}

	public InstancesInfo getInstancesInfo(final String applicationId, IProgressMonitor monitor) throws CoreException {
		return new Request<InstancesInfo>(NLS.bind("Getting application statistics for {0}", applicationId)) {
			@Override
			protected InstancesInfo doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				return client.getApplicationInstances(applicationId);
			}
		}.run(monitor);
	}

	public String getFile(final String applicationId, final int instanceIndex, final String path,
			IProgressMonitor monitor) throws CoreException {
		return new Request<String>("Retrieving file") {
			@Override
			protected String doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, path);
			}
		}.run(monitor);
	}

	public int[] getApplicationMemoryChoices() {
		if (client != null) {
			return client.getApplicationMemoryChoices();
		}
		return new int[0];
	}

	public DeploymentConfiguration getDeploymentConfiguration(final String framework, IProgressMonitor monitor)
			throws CoreException {
		return new Request<DeploymentConfiguration>("Getting available service options") {
			@Override
			protected DeploymentConfiguration doRun(CloudFoundryClient client, SubMonitor progress)
					throws CoreException {
				DeploymentConfiguration configuration = new DeploymentConfiguration();
				// XXX make bogus call that triggers login if needed to work
				// around NPE in client.getApplicationMemoryChoices()
				client.getServices();
				configuration.setMemoryOptions(getApplicationMemoryChoices());
				configuration.setDefaultMemory(client.getDefaultApplicationMemory(framework));
				return configuration;
			}
		}.run(monitor);
	}

	public List<ServiceConfiguration> getServiceConfigurations(IProgressMonitor monitor) throws CoreException {
		return new Request<List<ServiceConfiguration>>("Getting available service options") {
			@Override
			protected List<ServiceConfiguration> doRun(CloudFoundryClient client, SubMonitor progress)
					throws CoreException {
				return client.getServiceConfigurations();
			}
		}.run(monitor);
	}

	/**
	 * For testing only.
	 */
	public void deleteAllApplications(IProgressMonitor monitor) throws CoreException {
		new Request<Object>("Deleting all applications") {
			@Override
			protected Object doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.deleteAllApplications();
				return null;
			}
		}.run(monitor);
	}

	public List<CloudService> getServices(IProgressMonitor monitor) throws CoreException {
		return new Request<List<CloudService>>("Getting available services") {
			@Override
			protected List<CloudService> doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				return client.getServices();
			}
		}.run(monitor);
	}

	public void refreshModules(IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();

		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				doRefreshModules(cloudServer, client, progress);
				return null;
			}
		}.run(monitor);

		CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);

		setRefreshInterval(DEFAULT_INTERVAL);
	}

	/**
	 * This method is API used by CloudFoundry Code.
	 */
	public void deployApplication(final String appName, final int memory, final File warFile, final List<String> uris,
			final List<String> serviceNames, final IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();

		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				try {
					getClient().createApplication(appName, CloudApplication.SPRING, memory, uris, serviceNames);
					getClient().uploadApplication(appName, warFile);
					getClient().startApplication(appName);
					// getClient().createAndUploadAndStartApplication(appName,
					// CloudApplication.SPRING, memory, warFile, uris,
					// serviceNames);
				}
				catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
							"Failed to deploy application from {0}", warFile), e));
				}
				doRefreshModules(cloudServer, client, progress);
				return null;
			}
		}.run(monitor);

		CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);

		setRefreshInterval(DEFAULT_INTERVAL);
	}

	public void resetClient() {
		client = null;
	}

	protected DeploymentDescriptor getDeploymentDescriptor(IModule[] modules, IProgressMonitor monitor)
			throws CoreException {

		IModule module = modules[0];

		CloudFoundryServer cloudServer = getCloudFoundryServer();
		ApplicationModule cloudModule = cloudServer.getApplication(module);
		// prompt user for missing details

		DeploymentDescriptor descriptor = CloudFoundryPlugin.getCallback().prepareForDeployment(cloudServer,
				cloudModule, monitor);

		return descriptor;
	}

	/**
	 * Stops the application and does a full publish of the application before
	 * restarting it. Should not be called for incremental updates.
	 * @param modules
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public ApplicationModule debugModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		return doDebugModule(modules, true, monitor);
	}

	/**
	 * Deploys or starts an app in debug mode and either a full publish or
	 * incremental publish may be specified. Automatic detection of changes in
	 * an app project are detected, and if incremental publish is specified, an
	 * optimised incremental publish is used that only typically would involve
	 * one payload to the server containing just the changes.
	 * @param modules
	 * @param fullPublish
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	protected ApplicationModule doDebugModule(IModule[] modules, boolean incrementalPublish, IProgressMonitor monitor)
			throws CoreException {
		// This flag allows modules to be updated after the app is deployed in
		// debug mode
		// This is necessary to make sure the app module is updated with the
		// latest cloud application
		// in particular if a mode change has occurred (i.e. starting an
		// application in debug
		// mode that was previously running in regular mode). CF feature relies
		// on
		// cached information from the cloud application to be accurate to check
		// whether certain functionality is enabled, etc..
		boolean waitForDeployment = true;

		boolean debug = true;

		return doDeployOrStartModule(modules, waitForDeployment, incrementalPublish, monitor, debug);
	}

	/**
	 * Deploys or starts a module by first doing a full publish of the app.
	 * @param modules
	 * @param waitForDeployment
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public ApplicationModule deployOrStartModule(final IModule[] modules, boolean waitForDeployment,
			IProgressMonitor monitor) throws CoreException {
		return doDeployOrStartModule(modules, waitForDeployment, false, monitor, false);
	}

	/**
	 * Deploys or starts a module by doing either a full publish or incremental.
	 * @param modules
	 * @param waitForDeployment
	 * @param isIncremental
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	protected ApplicationModule doDeployOrStartModule(final IModule[] modules, boolean waitForDeployment,
			boolean isIncremental, IProgressMonitor monitor, boolean debug) throws CoreException {

		ApplicationModule appModule = null;

		DeploymentDescriptor descriptor = getDeploymentDescriptor(modules, monitor);

		descriptor.isIncrementalPublish = isIncremental;

		if (debug) {
			// Set the mode to Debug
			descriptor.deploymentMode = ApplicationAction.DEBUG;
		}
		appModule = new StartOrDeployAction(waitForDeployment, modules, descriptor).deployModule(monitor);

		return appModule;
	}

	@Override
	public void startModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		deployOrStartModule(modules, false, monitor);
	}

	@Override
	public void stop(boolean force) {
		setServerState(IServer.STATE_STOPPED);
		closeCaldecottTunnelsAsynch();
	}

	protected void closeCaldecottTunnelsAsynch() {
		String jobName = "Stopping all tunnels";

		try {
			jobName += ": " + getCloudFoundryServer().getDeploymentName();
		}
		catch (CoreException e1) {
			CloudFoundryPlugin.logError(e1);
		}

		Job job = new Job(jobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				closeCaldecottTunnels(monitor);
				return Status.OK_STATUS;
			}

		};
		job.setSystem(false);
		job.schedule();
	}

	protected void closeCaldecottTunnels(IProgressMonitor monitor) {
		// Close all open Caldecott Tunnels
		try {
			new CaldecottTunnelHandler(getCloudFoundryServer()).stopAndDeleteAllTunnels(monitor);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	@Override
	public void stopModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		Server server = (Server) getServer();
		boolean succeeded = false;
		try {
			server.setModuleState(modules, IServer.STATE_STOPPING);

			CloudFoundryServer cloudServer = getCloudFoundryServer();
			final ApplicationModule cloudModule = cloudServer.getApplication(modules[0]);

			CloudFoundryPlugin.getCallback().applicationStopping(getCloudFoundryServer(), cloudModule);
			new Request<Void>() {
				@Override
				protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
					client.stopApplication(cloudModule.getApplicationId());
					return null;
				}
			}.run(monitor);

			server.setModuleState(modules, IServer.STATE_STOPPED);
			succeeded = true;

			// If succeeded, stop all Caldecott tunnels if the app is the
			// Caldecott app
			if (CaldecottTunnelHandler.isCaldecottApp(cloudModule.getApplicationId())) {
				CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
				handler.stopAndDeleteAllTunnels(monitor);
			}
		}
		finally {
			if (!succeeded) {
				server.setModuleState(modules, IServer.STATE_UNKNOWN);
			}
		}
	}

	/**
	 * Updates and restarts an application in debug mode. Incremental publish
	 * will occur on update restarts if any changes are detected.
	 * @param modules
	 * @param monitor
	 * @param isIncrementalPublishing true if optimised incremental publishing
	 * should be enabled. False otherwise
	 * @return
	 * @throws CoreException
	 */
	public ApplicationModule updateRestartDebugModule(IModule[] modules, boolean isIncrementalPublishing,
			IProgressMonitor monitor) throws CoreException {
		stopModule(modules, monitor);
		return doDebugModule(modules, isIncrementalPublishing, monitor);
	}

	@Override
	/**
	 * Note that this automatically restarts a module in the start mode it is currently, or was currently running in.
	 * It automatically detects if an application is running in debug mode or regular run mode, and restarts it in that
	 * same mode. Other API exists to restart an application in a specific mode, if automatic detection and restart in
	 * existing mode is not required.
	 */
	public void restartModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {

		if (CloudFoundryProperties.isApplicationRunningInDebugMode.testProperty(modules, getCloudFoundryServer())) {
			restartDebugModule(modules, monitor);
		}
		else {
			restartModuleRunMode(modules, monitor);
		}

	}

	/**
	 * This will restart an application in debug mode only.
	 * @param modules
	 * @param monitor
	 * @throws CoreException
	 */
	public void restartDebugModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		// For restarts, check the deployment mode to ensure the application is
		// restarted in the correct mode
		DeploymentDescriptor descriptor = getDeploymentDescriptor(modules, monitor);
		// Indicate that the application should be launched in DebugMode
		descriptor.deploymentMode = ApplicationAction.DEBUG;

		// stop the application first as it may be connected to a debugger
		// and port/IP need to be released
		stopModule(modules, monitor);

		new RestartAction(modules, descriptor).deployModule(monitor);
	}

	/**
	 * Update restart republishes redeploys the application with changes. This
	 * is not the same as restarting an application which simply restarts the
	 * application in its current server version without receiving any local
	 * changes. It will only update restart an application in regular run mode.
	 * It does not support debug mode.Publishing of changes is done
	 * incrementally.
	 * @param modules
	 * @param monitor
	 * @param isIncrementalPublishing true if optimised incremental publishing
	 * should be enabled. False otherwise
	 * @throws CoreException
	 */
	public void updateRestartModuleRunMode(IModule[] modules, boolean isIncrementalPublishing, IProgressMonitor monitor)
			throws CoreException {
		doDeployOrStartModule(modules, false, isIncrementalPublishing, monitor, false);
	}

	/**
	 * This will restart an application in run mode. It does not restart an
	 * application in debug mode
	 * @param modules
	 * @param monitor
	 * @throws CoreException
	 */
	public void restartModuleRunMode(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		DeploymentDescriptor descriptor = getDeploymentDescriptor(modules, monitor);
		new RestartAction(modules, descriptor).deployModule(monitor);
	}

	public void updateApplicationInstances(ApplicationModule module, final int instanceCount, IProgressMonitor monitor)
			throws CoreException {
		final String appName = module.getApplication().getName();
		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.updateApplicationInstances(appName, instanceCount);
				return null;
			}
		}.run(monitor);

		CloudFoundryPlugin.getDefault().fireInstancesUpdated(getCloudFoundryServer());
	}

	public void updatePassword(final String newPassword, IProgressMonitor monitor) throws CoreException {
		new Request<Void>() {

			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.updatePassword(newPassword);
				return null;
			}

		}.run(monitor);
	}

	public void updateApplicationMemory(ApplicationModule module, final int memory, IProgressMonitor monitor)
			throws CoreException {
		final String appName = module.getApplication().getName();
		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.updateApplicationMemory(appName, memory);
				return null;
			}
		}.run(monitor);
	}

	public void updateApplicationUrls(final String appName, final List<String> uris, IProgressMonitor monitor)
			throws CoreException {
		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.updateApplicationUris(appName, uris);
				return null;
			}
		}.run(monitor);
	}

	public List<String> findCaldecottTunnelsToClose(CloudFoundryClient client, String appName,
			List<String> servicesToUpdate) {
		List<String> services = new ArrayList<String>();

		CloudApplication caldecottApp = client.getApplication(appName);
		if (caldecottApp != null) {
			List<String> existingServices = caldecottApp.getServices();
			if (existingServices != null) {
				Set<String> possibleDeletedServices = new HashSet<String>(existingServices);
				for (String updatedService : servicesToUpdate) {
					if (possibleDeletedServices.contains(updatedService)) {
						possibleDeletedServices.remove(updatedService);
					}
				}
				services.addAll(possibleDeletedServices);
			}
		}
		return services;
	}

	public void updateServices(String appName, List<String> services, IProgressMonitor monitor) throws CoreException {
		updateServices(appName, services, false, monitor);
	}

	public void updateServicesAndCloseCaldecottTunnels(String appName, List<String> services, IProgressMonitor monitor)
			throws CoreException {
		updateServices(appName, services, true, monitor);

	}

	protected void updateServices(final String appName, final List<String> services,
			final boolean closeRelatedCaldecottTunnels, IProgressMonitor monitor) throws CoreException {
		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				// Prior to updating the services, obtain the current list of
				// bound services for the app
				// and determine if any services are being unbound. If unbound,
				// check if it is the Caldecott app
				// and accordingly, close related tunnels.
				if (closeRelatedCaldecottTunnels && CaldecottTunnelHandler.isCaldecottApp(appName)) {

					List<String> caldecottServicesToClose = findCaldecottTunnelsToClose(client, appName, services);
					// Close tunnels before the services are removed
					if (caldecottServicesToClose != null) {
						CaldecottTunnelHandler handler = new CaldecottTunnelHandler(getCloudFoundryServer());

						for (String serviceName : caldecottServicesToClose) {
							handler.stopAndDeleteCaldecottTunnel(serviceName, progress);
						}
					}
				}

				client.updateApplicationServices(appName, services);

				return null;
			}
		}.run(monitor);
	}

	public void register(final String email, final String password, IProgressMonitor monitor) throws CoreException {
		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				client.register(email, password);
				return null;
			}
		}.run(monitor);
	}

	/**
	 * Public for testing only.
	 */
	public synchronized CloudFoundryClient getClient() throws CoreException {
		if (client == null) {
			String userName = getCloudFoundryServer().getUsername();
			String password = getCloudFoundryServer().getPassword();
			client = createClient(getCloudFoundryServer().getUrl(), userName, password);

		}
		return client;
	}

	private boolean isApplicationReady(CloudApplication application) {
		/*
		 * RestTemplate restTemplate = new RestTemplate(); String response =
		 * restTemplate.getForObject(application.getUris().get(0),
		 * String.class); if
		 * (response.contains("B29 ROUTER: 404 - FILE NOT FOUND")) { return
		 * false; }
		 */
		return true;
	}

	private void setRefreshInterval(long interval) {
		if (refreshJob == null) {
			try {
				refreshJob = new RefreshJob(getCloudFoundryServer());
			}
			catch (CoreException e) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
								"Failed to start automatic refresh: {0}", e.getMessage()), e));
				return;
			}
		}
		refreshJob.setInterval(interval);
		refreshJob.reschedule();
	}

	private boolean waitForStart(CloudFoundryClient client, String deploymentId, IProgressMonitor monitor)
			throws InterruptedException {
		long timeLeft = DEPLOYMENT_TIMEOUT;
		while (timeLeft > 0) {
			CloudApplication deploymentDetails = client.getApplication(deploymentId);
			if (AppState.STARTED.equals(deploymentDetails.getState()) && isApplicationReady(deploymentDetails)) {
				return true;
			}
			Thread.sleep(SHORT_INTERVAL);
			timeLeft -= SHORT_INTERVAL;
		}
		return false;
	}

	private CloudApplication waitForUpload(CloudFoundryClient client, String applicationId, IProgressMonitor monitor)
			throws InterruptedException {
		long timeLeft = UPLOAD_TIMEOUT;
		while (timeLeft > 0) {
			CloudApplication application = client.getApplication(applicationId);
			if (applicationId.equals(application.getName())) {
				return application;
			}
			Thread.sleep(SHORT_INTERVAL);
			timeLeft -= SHORT_INTERVAL;
		}
		return null;
	}

	protected void doRefreshModules(final CloudFoundryServer cloudServer, CloudFoundryClient client,
			IProgressMonitor progress) throws CoreException {
		// update applications and deployments from server
		Map<String, CloudApplication> applicationByName = new LinkedHashMap<String, CloudApplication>();

		List<CloudApplication> applications = client.getApplications();
		for (CloudApplication application : applications) {
			applicationByName.put(application.getName(), application);
		}

		cloudServer.updateModules(applicationByName);
	}

	@Override
	protected void initialize(IProgressMonitor monitor) {
		super.initialize(monitor);
		getServer().addServerListener(serverListener, ServerEvent.SERVER_CHANGE);
	}

	@Override
	public IStatus publish(int kind, IProgressMonitor monitor) {
		try {
			if (kind == IServer.PUBLISH_CLEAN) {
				List<IModule[]> allModules = getAllModules();
				for (IModule[] module : allModules) {
					if (!module[0].isExternal()) {
						deployOrStartModule(module, false, monitor);
					}
				}
				return Status.OK_STATUS;
			}
			else if (kind == IServer.PUBLISH_INCREMENTAL) {
				List<IModule[]> allModules = getAllModules();
				for (IModule[] module : allModules) {
					CloudApplication app = getCloudFoundryServer().getApplication(module[0]).getApplication();
					if (app != null) {
						int publishState = getServer().getModulePublishState(module);
						if (publishState != IServer.PUBLISH_STATE_NONE) {
							deployOrStartModule(module, false, monitor);
						}
					}
				}
				((Server) getServer()).setServerPublishState(IServer.PUBLISH_STATE_NONE);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.getDefault().getLog()
					.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, "Fail to publish to server", e));
			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
		// return super.publish(kind, monitor);
	}

	@Override
	protected void publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		super.publishModule(kind, deltaKind, module, monitor);

		// If the delta indicates that the module has been removed, remove it
		// from the server.
		// Note that although the "module" parameter is of IModule[] type,
		// documentation
		// (and the name of the parameter) indicates that it is always one
		// module
		if (deltaKind == REMOVED) {
			final CloudFoundryServer cloudServer = getCloudFoundryServer();
			final ApplicationModule cloudModule = cloudServer.getApplication(module[0]);
			if (cloudModule.getApplication() != null) {
				new Request<Void>() {
					@Override
					protected Void doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
						client.deleteApplication(cloudModule.getName());
						return null;
					}
				}.run(monitor);
			}
			// } else if (deltaKind == ADDED | deltaKind == CHANGED) {
			// IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
			// if (delta.length > 0 &&
			// getCloudFoundryServer().getApplication(module[0]).getApplication()
			// != null) {
			// deployOrStartModule(module, false, monitor);
			// }
		}
	}

	/**
	 * Determines if a server supports debug mode. Typically this would be a
	 * cached value for performance reasons, and will not reflect changes
	 */
	public synchronized boolean isServerDebugModeAllowed() {
		return isDebugModeSupported == DebugSupportCheck.SUPPORTED;
	}

	/**
	 * Obtains the debug mode type of the given module. Note that the module
	 * need not be started. It could be stopped, and still have a debug mode
	 * associated with it.
	 * @param module
	 * @param monitor
	 * @return
	 */
	public DebugModeType getDebugModeType(IModule module, IProgressMonitor monitor) {
		try {
			CloudFoundryServer cloudServer = getCloudFoundryServer();
			ApplicationModule cloudModule = cloudServer.getApplication(module);

			// Check if a cloud application exists (i.e., it is deployed) before
			// determining if it is deployed in debug mode
			CloudApplication cloudApplication = cloudModule.getApplication();

			if (cloudApplication != null) {
				return DebugModeType.getDebugModeType(cloudApplication.getDebug());
			}

		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	/**
	 * Invokes the appropriate Java client API for either restarting an
	 * application in regular mode or debug mode. If debug mode is not
	 * specified, then by default it will always restart in regular mode
	 * @param applicationId
	 * @param client
	 * @param restartOrDebugAction either debug mode or regular start/restart
	 */
	protected void restartOrDebugApplicationInClient(String applicationId, CloudFoundryClient client,
			ApplicationAction restartOrDebugAction) {
		switch (restartOrDebugAction) {
		case DEBUG:
			// Only launch in Suspend mode
			client.debugApplication(applicationId, DebugModeType.SUSPEND.getDebugMode());
			break;
		default:
			client.restartApplication(applicationId);
			break;
		}
	}

	public static void validate(String location, String userName, String password, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN);
		try {
			CloudFoundryClient client = createClient(location, userName, password);
			client.login();
		}
		catch (RestClientException e) {
			throw toCoreException(e);
		}
		catch (RuntimeException e) {
			// try to guard against IOException in parsing response
			if (e.getCause() instanceof IOException) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Parse error from server response", e.getCause()));
				throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
						"Unable to communicate with server"));
			}
			else {
				throw e;
			}
		}
		finally {
			progress.done();
		}
	}

	public static void register(String location, String userName, String password, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN);
		try {
			CloudFoundryClient client = createClient(location, userName, password);
			client.register(userName, password);
		}
		catch (RestClientException e) {
			throw toCoreException(e);
		}
		catch (RuntimeException e) {
			// try to guard against IOException in parsing response
			if (e.getCause() instanceof IOException) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Parse error from server response", e.getCause()));
				throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
						"Unable to communicate with server"));
			}
			else {
				throw e;
			}
		}
		finally {
			progress.done();
		}
	}

	private static CloudFoundryClient createClient(String location, String userName, String password)
			throws CoreException {
		URL url;
		try {
			url = new URL(location);
			int port = url.getPort();
			if (port == -1) {
				port = url.getDefaultPort();
			}
			if (password == null) {
				// lost the password, start with an empty one to avoid assertion
				// error
				password = "";
			}
			return CloudFoundryPlugin.getDefault().getCloudFoundryClient(userName, password, url);
		}
		catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
					"The server url ''{0}'' is invalid: {1}", location, e.getMessage()), e));
		}
	}

	static CoreException toCoreException(Exception e) {
		if (e instanceof CloudFoundryException) {
			if (((CloudFoundryException) e).getDescription() != null) {
				return new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind("{0} ({1})",
						((CloudFoundryException) e).getDescription(), e.getMessage()), e));
			}
		}
		return new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
				"Communication with server failed: {0}", e.getMessage()), e));
	}

	// public static class RequestFactory extends
	// CommonsClientHttpRequestFactory {
	//
	// private HttpClient client;
	//
	// /**
	// * For testing.
	// */
	// public static boolean proxyEnabled = true;
	//
	// public RequestFactory(HttpClient client) {
	// super(client);
	// this.client = client;
	// }
	//
	// public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
	// throws IOException {
	// IProxyData[] proxy =
	// CloudFoundryPlugin.getDefault().getProxyService().select(uri);
	// if (proxyEnabled && proxy != null && proxy.length > 0) {
	// client.getHostConfiguration().setProxy(proxy[0].getHost(),
	// proxy[0].getPort());
	// }else {
	// client.getHostConfiguration().setProxyHost(null);
	// }
	// return super.createRequest(uri, httpMethod);
	// }
	//
	// }

	abstract class Request<T> {

		private final String label;

		public Request() {
			this("");
		}

		public Request(String label) {
			Assert.isNotNull(label);
			this.label = label;
		}

		public T run(IProgressMonitor monitor) throws CoreException {
			CloudFoundryServer cloudServer = getCloudFoundryServer();

			if (cloudServer.getUsername() == null || cloudServer.getUsername().length() == 0
					|| cloudServer.getPassword() == null || cloudServer.getPassword().length() == 0) {
				CloudFoundryPlugin.getCallback().getCredentials(cloudServer);
			}

			Server server = (Server) getServer();
			if (server.getServerState() == IServer.STATE_STOPPED || server.getServerState() == IServer.STATE_STOPPING) {
				server.setServerState(IServer.STATE_STARTING);
			}

			SubMonitor progress = SubMonitor.convert(monitor, label, 100);

			T result;
			boolean succeeded = false;
			try {
				CloudFoundryClient client = getClient();
				try {
					result = doRun(client, progress);
					succeeded = true;
				}
				catch (CloudFoundryException e) {
					// try again in case of a login failure
					if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
						client.login();
						result = doRun(client, progress);
						succeeded = true;
					}
					else {
						throw e;
					}
				}

				// Since request succeeded, at this stage determine
				// if the server supports debugging.
				requestAllowDebug(client);

			}
			catch (RestClientException e) {
				throw toCoreException(e);
			}
			finally {
				if (!succeeded) {
					if (server.getServerState() == IServer.STATE_STARTING) {
						server.setServerState(IServer.STATE_STOPPED);
					}
				}
				progress.done();
			}

			if (server.getServerState() != IServer.STATE_STARTED) {
				server.setServerState(IServer.STATE_STARTED);
			}
			// server.setServerPublishState(IServer.PUBLISH_STATE_NONE);

			return result;
		}

		protected abstract T doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException;

	}

	/**
	 * Deploys an application and or starts it in regular or debug mode. If
	 * deployed in debug mode, an attempt will be made to connect the deployed
	 * application to a debugger
	 * 
	 */
	protected abstract class DeployAction {

		final protected IModule[] modules;

		private DeploymentDescriptor descriptor;

		protected DeployAction(IModule[] modules, DeploymentDescriptor descriptor) {
			this.modules = modules;
			this.descriptor = descriptor;
		}

		public ApplicationModule deployModule(IProgressMonitor monitor) throws CoreException {

			try {
				CloudFoundryServer cloudServer = getCloudFoundryServer();

				ApplicationModule deployedModule = performDeployment(monitor, descriptor);

				if (descriptor.deploymentMode == ApplicationAction.DEBUG) {
					new DebugCommandBuilder(modules, cloudServer).getDebugCommand(
							ApplicationAction.CONNECT_TO_DEBUGGER, null).run(monitor);
				}

				// In addition, starting, restarting, and update-restarting a
				// caldecott app should always
				// disconnect existing tunnels.
				if (CaldecottTunnelHandler.isCaldecottApp(deployedModule.getApplicationId())) {
					new CaldecottTunnelHandler(cloudServer).stopAndDeleteAllTunnels(monitor);
				}
				return deployedModule;
			}
			catch (OperationCanceledException e) {
				// ignore so webtools does not show an exception
				((Server) getServer()).setModuleState(modules, IServer.STATE_UNKNOWN);
			}
			return null;
		}

		protected abstract ApplicationModule performDeployment(IProgressMonitor monitor, DeploymentDescriptor descriptor)
				throws CoreException;
	}

	protected boolean hasChildModules(IModule[] modules) {
		IWebModule webModule = CloudUtil.getWebModule(modules);
		return webModule != null && webModule.getModules() != null && webModule.getModules().length > 0;
	}

	protected class StartOrDeployAction extends DeployAction {

		final protected boolean waitForDeployment;

		public StartOrDeployAction(boolean waitForDeployment, IModule[] modules, DeploymentDescriptor descriptor) {
			super(modules, descriptor);
			this.waitForDeployment = waitForDeployment;
		}

		protected ApplicationModule performDeployment(IProgressMonitor monitor, final DeploymentDescriptor descriptor)
				throws CoreException {
			final Server server = (Server) getServer();
			final CloudFoundryServer cloudServer = getCloudFoundryServer();
			final IModule module = modules[0];
			final ApplicationModule cloudModule = cloudServer.getApplication(module);

			try {
				cloudModule.setErrorStatus(null);

				// update mapping
				cloudModule.setApplicationId(descriptor.applicationInfo.getAppName());

				// Update the Staging in the Application module
				cloudModule.setStaging(descriptor.staging);

				server.setModuleState(modules, IServer.STATE_STARTING);
				setRefreshInterval(SHORT_INTERVAL);

				boolean started = new Request<Boolean>() {
					@Override
					protected Boolean doRun(final CloudFoundryClient client, SubMonitor progress) throws CoreException {
						if (descriptor.applicationInfo == null) {
							throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
									"Unable to deploy module"));
						}

						boolean started = false;
						final String applicationId = descriptor.applicationInfo.getAppName();

						if (modules[0].isExternal()) {
							restartOrDebugApplicationInClient(applicationId, client, descriptor.deploymentMode);
							CloudFoundryPlugin.trace("Application " + applicationId + " restarted");
							started = true;
						}
						else {
							if (StandaloneUtil.isStandaloneApp(cloudModule)) {

								// Get the module resources for the standalone
								// as provided by the standlaone module factory
								IModuleResource[] resources = getResources(modules);
								if (resources == null || resources.length == 0) {
									throw new CoreException(
											CloudFoundryPlugin
													.getErrorStatus("Unable to deploy standalone Java module. No deployable resources found in target or output folders."));
								}
								else {
									descriptor.applicationArchive = new StandaloneApplicationArchive(modules[0],
											Arrays.asList(resources));
								}

							}
							else if (descriptor.isIncrementalPublish && !hasChildModules(modules)) {
								// Determine if an incremental publish should
								// occur
								// For the time being support incremental
								// publish
								// only if the app does not have child modules
								// To compute incremental deltas locally,
								// modules must be provided
								// Computes deltas locally before publishing to
								// the server.
								// Potentially more efficient. Should be used
								// only on incremental
								// builds

								handleIncrementalPublish(descriptor, modules);
							}
							else {
								// Create a full war archive
								File warFile = CloudUtil.createWarFile(modules, server, progress);
								if (!warFile.exists()) {
									throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
											"Unable to create war file"));
								}

								CloudFoundryPlugin.trace("War file " + warFile.getName() + " created");
								descriptor.applicationInfo.setWarFile(warFile);

							}

							// Tell webtools the module has been published
							setModulePublishState(modules, IServer.PUBLISH_STATE_NONE);

							// update server publish status
							IModule[] serverModules = server.getModules();
							boolean allSynched = true;
							for (IModule serverModule : serverModules) {
								int modulePublishState = server.getModulePublishState(new IModule[] { serverModule });
								if (modulePublishState == IServer.PUBLISH_STATE_INCREMENTAL
										|| modulePublishState == IServer.PUBLISH_STATE_FULL) {
									allSynched = false;

								}
							}
							if (allSynched) {
								server.setServerPublishState(IServer.PUBLISH_STATE_NONE);
							}

							doDeployApplication(client, cloudModule, descriptor, progress);
							CloudFoundryPlugin.trace("Application " + applicationId + " deployed");

							cloudServer.tagAsDeployed(module);

							// start application in either regular or debug mode
							if (descriptor.deploymentMode != null) {
								CloudFoundryPlugin.trace("Application " + applicationId + " starting");
								restartOrDebugApplicationInClient(applicationId, client, descriptor.deploymentMode);
								started = true;
							}
							else {
								server.setModuleState(modules, IServer.STATE_STOPPED);
							}
						}

						if (started) {
							refreshAfterDeployment(started, client, cloudModule, cloudServer, applicationId, progress);
						}

						return started;
					}

				}.run(monitor);

				if (started) {
					server.setModuleState(modules, IServer.STATE_STARTED);
				}

				setRefreshInterval(DEFAULT_INTERVAL);
				CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);

				if (started && cloudModule != null && cloudModule.getApplication() != null) {
					CloudFoundryPlugin.getCallback().applicationStarted(getCloudFoundryServer(), cloudModule);
				}
				return cloudServer.getApplication(modules[0]);

			}
			catch (CoreException e) {
				cloudServer.getApplication(modules[0]).setErrorStatus(e);
				server.setModulePublishState(modules, IServer.PUBLISH_STATE_UNKNOWN);
				throw e;
			}
		}
	}

	protected void handleIncrementalPublish(final DeploymentDescriptor descriptor, IModule[] modules) {
		IModuleResource[] allResources = getResources(modules);
		IModuleResourceDelta[] deltas = getPublishedResourceDelta(modules);
		List<IModuleResource> changedResources = getChangedResources(deltas);
		ApplicationArchive moduleArchive = new ModuleResourceApplicationArchive(Arrays.asList(allResources),
				changedResources, modules[0], descriptor.applicationInfo.getAppName());

		descriptor.applicationArchive = moduleArchive;

	}

	protected void refreshAfterDeployment(boolean waitForDeployment, CloudFoundryClient client,
			ApplicationModule cloudModule, CloudFoundryServer cloudServer, String applicationId,
			IProgressMonitor progress) throws CoreException {
		if (waitForDeployment) {
			try {
				if (!waitForStart(client, cloudModule.getApplicationId(), progress)) {
					throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
							"Deployment of {0} timed out", cloudModule.getName())));
				}
			}
			catch (InterruptedException e) {
				throw new OperationCanceledException();
			}

			CloudFoundryPlugin.trace("Application " + applicationId + " started");
			doRefreshModules(cloudServer, client, progress);
		}
	}

	protected class RestartAction extends DeployAction {

		public RestartAction(IModule[] modules, DeploymentDescriptor descriptor) {
			super(modules, descriptor);
		}

		protected ApplicationModule performDeployment(IProgressMonitor monitor, final DeploymentDescriptor descriptor)
				throws CoreException {
			final Server server = (Server) getServer();
			final CloudFoundryServer cloudServer = getCloudFoundryServer();
			final IModule module = modules[0];
			final ApplicationModule cloudModule = cloudServer.getApplication(module);
			final boolean waitForDeployment = true;
			try {
				cloudModule.setErrorStatus(null);

				// update mapping
				cloudModule.setApplicationId(descriptor.applicationInfo.getAppName());

				server.setModuleState(modules, IServer.STATE_STARTING);
				setRefreshInterval(SHORT_INTERVAL);

				boolean started = new Request<Boolean>() {
					@Override
					protected Boolean doRun(final CloudFoundryClient client, SubMonitor progress) throws CoreException {
						if (descriptor.applicationInfo == null) {
							throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
									"Unable to deploy module"));
						}

						boolean started = false;
						String applicationId = descriptor.applicationInfo.getAppName();

						restartOrDebugApplicationInClient(applicationId, client, descriptor.deploymentMode);
						CloudFoundryPlugin.trace("Application " + applicationId + " restarted");
						started = true;

						refreshAfterDeployment(waitForDeployment, client, cloudModule, cloudServer, applicationId,
								progress);

						return started;
					}
				}.run(monitor);

				if (started) {
					server.setModuleState(modules, IServer.STATE_STARTED);
				}

				setRefreshInterval(DEFAULT_INTERVAL);
				CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);

				if (started && cloudModule != null && cloudModule.getApplication() != null) {
					CloudFoundryPlugin.getCallback().applicationStarted(getCloudFoundryServer(), cloudModule);
				}
				return cloudModule;
			}
			catch (CoreException e) {
				cloudServer.getApplication(modules[0]).setErrorStatus(e);
				server.setModulePublishState(modules, IServer.PUBLISH_STATE_UNKNOWN);
				throw e;
			}
		}

	}

}
