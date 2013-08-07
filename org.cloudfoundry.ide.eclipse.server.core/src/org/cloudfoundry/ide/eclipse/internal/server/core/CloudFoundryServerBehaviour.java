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

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback.DeploymentDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommandBuilder;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugModeType;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpaceServerLookup;
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
import org.springframework.web.client.RestClientException;

/**
 * 
 * Contains many of the calls to the CF Java client. The CF server behaviour
 * should be the main call point for interacting with the actual cloud server,
 * with the exception of Caldecott, which is handled in a similar behaviour.
 * 
 * It's important to note that almost all Java client calls are wrapped around a
 * Request object, and it is important to wrap future client calls around a
 * Request object, as the request object handles automatic client login, server
 * state verification, and proxy handling.
 * 
 * 
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class CloudFoundryServerBehaviour extends ServerBehaviourDelegate {

	private CloudFoundryOperations client;

	private RefreshJob refreshJob;

	/*
	 * FIXNS: Until V2 MCF is released, disable debugging support for V2, as
	 * public clouds also indicate they support debug.
	 */
	private DebugSupportCheck isDebugModeSupported = DebugSupportCheck.UNSUPPORTED;

	private List<CloudDomain> domainFromOrgs;

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
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
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
	protected synchronized void requestAllowDebug(CloudFoundryOperations client) throws CoreException {
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
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				for (CloudService service : services) {
					client.createService(service);
				}

				return null;
			}
		}.run(monitor);
		CloudFoundryPlugin.getDefault().fireServicesUpdated(getCloudFoundryServer());
	}

	public synchronized String getLaunchURL(String applicationName, CloudDomain domain) throws CoreException {

		if (domain == null) {
			CloudFoundryServer cloudServer = getCloudFoundryServer();
			CloudFoundrySpace space = cloudServer.getCloudFoundrySpace();

			if (space == null) {
				throw new CoreException(
						CloudFoundryPlugin
								.getErrorStatus("No organization and space selected. Unable to generate application launch URL based on organization domain for: "
										+ applicationName));
			}

			if (domainFromOrgs == null || domainFromOrgs.isEmpty()) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus("No domains found for: " + space.getOrgName()
						+ " - " + space.getSpaceName()));
			}

			// Retrieve the first domain
			domain = domainFromOrgs.get(0);
		}

		String url = domain.getName();
		url = url.replace("http://", "");

		// For V2 servers, simply append the application name to the domain.
		// Do not remove the prefix, unlike V1.
		url = applicationName + "." + url;
		return url;

		// // Old V1 way of calculating the URL. Kept as a reference only. Not
		// used for V2 (orgs/spaces)
		// if (!supportsSpaces()) {
		// // Still support V1 for older cloud foundry servers
		// Server server = (Server) getCloudFoundryServer().getServerOriginal();
		// applicationName = applicationName.toLowerCase();
		// String url = server.getAttribute(CloudFoundryServer.PROP_URL, "");
		// url = url.replace("http://", "");
		//
		// // Remove the "api" prefix from the URL and replace it with the app
		// // name
		// String prefix = url.split("\\.")[0];
		// return url.replace(prefix, applicationName);
		//
		// }

	}

	public void deleteModules(final IModule[] modules, final boolean deleteServices, IProgressMonitor monitor)
			throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();
		new RequestWithRefreshCallBack<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				for (IModule module : modules) {
					final CloudFoundryApplicationModule appModule = cloudServer.getApplication(module);

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
								// This has to be used instead of addAll(..), as
								// there is a chance the list is non-empty but
								// contains null entries
								for (String serviceName : actualServices) {
									if (serviceName != null) {
										servicesToDelete.add(serviceName);
									}
								}
							}

							// Close any Caldecott tunnels before deleting app
							if (TunnelBehaviour.isCaldecottApp(appModule.getApplicationId())) {
								// Delete all tunnels if the Caldecott app is
								// removed
								new TunnelBehaviour(cloudServer).stopAndDeleteAllTunnels(progress);
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
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				TunnelBehaviour handler = new TunnelBehaviour(getCloudFoundryServer());
				for (String service : services) {
					client.deleteService(service);

					// Also delete any existing Tunnels
					handler.stopAndDeleteCaldecottTunnel(service, progress);
				}
				return null;
			}
		}.run(monitor);
	}

	/**
	 * This performs the primary operation of creating an application and then
	 * pushing the application contents to the server. These are performed in
	 * separate requests via the CF client. If the application does not exist,
	 * it is first created through an initial request. Once the application is
	 * created, or if it already exists, the next step is to upload (push) the
	 * application archive containing the application's resources. This is
	 * performed in a second separate request.
	 * @param client
	 * @param appModule
	 * @param descriptor
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	protected CloudApplication pushApplication(CloudFoundryOperations client,
			final CloudFoundryApplicationModule appModule, final DeploymentDescriptor descriptor,
			IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(descriptor.applicationInfo);

		ApplicationInfo applicationInfo = descriptor.applicationInfo;
		String applicationId = applicationInfo.getAppName();

		appModule.setLastApplicationInfo(applicationInfo);
		appModule.setLastDeploymentInfo(descriptor.deploymentInfo);

		try {
			List<CloudApplication> existingApps = client.getApplications();
			boolean found = false;
			for (CloudApplication existingApp : existingApps) {
				if (existingApp.getName().equals(applicationId)) {
					found = true;
					break;
				}
			}

			// 1. Create the application if it doesn't already exist
			if (!found) {
				Staging staging = descriptor.staging;
				List<String> uris = descriptor.deploymentInfo.getUris() != null ? descriptor.deploymentInfo.getUris()
						: new ArrayList<String>(0);
				List<String> services = descriptor.deploymentInfo.getServices() != null ? descriptor.deploymentInfo
						.getServices() : new ArrayList<String>(0);

				if (staging == null) {
					// Even for v2, a non-null staging is required.
					staging = new Staging();
				}
				client.createApplication(applicationId, staging, descriptor.deploymentInfo.getMemory(), uris, services);
			}
			File warFile = applicationInfo.getWarFile();

			// 2. Now push the application content.
			if (warFile != null) {
				client.uploadApplication(applicationId, warFile);
			}
			else {
				ApplicationArchive archive = descriptor.applicationArchive;
				if (archive instanceof CachingApplicationArchive) {
					final CachingApplicationArchive cachingArchive = (CachingApplicationArchive) archive;
					client.uploadApplication(applicationId, archive, new UploadStatusCallback() {

						public void onProcessMatchedResources(int length) {

						}

						public void onMatchedFileNames(Set<String> matchedFileNames) {
							cachingArchive.generatePartialWarFile(matchedFileNames);
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

		Set<CloudFoundryApplicationModule> deletedModules = new HashSet<CloudFoundryApplicationModule>(
				cloudServer.getApplications());
		cloudServer.clearApplications();

		// update state for cloud applications
		server.setExternalModules(new IModule[0]);
		for (CloudFoundryApplicationModule module : deletedModules) {
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
			protected CloudApplication doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplication(applicationId);
			}
		}.run(monitor);
	}

	public List<CloudApplication> getApplications(IProgressMonitor monitor) throws CoreException {
		return new Request<List<CloudApplication>>("Getting applications") {
			@Override
			protected List<CloudApplication> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				return client.getApplications();
			}
		}.run(monitor);
	}

	public ApplicationStats getApplicationStats(final String applicationId, IProgressMonitor monitor)
			throws CoreException {
		return new StagingAwareRequest<ApplicationStats>(NLS.bind("Getting application statistics for {0}",
				applicationId)) {
			@Override
			protected ApplicationStats doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplicationStats(applicationId);
			}
		}.run(monitor);
	}

	protected boolean appStarted(final String applicationId, CloudFoundryOperations operations, IProgressMonitor monitor)
			throws CoreException {
		CloudApplication cloudApplication = operations.getApplication(applicationId);
		if (cloudApplication != null) {
			AbstractWaitForStateOperation waitForStart = new AbstractWaitForStateOperation(getCloudFoundryServer(),
					"Waiting for application to start", 2, 1000) {

				@Override
				protected void doOperation(CloudFoundryServerBehaviour behaviour, IModule module,
						IProgressMonitor progress) throws CoreException {
					// Do nothing. Wait for application to start
				}

				@Override
				protected boolean isInState(AppState state) {
					return state.equals(CloudApplication.AppState.STARTED);
				}

			};
			return waitForStart.run(monitor, cloudApplication);
		}
		return false;
	}

	public InstancesInfo getInstancesInfo(final String applicationId, IProgressMonitor monitor) throws CoreException {
		return new StagingAwareRequest<InstancesInfo>(NLS.bind("Getting application statistics for {0}", applicationId)) {
			@Override
			protected InstancesInfo doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplicationInstances(applicationId);
			}
		}.run(monitor);
	}

	public String getFile(final String applicationId, final int instanceIndex, final String path,
			IProgressMonitor monitor) throws CoreException {
		return new StagingAwareRequest<String>() {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, path);
			}
		}.run(monitor);
	}

	public String getFile(final String applicationId, final int instanceIndex, final String filePath,
			final int startPosition, IProgressMonitor monitor) throws CoreException {
		return new StagingAwareRequest<String>() {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, filePath, startPosition);
			}
		}.run(monitor);
	}

	public int[] getApplicationMemoryChoices() {
		if (client != null) {
			return client.getApplicationMemoryChoices();
		}
		return new int[0];
	}

	public DeploymentConfiguration getDeploymentConfiguration(IProgressMonitor monitor) throws CoreException {
		return new Request<DeploymentConfiguration>("Getting available service options") {
			@Override
			protected DeploymentConfiguration doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				DeploymentConfiguration configuration = new DeploymentConfiguration(getApplicationMemoryChoices());
				// XXX make bogus call that triggers login if needed to work
				// around NPE in client.getApplicationMemoryChoices()
				client.getServices();
				return configuration;
			}
		}.run(monitor);
	}

	public List<CloudServiceOffering> getServiceOfferings(IProgressMonitor monitor) throws CoreException {
		return new Request<List<CloudServiceOffering>>("Getting available service options") {
			@Override
			protected List<CloudServiceOffering> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				return client.getServiceOfferings();
			}
		}.run(monitor);
	}

	/**
	 * For testing only.
	 */
	public void deleteAllApplications(IProgressMonitor monitor) throws CoreException {
		new Request<Object>("Deleting all applications") {
			@Override
			protected Object doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteAllApplications();
				return null;
			}
		}.run(monitor);
	}

	public List<CloudService> getServices(IProgressMonitor monitor) throws CoreException {
		return new Request<List<CloudService>>("Getting available services") {
			@Override
			protected List<CloudService> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getServices();
			}
		}.run(monitor);
	}

	public void refreshModules(IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();

		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				doRefreshModules(cloudServer, client, progress);
				return null;
			}
		}.run(monitor);

		CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);

		setRefreshInterval(CloudFoundryClientRequest.DEFAULT_INTERVAL);
	}

	/**
	 * This method is API used by CloudFoundry Code.
	 */
	public void deployApplication(final String appName, final int memory, final File warFile, final List<String> uris,
			final List<String> serviceNames, final IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getCloudFoundryServer();

		new Request<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				try {
					getClient(progress).createApplication(appName, new Staging(), memory, uris, serviceNames);
					getClient(progress).uploadApplication(appName, warFile);
					getClient(progress).startApplication(appName);
					// getClient().createAndUploadAndStartApplication(appName,
					// DeploymentConstants.SPRING, memory, warFile, uris,
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

		setRefreshInterval(CloudFoundryClientRequest.DEFAULT_INTERVAL);
	}

	public void resetClient() {
		client = null;
	}

	protected DeploymentDescriptor getDeploymentDescriptor(IModule[] modules, IProgressMonitor monitor)
			throws CoreException {

		IModule module = modules[0];

		CloudFoundryServer cloudServer = getCloudFoundryServer();
		CloudFoundryApplicationModule cloudModule = cloudServer.getApplication(module);
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
	public CloudFoundryApplicationModule debugModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
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
	protected CloudFoundryApplicationModule doDebugModule(IModule[] modules, boolean incrementalPublish,
			IProgressMonitor monitor) throws CoreException {
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
	public CloudFoundryApplicationModule deployOrStartModule(final IModule[] modules, boolean waitForDeployment,
			IProgressMonitor monitor) throws CoreException {
		return doDeployOrStartModule(modules, waitForDeployment, false, monitor, false);
	}

	public CloudFoundryApplicationModule deployOrStartModule(final IModule[] modules, boolean waitForDeployment,
			IProgressMonitor monitor, DeploymentDescriptor descriptor) throws CoreException {
		CloudFoundryApplicationModule appModule = new StartOrDeployAction(waitForDeployment, modules, descriptor)
				.deployModule(monitor);

		return appModule;
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
	protected CloudFoundryApplicationModule doDeployOrStartModule(final IModule[] modules, boolean waitForDeployment,
			boolean isIncremental, IProgressMonitor monitor, boolean debug) throws CoreException {

		CloudFoundryApplicationModule appModule = null;

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
		// This stops the server locally, it does NOT stop the remotely running
		// applications
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
			new TunnelBehaviour(getCloudFoundryServer()).stopAndDeleteAllTunnels(monitor);
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
			final CloudFoundryApplicationModule cloudModule = cloudServer.getApplication(modules[0]);

			CloudFoundryPlugin.getCallback().applicationStopping(getCloudFoundryServer(), cloudModule);
			new Request<Void>() {
				@Override
				protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
					client.stopApplication(cloudModule.getApplicationId());
					return null;
				}
			}.run(monitor);

			server.setModuleState(modules, IServer.STATE_STOPPED);
			succeeded = true;
			CloudFoundryPlugin.getCallback().applicationStopped(cloudModule, cloudServer);

			// If succeeded, stop all Caldecott tunnels if the app is the
			// Caldecott app
			if (TunnelBehaviour.isCaldecottApp(cloudModule.getApplicationId())) {
				TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
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
	public CloudFoundryApplicationModule updateRestartDebugModule(IModule[] modules, boolean isIncrementalPublishing,
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

	public String getStagingLogs(final StartingInfo info, final int offset, IProgressMonitor monitor)
			throws CoreException {
		return new StagingAwareRequest<String>("Reading staging logs") {

			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getStagingLogs(info, offset);
			}

		}.run(monitor);
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

	public void updateApplicationInstances(CloudFoundryApplicationModule module, final int instanceCount,
			IProgressMonitor monitor) throws CoreException {
		final String appName = module.getApplication().getName();
		new StagingAwareRequest<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationInstances(appName, instanceCount);
				return null;
			}
		}.run(monitor);

		CloudFoundryPlugin.getDefault().fireInstancesUpdated(getCloudFoundryServer());
	}

	public void updatePassword(final String newPassword, IProgressMonitor monitor) throws CoreException {
		new Request<Void>() {

			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updatePassword(newPassword);
				return null;
			}

		}.run(monitor);
	}

	public void updateApplicationMemory(CloudFoundryApplicationModule module, final int memory, IProgressMonitor monitor)
			throws CoreException {
		final String appName = module.getApplication().getName();
		new StagingAwareRequest<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationMemory(appName, memory);
				return null;
			}
		}.run(monitor);
	}

	public void updateApplicationUrls(final String appName, final List<String> uris, IProgressMonitor monitor)
			throws CoreException {
		new StagingAwareRequest<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationUris(appName, uris);
				return null;
			}
		}.run(monitor);
	}

	public List<String> findCaldecottTunnelsToClose(CloudFoundryOperations client, String appName,
			List<String> servicesToUpdate) {
		List<String> services = new ArrayList<String>();

		CloudApplication caldecottApp = client.getApplication(appName);
		if (caldecottApp != null) {
			List<String> existingServices = caldecottApp.getServices();
			if (existingServices != null) {
				Set<String> possibleDeletedServices = new HashSet<String>();
				// Must iterate rather than passing to constructor or using
				// addAll, as some
				// of the entries in existing services may be null
				for (String existingService : existingServices) {
					if (existingService != null) {
						possibleDeletedServices.add(existingService);
					}
				}

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
		new StagingAwareRequest<Void>() {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				// Prior to updating the services, obtain the current list of
				// bound services for the app
				// and determine if any services are being unbound. If unbound,
				// check if it is the Caldecott app
				// and accordingly, close related tunnels.
				if (closeRelatedCaldecottTunnels && TunnelBehaviour.isCaldecottApp(appName)) {

					List<String> caldecottServicesToClose = findCaldecottTunnelsToClose(client, appName, services);
					// Close tunnels before the services are removed
					if (caldecottServicesToClose != null) {
						TunnelBehaviour handler = new TunnelBehaviour(getCloudFoundryServer());

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
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.register(email, password);
				return null;
			}
		}.run(monitor);
	}

	/**
	 * Public for testing only. Use alternate public getClient() API for actual
	 * client operations. If credentials are not used, as in the case when only
	 * a URL is present for a server, null must be passed for the credentials.
	 */
	public synchronized CloudFoundryOperations getClient(CloudCredentials credentials, IProgressMonitor monitor)
			throws CoreException {
		if (client == null) {
			CloudFoundrySpace cloudSpace = new CloudSpaceServerLookup(getCloudFoundryServer(), credentials)
					.getCloudSpace(monitor);

			if (credentials != null) {
				client = createClient(getCloudFoundryServer().getUrl(), credentials, cloudSpace);
			}
			else {
				String userName = getCloudFoundryServer().getUsername();
				String password = getCloudFoundryServer().getPassword();
				client = createClient(getCloudFoundryServer().getUrl(), userName, password, cloudSpace);
			}
		}
		return client;
	}

	/**
	 * In most cases, the progress monitor can be null, although if available
	 * 
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public synchronized CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
		return getClient((CloudCredentials) null, monitor);
	}

	private boolean isApplicationReady(CloudApplication application) {
		/*
		 * RestTemplate restTemplate = new RestTemplate(); String response =
		 * restTemplate.getForObject(application.getUris().get(0),
		 * String.class); if
		 * (response.contains("B29 ROUTER: 404 - FILE NOT FOUND")) { return
		 * false; }
		 */
		return AppState.STARTED.equals(application.getState());
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

	private boolean waitForStart(CloudFoundryOperations client, String deploymentId, IProgressMonitor monitor)
			throws InterruptedException {
		long initialInterval = CloudFoundryClientRequest.SHORT_INTERVAL;
		Thread.sleep(initialInterval);
		long timeLeft = CloudFoundryClientRequest.DEPLOYMENT_TIMEOUT - initialInterval;
		while (timeLeft > 0) {
			CloudApplication deploymentDetails = client.getApplication(deploymentId);
			if (isApplicationReady(deploymentDetails)) {
				return true;
			}
			Thread.sleep(CloudFoundryClientRequest.ONE_SECOND_INTERVAL);
			timeLeft -= CloudFoundryClientRequest.ONE_SECOND_INTERVAL;
		}
		return false;
	}

	private CloudApplication waitForUpload(CloudFoundryOperations client, String applicationId, IProgressMonitor monitor)
			throws InterruptedException {
		long timeLeft = CloudFoundryClientRequest.UPLOAD_TIMEOUT;
		while (timeLeft > 0) {
			CloudApplication application = client.getApplication(applicationId);
			if (applicationId.equals(application.getName())) {
				return application;
			}
			Thread.sleep(CloudFoundryClientRequest.SHORT_INTERVAL);
			timeLeft -= CloudFoundryClientRequest.SHORT_INTERVAL;
		}
		return null;
	}

	protected void doRefreshModules(final CloudFoundryServer cloudServer, CloudFoundryOperations client,
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
			final CloudFoundryApplicationModule cloudModule = cloudServer.getApplication(module[0]);
			if (cloudModule.getApplication() != null) {
				new Request<Void>() {
					@Override
					protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
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
			CloudFoundryApplicationModule cloudModule = cloudServer.getApplication(module);

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
	 * Given a WTP module, the corresponding CF application module will have its
	 * app instance stats refreshed. As the application module also has a
	 * reference to the actual cloud application, an updated cloud application
	 * will be retrieved as well.
	 * @param module whos application instances and stats should be refreshed
	 * @param monitor
	 * @throws CoreException
	 */
	public void refreshApplicationInstanceStats(IModule module, IProgressMonitor monitor) throws CoreException {
		if (module != null) {
			CloudFoundryApplicationModule appModule = getCloudFoundryServer().getApplication(module);

			try {
				CloudApplication application = getApplication(appModule.getApplicationId(), monitor);
				appModule.setCloudApplication(application);
			}
			catch (CoreException e) {
				// application is not deployed to server yet
			}

			if (appModule.getApplication() != null) {
				// refresh application stats
				ApplicationStats stats = getApplicationStats(appModule.getApplicationId(), monitor);
				InstancesInfo info = getInstancesInfo(appModule.getApplicationId(), monitor);
				appModule.setApplicationStats(stats);
				appModule.setInstancesInfo(info);
			}
			else {
				appModule.setApplicationStats(null);
			}
		}
	}

	public static void validate(String location, String userName, String password, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN);
		try {
			CloudFoundryOperations client = createClient(location, userName, password);
			CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client, null);
			operationsHandler.login(progress);
		}
		catch (RestClientException e) {
			throw CloudErrorUtil.toCoreException(e);
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
			CloudFoundryOperations client = createClient(location, userName, password);
			client.register(userName, password);
		}
		catch (RestClientException e) {
			throw CloudErrorUtil.toCoreException(e);
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

	/**
	 * Creates a standalone client with no association with a server behaviour.
	 * This is used only for connecting to a Cloud Foundry server for credential
	 * verification verification. To create a client bound to a server
	 * behaviour, it must be done through the server behaviour
	 * @param location
	 * @param userName
	 * @param password
	 * @return
	 * @throws CoreException
	 */
	public static CloudFoundryOperations createClient(String location, String userName, String password)
			throws CoreException {
		return createClient(location, userName, password, null);
	}

	private static CloudFoundryOperations createClient(String location, String userName, String password,
			CloudFoundrySpace cloudSpace) throws CoreException {
		if (password == null) {
			// lost the password, start with an empty one to avoid assertion
			// error
			password = "";
		}
		return createClient(location, new CloudCredentials(userName, password), cloudSpace);
	}

	private static CloudFoundryOperations createClient(String location, CloudCredentials credentials,
			CloudFoundrySpace cloudSpace) throws CoreException {
		URL url;
		try {
			url = new URL(location);
			int port = url.getPort();
			if (port == -1) {
				port = url.getDefaultPort();
			}
			// At this stage, determine if it is a cloud server and account that
			// supports orgs and spaces

			return cloudSpace != null ? CloudFoundryPlugin.getDefault().getCloudFoundryClient(credentials,
					cloudSpace.getSpace(), url) : CloudFoundryPlugin.getDefault().getCloudFoundryClient(credentials,
					url);
		}
		catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
					"The server url ''{0}'' is invalid: {1}", location, e.getMessage()), e));
		}
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

	/**
	 * A request checks server state prior to performing a server operation via
	 * a Cloud Foundry client, and resolves the Cloud Foundry client to be used
	 * for the operation. All request behaviour is performed in a sub monitor,
	 * therefore submonitor operations like creating a new child to track
	 * progress worked should be used. Additional checks prior to calling a Java
	 * client API is to perform a login check, and if the client is not
	 * currently logged in prior to attempting to make the API call, an
	 * automatic login will be performed. Another check performed on the client
	 * is whether proxy settings have changed during the same server behaviour
	 * session.
	 * 
	 * It is very important to wrap client calls in a Request object as it
	 * contains common error handling applicable to many client calls.
	 * 
	 * @param <T>
	 */
	abstract class Request<T> {

		private final String label;

		protected final long requestTimeOut;

		public Request() {
			this("");
		}

		public Request(String label) {
			this(label, CloudFoundryClientRequest.DEFAULT_CF_CLIENT_REQUEST_TIMEOUT);
		}

		public Request(String label, long requestTimeOut) {
			Assert.isNotNull(label);
			this.label = label;
			this.requestTimeOut = requestTimeOut > 0 ? requestTimeOut
					: CloudFoundryClientRequest.DEFAULT_CF_CLIENT_REQUEST_TIMEOUT;
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

			SubMonitor subProgress = SubMonitor.convert(monitor, label, 100);

			T result;
			boolean succeeded = false;
			try {

				CloudFoundryOperations client = getClient(subProgress);

				// Execute the request through a client request handler that
				// handles errors as well as proxy checks, and reattempts the
				// request once
				// if unauthorised/forbidden exception is thrown, and client
				// login is
				// attempted again.
				result = runAsClientRequestCheckConnection(client, cloudServer, subProgress);

				succeeded = true;

				try {
					// At this stage, the client is connected, otherwise the
					// client request would have failed.
					// Now retrieve information that should be done once per
					// connection session,
					// including whether the server supports debug, list of
					// application plans, and domains for the org.
					// Since request succeeded, at this stage determine
					// if the server supports debugging.

					// FIXNS: Disabled for CF 1.5.0 until V2 MCF is released
					// that supports debug.
					// requestAllowDebug(client);

					if (domainFromOrgs == null) {
						domainFromOrgs = client.getDomainsForOrg();
					}
				}
				catch (RestClientException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
			finally {
				if (!succeeded) {
					if (server.getServerState() == IServer.STATE_STARTING) {
						server.setServerState(IServer.STATE_STOPPED);
					}
				}
				subProgress.done();
			}

			if (server.getServerState() != IServer.STATE_STARTED) {
				server.setServerState(IServer.STATE_STARTED);
			}
			// server.setServerPublishState(IServer.PUBLISH_STATE_NONE);

			return result;
		}

		/**
		 * Attempts to execute the client request by first checking proxy
		 * settings, and if unauthorised/forbidden exceptions thrown the first
		 * time, will attempt to log in. If that succeeds, it will attempt one
		 * more time. Otherwise it will fail and not attempt the request any
		 * further.
		 * @param client
		 * @param cloudServer
		 * @param subProgress
		 * @return
		 * @throws CoreException if attempt to execute failed, even after a
		 * second attempt after a client login.
		 */
		protected T runAsClientRequestCheckConnection(CloudFoundryOperations client, CloudFoundryServer cloudServer,
				SubMonitor subProgress) throws CoreException {
			// Check that a user is logged in and proxy is updated
			String cloudURL = cloudServer.getUrl();
			CloudFoundryLoginHandler handler = new CloudFoundryLoginHandler(client, cloudURL);

			// Always check if proxy settings have changed.
			handler.updateProxyInClient(client);

			try {
				return runAsClientRequest(client, subProgress);
			}
			catch (CoreException ce) {
				CloudFoundryException cfe = ce.getCause() instanceof CloudFoundryException ? (CloudFoundryException) ce
						.getCause() : null;
				if (cfe != null && handler.shouldAttemptClientLogin(cfe)) {
					handler.login(subProgress, 3, CloudFoundryClientRequest.LOGIN_INTERVAL);
					return runAsClientRequest(client, subProgress);
				}
				else {
					throw ce;
				}
			}
		}

		protected T runAsClientRequest(CloudFoundryOperations client, SubMonitor subProgress) throws CoreException {
			return new CloudFoundryClientRequest<T>(client, getCloudFoundryServer(), requestTimeOut) {

				@Override
				protected T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
					return Request.this.doRun(client, progress);
				}

			}.run(subProgress);
		}

		protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	}

	/**
	 * 
	 * Request that is aware of potential staging related errors and may attempt
	 * the request again on certain types of staging errors like Staging Not
	 * Finished errors. Note that this should only be used on certain types of
	 * operations performed on a app that is already started, like fetching the
	 * staging logs, or app instances stats, as re-attempts on these operations
	 * due to staging related errors (e.g. staging not finished yet) is
	 * permissable. However, operations not related to particular application
	 * (e.g. creating a service, getting list of all apps), should not use this
	 * request.
	 */
	abstract class StagingAwareRequest<T> extends Request<T> {

		public StagingAwareRequest() {
			super();
		}

		public StagingAwareRequest(String label) {
			super(label, CloudFoundryClientRequest.DEPLOYMENT_TIMEOUT);
		}

		public StagingAwareRequest(String label, long requestTimeOut) {
			super(label, requestTimeOut);
		}

		protected T runAsClientRequest(CloudFoundryOperations client, SubMonitor subProgress) throws CoreException {
			return new StagingAwareClientRequest<T>(client, getCloudFoundryServer(), requestTimeOut) {

				@Override
				protected T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
					return StagingAwareRequest.this.doRun(client, progress);
				}

			}.run(subProgress);
		}

		protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

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

		public CloudFoundryApplicationModule deployModule(IProgressMonitor monitor) throws CoreException {

			try {
				CloudFoundryServer cloudServer = getCloudFoundryServer();

				CloudFoundryApplicationModule deployedModule = performDeployment(monitor, descriptor);

				if (descriptor.deploymentMode == ApplicationAction.DEBUG) {
					new DebugCommandBuilder(modules, cloudServer).getDebugCommand(
							ApplicationAction.CONNECT_TO_DEBUGGER, null).run(monitor);
				}

				// In addition, starting, restarting, and update-restarting a
				// caldecott app should always
				// disconnect existing tunnels.
				if (TunnelBehaviour.isCaldecottApp(deployedModule.getApplicationId())) {
					new TunnelBehaviour(cloudServer).stopAndDeleteAllTunnels(monitor);
				}
				return deployedModule;
			}
			catch (OperationCanceledException e) {
				// ignore so webtools does not show an exception
				((Server) getServer()).setModuleState(modules, IServer.STATE_UNKNOWN);
			}
			return null;
		}

		protected abstract CloudFoundryApplicationModule performDeployment(IProgressMonitor monitor,
				DeploymentDescriptor descriptor) throws CoreException;
	}

	protected boolean hasChildModules(IModule[] modules) {
		IWebModule webModule = CloudUtil.getWebModule(modules);
		return webModule != null && webModule.getModules() != null && webModule.getModules().length > 0;
	}

	/**
	 * This action is the primary operation for pushing an application to a CF
	 * server. <br/>
	 * Several primary steps are performed when deploying an application:
	 * <p/>
	 * 1. Create an archive file containing the application's resources.
	 * Incremental publishing is may be used here to create an archive
	 * containing only those files that have been changed.
	 * <p/>
	 * 2. Check if the application exists in the server. If not, create it.
	 * <p/>
	 * 3. Once the application is verified to exist, push the archive of the
	 * application to the server.
	 * <p/>
	 * 4. Set local WTP module states to indicate the an application's contents
	 * have been pushed (i.e. "published")
	 * <p/>
	 * 5. Start the application, in either debug or run mode, based on the
	 * application's descriptor, in the server.
	 * <p/>
	 * 6. Set local WTP module states to indicate whether an application has
	 * started, or is stopped if an error occurred while starting it.
	 * <p/>
	 * 7. Invoke callbacks to notify listeners that an application has been
	 * started. This usually performs UI refreshes, which may invoke additional
	 * calls by the listener to fetch application instance stats and other
	 * information.
	 * <p/>
	 * Note that ALL client calls need to be wrapped around a Request operation,
	 * as the Request operation performs additional checks prior to invoking the
	 * call, as well as handles errors.
	 */
	protected class StartOrDeployAction extends RestartAction {

		final protected boolean waitForDeployment;

		public StartOrDeployAction(boolean waitForDeployment, IModule[] modules, DeploymentDescriptor descriptor) {
			super(modules, descriptor);
			this.waitForDeployment = waitForDeployment;
		}

		protected CloudFoundryApplicationModule performDeployment(IProgressMonitor monitor,
				final DeploymentDescriptor descriptor) throws CoreException {
			final Server server = (Server) getServer();
			final CloudFoundryServer cloudServer = getCloudFoundryServer();
			final IModule module = modules[0];
			final CloudFoundryApplicationModule cloudModule = cloudServer.getApplication(module);

			try {

				// Update the local cloud module representing the application
				// first.
				cloudModule.setErrorStatus(null);

				// update mapping
				cloudModule.setApplicationId(descriptor.applicationInfo.getAppName());

				// Update the Staging in the Application module
				cloudModule.setStaging(descriptor.staging);

				server.setModuleState(modules, IServer.STATE_STARTING);

				setRefreshInterval(CloudFoundryClientRequest.MEDIUM_INTERVAL);

				final String applicationId = descriptor.applicationInfo.getAppName();

				// This request does three things:
				// 1. Checks if the application external or mapped to a local
				// project. If mapped to a local project
				// it creates an archive of the application's content
				// 2. If an archive file was created, it pushes the archive
				// file.
				// 3. While pushing the archive file, a check is made to see if
				// the application exists remotely. If not, the application is
				// created in the
				// CF server.

				if (!modules[0].isExternal()) {

					new Request<Void>("Pushing and starting application " + applicationId,
							CloudFoundryClientRequest.DEPLOYMENT_TIMEOUT) {
						@Override
						protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
								throws CoreException {

							if (descriptor.applicationInfo == null) {
								throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
										"Unable to push application: " + applicationId
												+ " due to missing application descriptor."));
							}

							// If the module is not external (meaning that it is
							// mapped to a local, accessible workspace project),
							// create an
							// archive file containing changes to the
							// application's
							// resources. Use incremental publishing if
							// possible.

							// Determine if an archive is provided for the
							// application type. Otherwise generated a .war
							// file.

							IApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(cloudModule
									.getLocalModule());

							if (delegate != null && delegate.providesApplicationArchive(cloudModule.getLocalModule())) {
								IModuleResource[] resources = getResources(modules);

								try {
									ApplicationArchive archive = delegate.getApplicationArchive(
											cloudModule.getLocalModule(), resources);
									descriptor.applicationArchive = archive;
								}
								catch (CoreException e) {
									// Log the error, but continue anyway to
									// see
									// if generating a .war file will work
									// for
									// this application type
									CloudFoundryPlugin.logError(e);
								}
							}

							// If no application archive was provided, then
							// proceed with default creation of a .war file for
							// the application
							if (descriptor.applicationArchive == null) {
								if (descriptor.isIncrementalPublish && !hasChildModules(modules)) {
									// Determine if an incremental publish
									// should
									// occur
									// For the time being support incremental
									// publish
									// only if the app does not have child
									// modules
									// To compute incremental deltas locally,
									// modules must be provided
									// Computes deltas locally before publishing
									// to
									// the server.
									// Potentially more efficient. Should be
									// used
									// only on incremental
									// builds

									handleIncrementalPublish(descriptor, modules);
								}
								else {
									// Create a full war archive
									File warFile = CloudUtil.createWarFile(modules, server, progress);
									if (!warFile.exists()) {
										throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
												"Unable to create war file for application: " + applicationId));
									}

									CloudFoundryPlugin.trace("War file " + warFile.getName() + " created");
									descriptor.applicationInfo.setWarFile(warFile);

								}
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

							pushApplication(client, cloudModule, descriptor, progress);

							CloudFoundryPlugin.trace("Application " + applicationId
									+ " pushed to Cloud Foundry server.");

							cloudServer.tagAsDeployed(module);

							return null;
						}

					}.run(monitor);

				}

				// If reached here it means the application creation and content
				// pushing probably succeeded without errors, therefore attempt
				// to
				// start the application
				super.performDeployment(monitor, descriptor);

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
		ApplicationArchive moduleArchive = new CachingApplicationArchive(Arrays.asList(allResources), changedResources,
				modules[0], descriptor.applicationInfo.getAppName());

		descriptor.applicationArchive = moduleArchive;

	}

	/**
	 * 
	 * Attempts to start an application. It does not create an application, or
	 * incrementally or fully push the application's resources. It simply starts
	 * the application in the server with the application's currently published
	 * resources, regardless of local changes have occurred or not.
	 * 
	 */
	protected class RestartAction extends DeployAction {

		public RestartAction(IModule[] modules, DeploymentDescriptor descriptor) {
			super(modules, descriptor);
		}

		protected CloudFoundryApplicationModule performDeployment(IProgressMonitor monitor,
				final DeploymentDescriptor descriptor) throws CoreException {
			final Server server = (Server) getServer();
			final CloudFoundryServer cloudServer = getCloudFoundryServer();
			final IModule module = modules[0];
			final CloudFoundryApplicationModule cloudModule = cloudServer.getApplication(module);

			try {
				cloudModule.setErrorStatus(null);

				// update mapping
				cloudModule.setApplicationId(descriptor.applicationInfo.getAppName());

				server.setModuleState(modules, IServer.STATE_STARTING);

				final String applicationId = descriptor.applicationInfo.getAppName();

				if (descriptor.applicationInfo == null) {
					server.setModuleState(modules, IServer.STATE_STOPPED);

					throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
							"Unable to start application: " + applicationId
									+ ". Missing application information in application client operation descriptor."));
				}

				if (descriptor.deploymentMode != null) {
					// Start the application. Use a regular request rather than
					// a staging-aware request, as any staging errors should not
					// result in a reattempt, unlike other cases (e.g. get the
					// staging
					// logs or refreshing app instance stats after an app has
					// started).

					CloudFoundryPlugin.getCallback().applicationAboutToStart(getCloudFoundryServer(), cloudModule);

					new Request<Void>("Starting application " + applicationId,
							CloudFoundryClientRequest.DEPLOYMENT_TIMEOUT) {
						@Override
						protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
								throws CoreException {
							CloudFoundryPlugin.trace("Application " + applicationId + " starting");

							switch (descriptor.deploymentMode) {
							case DEBUG:
								// Only launch in Suspend mode
								client.debugApplication(applicationId, DebugModeType.SUSPEND.getDebugMode());
								break;
							default:
								client.stopApplication(applicationId);

								StartingInfo info = client.startApplication(applicationId);
								if (info != null) {

									cloudModule.setStartingInfo(info);

									// Inform through callbacks that application
									// has started
									CloudFoundryPlugin.getCallback().applicationStarting(getCloudFoundryServer(),
											cloudModule);
								}

								break;
							}
							return null;
						}
					}.run(monitor);

					// This should be staging aware, in order to reattempt on
					// staging related issues when checking if an app has
					// started or not
					new StagingAwareRequest<Void>("Waiting for application to start: " + applicationId,
							CloudFoundryClientRequest.DEPLOYMENT_TIMEOUT) {
						@Override
						protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
								throws CoreException {

							// Now verify that the application did start
							try {
								if (!waitForStart(client, applicationId, progress)) {
									server.setModuleState(modules, IServer.STATE_STOPPED);

									throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
											NLS.bind("Starting of {0} timed out", cloudModule.getName())));
								}
							}
							catch (InterruptedException e) {
								server.setModuleState(modules, IServer.STATE_STOPPED);
								throw new OperationCanceledException();
							}

							server.setModuleState(modules, IServer.STATE_STARTED);

							CloudFoundryPlugin.trace("Application " + applicationId + " started");

							CloudFoundryPlugin.getCallback().applicationStarted(getCloudFoundryServer(), cloudModule);

							return null;
						}
					}.run(monitor);
				}
				else {
					// Missing a deployment mode is acceptable, as the
					// user may have elected
					// to push the application but not start it.
					server.setModuleState(modules, IServer.STATE_STOPPED);
				}

				// Refresh the list of modules regardless of whether the
				// application
				// started or not, as a user may have created a new
				// application or pushed
				// new content, but not wished to start the app
				new Request<Void>("Refreshing list of applications", CloudFoundryClientRequest.DEPLOYMENT_TIMEOUT) {
					@Override
					protected Void doRun(final CloudFoundryOperations client, SubMonitor progress) throws CoreException {

						doRefreshModules(cloudServer, client, progress);

						setRefreshInterval(CloudFoundryClientRequest.DEFAULT_INTERVAL);
						CloudFoundryPlugin.getDefault().fireServerRefreshed(cloudServer);

						return null;
					}
				}.run(monitor);

				return cloudModule;
			}
			catch (CoreException e) {
				cloudServer.getApplication(modules[0]).setErrorStatus(e);
				server.setModulePublishState(modules, IServer.PUBLISH_STATE_UNKNOWN);
				throw e;
			}
		}

	}

	/**
	 * Runs a client request, and then performs a refresh after 1 second
	 * interval
	 */
	abstract class RequestWithRefreshCallBack<T> extends Request<T> {
		public T run(IProgressMonitor monitor) throws CoreException {
			T result = super.run(monitor);
			setRefreshInterval(100);
			return result;
		}
	}

	abstract class FileRequest<T> extends Request<T> {

		FileRequest() {
			super("Retrieving file", CloudFoundryClientRequest.SHORT_INTERVAL);
		}

	}

}
