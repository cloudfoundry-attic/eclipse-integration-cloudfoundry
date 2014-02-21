/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CachingApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryLoginHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleResourceDeltaWrapper;
import org.cloudfoundry.ide.eclipse.internal.server.core.RefreshHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommandBuilder;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugModeType;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudOrgsAndSpaces;
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
 * <p/>
 * It's important to note that almost all Java client calls are wrapped around a
 * Request object, and it is important to wrap future client calls around a
 * Request object, as the request object handles automatic client login, server
 * state verification, and proxy handling.
 * 
 * <p/>
 * It is important to note that Application operations like deploying, starting,
 * restarting, update restarting, and stopping should be performed atomically as
 * {@link ApplicationOperation}, as the operation , among other things:
 * <p/>
 * 1. Ensures the deployment information for the application is complete and
 * valid before performing the operation.
 * <p/>
 * 2. Ensures any active refresh jobs running in the background are stopped
 * while the operation is performed
 * <p/>
 * 3. Ensures any active stopped refresh jobs are restarted after the operation.
 * <p/>
 * 4. Handles any common errors associated with these operations, in particular
 * staging errors.
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

	private static final String ERROR_NO_CLOUD_APPLICATION_FOUND = "No cloud module specified when attempting to update application instance stats.";

	private static final String PRE_STAGING_MESSAGE = "Staging application";

	private static final String APP_PUSH_MESSAGE = "Pushing application to Cloud Foundry server";

	private CloudFoundryOperations client;

	private RefreshHandler refreshHandler;

	private ApplicationUrlLookupService applicationUrlLookup;

	/*
	 * FIXNS: Until V2 MCF is released, disable debugging support for V2, as
	 * public clouds also indicate they support debug.
	 */
	private DebugSupportCheck isDebugModeSupported = DebugSupportCheck.UNSUPPORTED;

	private IServerListener serverListener = new IServerListener() {

		public void serverChanged(ServerEvent event) {
			if (event.getKind() == ServerEvent.SERVER_CHANGE) {
				// reset client to consume updated credentials at a later stage.
				// Do not connect
				// right away
				//
				internalResetClient();
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

		new BehaviourRequest<Void>(NLS.bind("Loggging in to {0}", cloudServer.getUrl())) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.login();
				return null;
			}
		}.run(monitor);

		Server server = (Server) cloudServer.getServerOriginal();
		server.setServerState(IServer.STATE_STARTED);
		server.setServerPublishState(IServer.PUBLISH_STATE_NONE);

		getApplicationUrlLookup().refreshDomains(monitor);

		refreshModules(monitor);
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

	public RefreshHandler getRefreshHandler() {
		return refreshHandler;
	}

	/**
	 * Creates the given list of services
	 * @param services
	 * @param monitor
	 * @throws CoreException
	 */
	public void createService(final CloudService[] services, IProgressMonitor monitor) throws CoreException {

		new BehaviourRequest<Void>(services.length == 1 ? NLS.bind("Creating service {0}", services[0].getName())
				: "Creating services") {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				for (CloudService service : services) {
					client.createService(service);
				}

				return null;
			}
		}.run(monitor);
		ServerEventHandler.getDefault().fireServicesUpdated(getCloudFoundryServer());
	}

	public synchronized List<CloudDomain> getDomainsFromOrgs(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<List<CloudDomain>>("Getting domains for orgs") {
			@Override
			protected List<CloudDomain> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getDomainsForOrg();
			}
		}.run(monitor);

	}

	public synchronized List<CloudDomain> getDomainsForSpace(IProgressMonitor monitor) throws CoreException {

		return new BehaviourRequest<List<CloudDomain>>("Getting domains for current space") {
			@Override
			protected List<CloudDomain> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getDomains();
			}
		}.run(monitor);
	}

	/**
	 * Deletes the given modules. Note that any refresh job is stopped while
	 * this operation is running, and restarted after its complete.
	 * @param modules
	 * @param deleteServices
	 * @param monitor
	 * @throws CoreException
	 */
	public void deleteModules(final IModule[] modules, final boolean deleteServices, IProgressMonitor monitor)
			throws CoreException {
		new DeleteModulesOperation(this, modules, deleteServices, this).run(monitor);
	}

	public void deleteApplication(String appName, IProgressMonitor monitor) throws CoreException {
		final String applicationName = appName;
		new BehaviourRequest<Void>("Deleting applications") {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteApplication(applicationName);

				return null;
			}
		}.run(monitor);

	}

	/**
	 * Deletes the list of services.
	 * @param services
	 * @throws CoreException if error occurred during service deletion.
	 */
	public ICloudFoundryOperation getDeleteServicesOperation(final List<String> services) throws CoreException {
		return new BehaviourOperation(CloudFoundryServerBehaviour.this) {

			@Override
			protected void performOperation(IProgressMonitor monitor) throws CoreException {
				new BehaviourRequest<Void>("Deleting services") {
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
		};
	}

	/**
	 * The Cloud application URL lookup is used to resolve a list of URL domains
	 * that an application can user when specifying a URL.
	 * <p/>
	 * Note that this only returns a cached lookup. The lookup may have to be
	 * refreshed separately to get the most recent list of domains.
	 * @return Lookup to retrieve list of application URL domains, as well as
	 * verify validity of an application URL. May be null as its a cached
	 * version.
	 */
	public ApplicationUrlLookupService getApplicationUrlLookup() {
		if (applicationUrlLookup == null) {
			try {
				applicationUrlLookup = new ApplicationUrlLookupService(getCloudFoundryServer());
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(
						NLS.bind("Failed to create the Cloud Foundry Application URL lookup service due to {0}",
								e.getMessage()), e);
			}
		}
		return applicationUrlLookup;
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

		getRefreshHandler().stop();
		CloudFoundryServer cloudServer = getCloudFoundryServer();

		Collection<CloudFoundryApplicationModule> cloudModules = cloudServer.getExistingCloudModules();

		for (CloudFoundryApplicationModule appModule : cloudModules) {
			CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, cloudServer);
		}

		Set<CloudFoundryApplicationModule> deletedModules = new HashSet<CloudFoundryApplicationModule>(cloudModules);

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
		return new BehaviourRequest<CloudApplication>(NLS.bind("Getting Application {0}", applicationId)) {
			@Override
			protected CloudApplication doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplication(applicationId);
			}
		}.run(monitor);
	}

	public List<CloudApplication> getApplications(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<List<CloudApplication>>("Getting applications") {
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
		return new FileRequest<String>() {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, path);
			}
		}.run(monitor);
	}

	public String getFile(final String applicationId, final int instanceIndex, final String filePath,
			final int startPosition, IProgressMonitor monitor) throws CoreException {
		return new FileRequest<String>() {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, filePath, startPosition);
			}
		}.run(monitor);
	}

	public List<CloudServiceOffering> getServiceOfferings(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<List<CloudServiceOffering>>("Getting available service options") {
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
		new BehaviourRequest<Object>("Deleting all applications") {
			@Override
			protected Object doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteAllApplications();
				return null;
			}
		}.run(monitor);
	}

	public List<CloudService> getServices(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<List<CloudService>>("Getting available services") {
			@Override
			protected List<CloudService> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getServices();
			}
		}.run(monitor);
	}

	/**
	 * Refresh the application modules and reschedules the app module refresh
	 * job to execute at certain intervals. This will synch all local
	 * application modules with the actual deployed applications. This may be a
	 * long running operation.
	 * @param monitor
	 */
	public void refreshModules(IProgressMonitor monitor) {
		try {
			final CloudFoundryServer cloudServer = getCloudFoundryServer();

			// Get updated list of cloud applications from the server
			List<CloudApplication> applications = getApplications(monitor);

			// update applications and deployments from server
			Map<String, CloudApplication> deployedApplicationsByName = new LinkedHashMap<String, CloudApplication>();

			for (CloudApplication application : applications) {
				deployedApplicationsByName.put(application.getName(), application);
			}

			cloudServer.updateModules(deployedApplicationsByName);
		}
		catch (Throwable t) {
			// refresh operations MUST not block any other operation.
			// therefore catch all errors and log them
			CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_FAILED_MODULE_REFRESH, t.getMessage()));
		}
	}

	/**
	 * Resets the client. Note that any cached information used by the previous
	 * client will be cleared. Credentials used to reset the client will be
	 * retrieved from the the local server store.
	 * @param monitor
	 * @throws CoreException failure to reset client, disconnect using current
	 * client, or login/connect to the server using new client
	 */
	public CloudFoundryOperations resetClient(IProgressMonitor monitor) throws CoreException {
		return resetClient(null, monitor);
	}

	/**
	 * Public for testing only. Clients should not call outside of test
	 * framework.Use {@link #resetClient(IProgressMonitor)} for actual client
	 * reset, as credentials should not be normally be passed through this API.
	 * Credentials typically are stored and retrieved indirectly by the
	 * behaviour through the server instance.
	 * 
	 * @param monitor
	 * @param credentials
	 * @throws CoreException
	 */
	public CloudFoundryOperations resetClient(CloudCredentials credentials, IProgressMonitor monitor)
			throws CoreException {
		internalResetClient();
		return getClient(credentials, monitor);
	}

	protected void internalResetClient() {
		client = null;
		applicationUrlLookup = null;
	}

	/**
	 * Starts an application in debug mode. Should ONLY be called if the
	 * application is currently stopped. Otherwise use
	 * {@link #updateRestartDebugModule(IModule[], boolean, IProgressMonitor)}.
	 * @param modules
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public CloudFoundryApplicationModule debugModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		return doDebugModule(false, modules, false, monitor);
	}

	/**
	 * Deploys or starts an app in debug mode and either a full publish or
	 * incremental publish may be specified. If incremental publish, changes in
	 * the app are automatically computed and only those changes are pushed to
	 * the server.
	 * @param modules
	 * @param fullPublish
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	protected CloudFoundryApplicationModule doDebugModule(boolean incrementalPublish, IModule[] modules,
			final boolean stopModule, IProgressMonitor monitor) throws CoreException {

		boolean waitForDeployment = true;

		ApplicationOperation op = new StartOrDeployOperation(waitForDeployment, incrementalPublish, modules) {

			@Override
			protected CloudFoundryApplicationModule prepareForDeployment(IProgressMonitor monitor) throws CoreException {

				if (stopModule) {
					stopModule(modules, monitor);
				}

				CloudFoundryApplicationModule appModule = super.prepareForDeployment(monitor);

				DeploymentInfoWorkingCopy workingCopy = appModule.resolveDeploymentInfoWorkingCopy(monitor);
				workingCopy.setDeploymentMode(ApplicationAction.DEBUG);
				workingCopy.save();
				return appModule;
			}

		};
		op.run(monitor);
		return op.getApplicationModule();

	}

	/**
	 * Deploys or starts a module by first doing a full publish of the app.
	 * @param modules containing {@link IModule} corresponding to the
	 * application that needs to be started.
	 * @param waitForDeployment true if start or deploy operation should wait
	 * until the application has been deployed before the operation terminates.
	 * 
	 * @return {@link ApplicationOperation} that performs the operation when
	 * run.
	 * @throws CoreException
	 * @throws {@link OperationCanceledException} if deployment or start
	 * cancelled.
	 */
	public ICloudFoundryOperation getDeployStartApplicationOperation(final IModule[] modules, boolean waitForDeployment)
			throws CoreException {
		return internalGetDeployStartApplicationOperation(modules, waitForDeployment);
	}

	protected ApplicationOperation internalGetDeployStartApplicationOperation(IModule[] modules,
			boolean waitForDeployment) throws CoreException {
		boolean incrementalPublish = false;
		return getDeployStartApplicationOperation(incrementalPublish, modules, waitForDeployment);
	}

	/**
	 * Deploys or starts a module by doing either a full publish or incremental.
	 * @param isIncremental true if incremental publish should be attempted.
	 * False otherwise
	 * @param modules
	 * @param waitForDeployment wait for the application to start. Waiting for
	 * an application to start may take a long time, depending on the
	 * application type. False otherwise
	 * @return {@link ICloudFoundryOperation} that performs the operation when
	 * run.
	 * @throws CoreException
	 */
	protected ApplicationOperation getDeployStartApplicationOperation(boolean isIncremental, final IModule[] modules,
			boolean waitForDeployment) throws CoreException {
		return new StartOrDeployOperation(waitForDeployment, isIncremental, modules);
	}

	/**
	 * Returns non-null Cloud application module mapped to the first module in
	 * the list of modules. If the cloud module module does not exist for the
	 * given module, it will attempt to create it. To avoid re-creating a cloud
	 * application module that may have been deleted, restrict invoking this
	 * method to only operations that start, restart, or update an application.
	 * Should not be called when deleting an application.
	 * @param local WST modules representing app to be deployed.
	 * @return non-null Cloud Application module mapped to the given WST module.
	 * @throws CoreException if no modules specified or mapped cloud application
	 * module cannot be resolved.
	 */
	protected CloudFoundryApplicationModule getOrCreateCloudApplicationModule(IModule[] modules) throws CoreException {

		CloudFoundryApplicationModule appModule = null;
		if (modules == null || modules.length == 0) {
			throw CloudErrorUtil.toCoreException(Messages.ERROR_NO_WST_MODULE);
		}
		else {
			IModule module = modules[0];

			CloudFoundryServer cloudServer = getCloudFoundryServer();

			appModule = cloudServer.getCloudModule(module);

			if (appModule == null) {
				throw CloudErrorUtil
						.toCoreException(NLS.bind(Messages.ERROR_NO_MAPPED_CLOUD_MODULE, modules[0].getId()));

			}

		}

		return appModule;
	}

	@Override
	public void startModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		ICloudFoundryOperation operation = getDeployStartApplicationOperation(modules, false);
		operation.run(monitor);
	}

	public CloudFoundryApplicationModule startModuleWaitForDeployment(IModule[] modules, IProgressMonitor monitor)
			throws CoreException {
		ApplicationOperation operation = internalGetDeployStartApplicationOperation(modules, true);
		operation.run(monitor);
		return operation.getApplicationModule();
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
			CloudFoundryPlugin.log(e1);
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
			CloudFoundryPlugin.log(e);
		}
	}

	@Override
	public void stopModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		getStopAppOperation(modules).run(monitor);
	}

	public ApplicationOperation getStopAppOperation(IModule[] modules) {
		return new StopApplicationOperation(modules);
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
		return doDebugModule(true, modules, isIncrementalPublishing, monitor);
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
			ICloudFoundryOperation operation = getRestartOperation(modules);
			operation.run(monitor);
		}

	}

	/**
	 * This will restart an application in debug mode only. Does not push
	 * application changes or create an application. Application must exist in
	 * the CF server if using this operation.
	 * @param modules
	 * @param monitor
	 * @throws CoreException
	 */
	public void restartDebugModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {

		new RestartOperation(modules) {

			@Override
			protected CloudFoundryApplicationModule prepareForDeployment(IProgressMonitor monitor) throws CoreException {
				// Explicitly stop the module to ensure any existing debugger
				// connections are terminated prior to starting a new connection
				stopModule(modules, monitor);

				CloudFoundryApplicationModule appModule = super.prepareForDeployment(monitor);

				DeploymentInfoWorkingCopy workingCopy = appModule.resolveDeploymentInfoWorkingCopy(monitor);
				workingCopy.setDeploymentMode(ApplicationAction.DEBUG);
				workingCopy.save();

				return appModule;
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
	 * 
	 * @param isIncrementalPublishing true if optimised incremental publishing
	 * should be enabled. False otherwise
	 * @throws CoreException
	 */
	public ICloudFoundryOperation getUpdateRestartOperation(IModule[] modules, boolean isIncrementalPublishing)
			throws CoreException {
		return getDeployStartApplicationOperation(isIncrementalPublishing, modules, false);
	}

	/**
	 * This will restart an application in run mode. It does not restart an
	 * application in debug mode. Does not push application resources or create
	 * the application. The application must exist in the CloudFoundry server.
	 * @param modules
	 * @throws CoreException
	 */
	public ICloudFoundryOperation getRestartOperation(IModule[] modules) throws CoreException {
		return new RestartOperation(modules);
	}

	/**
	 * Updates an the number of application instances. Does not restart the
	 * application if the application is already running. The CF server does
	 * allow instance scaling to occur while the application is running.
	 * @param module representing the application. must not be null or empty
	 * @param instanceCount must be 1 or higher.
	 * @param monitor
	 * @throws CoreException if error occurred during or after instances are
	 * updated.
	 */
	public void updateApplicationInstances(final CloudFoundryApplicationModule module, final int instanceCount,
			IProgressMonitor monitor) throws CoreException {
		final String appName = module.getApplication().getName();
		new AppInStoppedStateAwareRequest<Void>("Updating application instances") {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationInstances(appName, instanceCount);
				return null;
			}
		}.run(monitor);

		ServerEventHandler.getDefault().fireInstancesUpdated(getCloudFoundryServer());
	}

	public void updatePassword(final String newPassword, IProgressMonitor monitor) throws CoreException {
		new BehaviourRequest<Void>("Updating password") {

			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updatePassword(newPassword);
				return null;
			}

		}.run(monitor);
	}

	/**
	 * Updates an application's memory. Does not restart an application if the
	 * application is currently running. The CF server does allow memory scaling
	 * to occur while the application is running.
	 * @param module must not be null or empty
	 * @param memory must be above zero.
	 * @param monitor
	 * @throws CoreException if error occurred during or after memory is scaled.
	 * Exception does not always mean that the memory changes did not take
	 * effect. Memory could have changed, but some post operation like
	 * refreshing may have failed.
	 */
	public void updateApplicationMemory(final CloudFoundryApplicationModule module, final int memory,
			IProgressMonitor monitor) throws CoreException {
		final String appName = module.getApplication().getName();
		new AppInStoppedStateAwareRequest<Void>(NLS.bind("Updating application memory for {0}",
				module.getDeployedApplicationName())) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationMemory(appName, memory);
				return null;
			}
		}.run(monitor);
	}

	public void updateApplicationUrls(final String appName, final List<String> uris, IProgressMonitor monitor)
			throws CoreException {
		new AppInStoppedStateAwareRequest<Void>(NLS.bind("Updating application URLs for {0}", appName)) {
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

	public void refreshApplicationBoundServices(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		DeploymentInfoWorkingCopy copy = appModule.resolveDeploymentInfoWorkingCopy(monitor);
		List<CloudService> boundServices = copy.getServices();
		if (boundServices != null && !boundServices.isEmpty()) {

			List<CloudService> allServices = getServices(monitor);
			if (allServices != null) {
				Map<String, CloudService> existingAsMap = new HashMap<String, CloudService>();

				for (CloudService existingServices : allServices) {
					existingAsMap.put(existingServices.getName(), existingServices);
				}

				List<CloudService> updatedServices = new ArrayList<CloudService>();

				for (CloudService boundService : boundServices) {
					CloudService updatedService = existingAsMap.get(boundService.getName());
					// Check if there is an updated mapping to an actual Cloud
					// Service or retain the old one.
					if (updatedService != null) {
						updatedServices.add(updatedService);
					}
					else {
						updatedServices.add(boundService);
					}
				}

				copy.setServices(updatedServices);
				copy.save();
			}

		}
	}

	public void updateServicesAndCloseCaldecottTunnels(String appName, List<String> services, IProgressMonitor monitor)
			throws CoreException {
		updateServices(appName, services, true, monitor);

	}

	protected void updateServices(final String appName, final List<String> services,
			final boolean closeRelatedCaldecottTunnels, IProgressMonitor monitor) throws CoreException {
		new StagingAwareRequest<Void>("Update services") {
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
		new BehaviourRequest<Void>("Registering account") {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.register(email, password);
				return null;
			}
		}.run(monitor);
	}

	/**
	 * Gets the active client used by the behaviour for server operations.
	 * However, clients are created lazily, and invoking it multipe times does
	 * not recreate the client, as only one client is created per lifecycle of
	 * the server behaviour (but not necessarily per connection session, as the
	 * server behaviour may be created and disposed multiple times by the WST
	 * framework). To use the server-stored credentials, pass null credentials.
	 * <p/>
	 * This API is not suitable to changing credentials. User appropriate API
	 * for the latter like {@link #updatePassword(String, IProgressMonitor)}
	 */
	protected synchronized CloudFoundryOperations getClient(CloudCredentials credentials, IProgressMonitor monitor)
			throws CoreException {
		if (client == null) {
			CloudFoundryServer cloudServer = getCloudFoundryServer();

			String url = cloudServer.getUrl();
			if (!cloudServer.hasCloudSpace()) {
				throw CloudErrorUtil.toCoreException(NLS.bind(Messages.ERROR_FAILED_CLIENT_CREATION_NO_SPACE,
						cloudServer.getServerId()));
			}

			CloudFoundrySpace cloudFoundrySpace = cloudServer.getCloudFoundrySpace();

			if (credentials != null) {
				client = createClient(url, credentials, cloudFoundrySpace, cloudServer.getSelfSignedCertificate());
			}
			else {
				String userName = getCloudFoundryServer().getUsername();
				String password = getCloudFoundryServer().getPassword();
				client = createClient(url, userName, password, cloudFoundrySpace,
						cloudServer.getSelfSignedCertificate());
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
	protected synchronized CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
		return getClient((CloudCredentials) null, monitor);
	}

	/**
	 * Updates the application's environment variables. Note that the
	 * application needs to first exist in the server, and be in a state that
	 * will accept environment variable changes (either stopped, or running
	 * after staging has completed). WARNING: The {@link CloudApplication}
	 * mapping in the module WILL be updated if the environment variable update
	 * is successful, which will replace any existing deployment info in the app
	 * module.
	 * @param appModule
	 * @param monitor
	 * @throws CoreException
	 */
	public void updateEnvironmentVariables(final CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		new BehaviourRequest<Void>("Updating environment variables") {

			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				// Update environment variables.
				Map<String, String> varsMap = new HashMap<String, String>();

				SubMonitor subProgress = SubMonitor.convert(progress);
				subProgress
						.setTaskName("Updating environment variables for: " + appModule.getDeployedApplicationName());

				try {
					List<EnvironmentVariable> vars = appModule.getDeploymentInfo().getEnvVariables();

					if (vars != null) {
						for (EnvironmentVariable var : vars) {
							varsMap.put(var.getVariable(), var.getValue());
						}
					}

					client.updateApplicationEnv(appModule.getDeployedApplicationName(), varsMap);

					// Update the cloud application which contains the updated
					// environment variables.
					CloudApplication cloudApplication = getApplication(appModule.getDeployedApplicationName(),
							subProgress);
					appModule.setCloudApplication(cloudApplication);

				}
				finally {
					subProgress.done();
				}

				return null;
			}

		}.run(monitor);

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

	private boolean waitForStart(CloudFoundryOperations client, String deploymentId, IProgressMonitor monitor)
			throws InterruptedException {
		long initialInterval = CloudOperationsConstants.SHORT_INTERVAL;
		Thread.sleep(initialInterval);
		long timeLeft = CloudOperationsConstants.DEPLOYMENT_TIMEOUT - initialInterval;
		while (timeLeft > 0) {
			CloudApplication deploymentDetails = client.getApplication(deploymentId);
			if (isApplicationReady(deploymentDetails)) {
				return true;
			}
			Thread.sleep(CloudOperationsConstants.ONE_SECOND_INTERVAL);
			timeLeft -= CloudOperationsConstants.ONE_SECOND_INTERVAL;
		}
		return false;
	}

	private CloudApplication getDeployedCloudApplication(CloudFoundryOperations client, String applicationId,
			IProgressMonitor monitor) {
		long timeLeft = CloudOperationsConstants.UPLOAD_TIMEOUT;
		while (timeLeft > 0) {
			CloudApplication application = client.getApplication(applicationId);
			if (applicationId.equals(application.getName())) {
				return application;
			}
			try {
				Thread.sleep(CloudOperationsConstants.SHORT_INTERVAL);
			}
			catch (InterruptedException e) {
				// Ignore. Try again until time runs out
			}
			timeLeft -= CloudOperationsConstants.SHORT_INTERVAL;
		}
		return null;
	}

	/**
	 * Will fetch the latest list of cloud applications from the server, and
	 * update the local module mappings accordingly.
	 * @param cloudServer
	 * @param monitor
	 * @return true if refresh was performed. False otherwise.
	 */

	@Override
	protected void initialize(IProgressMonitor monitor) {
		super.initialize(monitor);
		getServer().addServerListener(serverListener, ServerEvent.SERVER_CHANGE);
		try {
			refreshHandler = new RefreshHandler(getCloudFoundryServer());
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(Messages.ERROR_INITIALISE_REFRESH_NO_SERVER);
		}
	}

	@Override
	public IStatus publish(int kind, IProgressMonitor monitor) {
		try {
			if (kind == IServer.PUBLISH_CLEAN) {
				List<IModule[]> allModules = getAllModules();
				for (IModule[] module : allModules) {
					if (!module[0].isExternal()) {
						startModule(module, monitor);
					}
				}
				return Status.OK_STATUS;
			}
			else if (kind == IServer.PUBLISH_INCREMENTAL) {
				List<IModule[]> allModules = getAllModules();
				for (IModule[] module : allModules) {
					CloudApplication app = getCloudFoundryServer().getCloudModule(module[0]).getApplication();
					if (app != null) {
						int publishState = getServer().getModulePublishState(module);
						if (publishState != IServer.PUBLISH_STATE_NONE) {
							startModule(module, monitor);
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
			final CloudFoundryApplicationModule cloudModule = cloudServer.getCloudModule(module[0]);
			if (cloudModule.getApplication() != null) {
				new BehaviourRequest<Void>(NLS.bind("Deleting application {0}",
						cloudModule.getDeployedApplicationName())) {
					@Override
					protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
						client.deleteApplication(cloudModule.getDeployedApplicationName());
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
			CloudFoundryApplicationModule cloudModule = cloudServer.getExistingCloudModule(module);

			if (cloudModule == null) {
				CloudFoundryPlugin.logError("No cloud application module found for: " + module.getId()
						+ ". Unable to determine debug mode.");
				return null;
			}

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
	 * app instance stats updated. As the application module also has a
	 * reference to the actual cloud application, an updated cloud application
	 * will be mapped.
	 * @param module whos application instances and stats should be updated
	 * @param monitor
	 * @throws CoreException
	 */
	public void updateApplicationInstanceStats(IModule module, IProgressMonitor monitor) throws CoreException {
		if (module != null) {

			CloudFoundryApplicationModule appModule = getCloudFoundryServer().getExistingCloudModule(module);

			if (appModule != null) {
				internalUpdateAppStats(appModule, monitor);
			}
		}
	}

	/**
	 * True if the application is running. False otherwise. Note that an
	 * application refresh is performed on the cloud module, therefore the
	 * mapping between the cloud application and the module will always be
	 * updated with this call.
	 * @param appModule
	 * @param monitor
	 * @return true if application is running. False otherwise.
	 */
	public boolean isApplicationRunning(CloudFoundryApplicationModule appModule, IProgressMonitor monitor) {
		try {
			// Refresh the stats FIRST before checking for the app state, as
			// stat refresh will upate the cloud application mapping (and
			// therefore also update the app state)
			return internalUpdateAppStats(appModule, monitor) && appModule.getApplication() != null
					&& isApplicationReady(appModule.getApplication());
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	/**
	 * Retrieves the orgs and spaces for the current server instance.
	 * @param monitor
	 * @return
	 * @throws CoreException if it failed to retrieve the orgs and spaces.
	 */
	public CloudOrgsAndSpaces getCloudSpaces(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<CloudOrgsAndSpaces>("Getting orgs and spaces") {

			@Override
			protected CloudOrgsAndSpaces doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return internalGetCloudSpaces(client);
			}

		}.run(monitor);
	}

	/**
	 * Attempts to retrieve cloud spaces using the given set of credentials and
	 * server URL. This bypasses the session client in a Cloud Foundry server
	 * instance, if one exists for the given server URL, and therefore attempts
	 * to retrieve the cloud spaces with a disposable, temporary client that
	 * logs in with the given credentials.Therefore, if fetching orgs and spaces
	 * from an existing server instance, please use
	 * {@link CloudFoundryServerBehaviour#getCloudSpaces(IProgressMonitor)}.
	 * @param client
	 * @param selfSigned true if connecting to a self-signing server. False
	 * otherwise
	 * @param monitor which performs client login checks, and basic error
	 * handling. False if spaces should be obtained directly from the client
	 * API.
	 * 
	 * @return resolved orgs and spaces for the given credential and server URL.
	 */
	public static CloudOrgsAndSpaces getCloudSpacesExternalClient(CloudCredentials credentials, final String url,
			boolean selfSigned, IProgressMonitor monitor) throws CoreException {

		final CloudFoundryOperations operations = CloudFoundryServerBehaviour.createClient(url, credentials.getEmail(),
				credentials.getPassword(), selfSigned);

		return new ClientRequest<CloudOrgsAndSpaces>("Getting orgs and spaces") {
			@Override
			protected CloudOrgsAndSpaces doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return internalGetCloudSpaces(client);
			}

			@Override
			protected CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
				return operations;
			}

			protected String getCloudServerUrl() throws CoreException {
				return url;
			}
		}.run(monitor);

	}

	/**
	 * This should be called within a {@link ClientRequest}, as it makes a
	 * direct client call.
	 * @param client
	 * @return
	 */
	private static CloudOrgsAndSpaces internalGetCloudSpaces(CloudFoundryOperations client) {
		List<CloudSpace> foundSpaces = client.getSpaces();
		if (foundSpaces != null && !foundSpaces.isEmpty()) {
			List<CloudSpace> actualSpaces = new ArrayList<CloudSpace>(foundSpaces);
			CloudOrgsAndSpaces orgsAndSpaces = new CloudOrgsAndSpaces(actualSpaces);
			return orgsAndSpaces;
		}

		return null;
	}

	/**
	 * Updates the application instances stats for the given cloud application
	 * module. It does not update the Cloud app module -> cloud application
	 * mapping.
	 * @param appModule cannot be null.
	 * @param monitor
	 * @throws CoreException error in retrieving application instances stats
	 * from the server.
	 * @return true if application stats are refreshed (application is running).
	 * False is application is not running and stats could not be fetched.
	 */
	protected boolean internalUpdateAppStats(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {

		// Update the CloudApplication in the cloud module.
		CloudApplication application = getApplication(appModule.getDeployedApplicationName(), monitor);
		appModule.setCloudApplication(application);

		if (application == null) {
			throw CloudErrorUtil.toCoreException(ERROR_NO_CLOUD_APPLICATION_FOUND);
		}

		InstancesInfo info = internalUpdateInstancesInfo(appModule, monitor);
		ApplicationStats stats = internalUpdateStats(appModule, monitor);
		return info != null && stats != null;
	}

	protected ApplicationStats internalUpdateStats(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		ApplicationStats stats = getApplicationStats(appModule.getDeployedApplicationName(), monitor);
		appModule.setApplicationStats(stats);

		return stats;
	}

	protected InstancesInfo internalUpdateInstancesInfo(CloudFoundryApplicationModule appModule,
			IProgressMonitor monitor) throws CoreException {
		InstancesInfo info = getInstancesInfo(appModule.getDeployedApplicationName(), monitor);
		appModule.setInstancesInfo(info);

		return info;
	}

	public static void validate(String location, String userName, String password, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN);
		try {
			CloudFoundryOperations client = createClient(location, userName, password, selfSigned);
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

	public static void register(String location, String userName, String password, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN);
		try {
			CloudFoundryOperations client = createClient(location, userName, password, selfSigned);
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
	 * verification. The session client for the server behaviour is created when
	 * the latter is created
	 * @param location
	 * @param userName
	 * @param password
	 * @param selfSigned true if connecting to self-signing server. False
	 * otherwise
	 * @return
	 * @throws CoreException
	 */
	static CloudFoundryOperations createClient(String location, String userName, String password, boolean selfSigned)
			throws CoreException {
		return createClient(location, userName, password, null, selfSigned);
	}

	/**
	 * Creates a new client to the given server URL using the specified
	 * credentials. This does NOT connect the client to the server, nor does it
	 * set the client as a session client for the server delegate.
	 * 
	 * @param serverURL must not be null
	 * @param userName must not be null
	 * @param password must not be null
	 * @param cloudSpace optional, as a valid client can still be created
	 * without org/space (for example, a client can be used to do an org/space
	 * lookup.
	 * @param selfSigned true if connecting to self-signed server. False
	 * otherwise
	 * @return Non-null client.
	 * @throws CoreException if failed to create the client.
	 */
	private static CloudFoundryOperations createClient(String serverURL, String userName, String password,
			CloudFoundrySpace cloudSpace, boolean selfSigned) throws CoreException {
		if (password == null) {
			// lost the password, start with an empty one to avoid assertion
			// error
			password = "";
		}
		return createClient(serverURL, new CloudCredentials(userName, password), cloudSpace, selfSigned);
	}

	/**
	 * Creates a new client to the specified server URL using the given
	 * credentials. This does NOT connect the client to the server, nor does it
	 * set the client as the session client for the server behaviour. The
	 * session client is set indirectly via {@link #connect(IProgressMonitor)}
	 * @param serverURL server to connect to. Must NOT be null.
	 * @param credentials must not be null.
	 * @param cloudSpace optional. Can be null, as a client can be created
	 * without specifying an org/space (e.g. a client can be created for the
	 * purpose of looking up all the orgs/spaces in a server)
	 * @param selfSigned true if connecting to a server with self signed
	 * certificate. False otherwise
	 * @return non-null client.
	 * @throws CoreException if failed to create client.
	 */
	private static CloudFoundryOperations createClient(String serverURL, CloudCredentials credentials,
			CloudFoundrySpace cloudSpace, boolean selfSigned) throws CoreException {

		URL url;
		try {
			url = new URL(serverURL);
			int port = url.getPort();
			if (port == -1) {
				port = url.getDefaultPort();
			}

			// If no cloud space is specified, use appropriate client factory
			// API to create a non-space client
			// NOTE that using a space API with null org and space will result
			// in errors as that API will
			// expect valid org and space values.
			return cloudSpace != null ? CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(
					credentials, url, cloudSpace.getOrgName(), cloudSpace.getSpaceName(), selfSigned)
					: CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(credentials, url,
							selfSigned);
		}
		catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
					"The server url ''{0}'' is invalid: {1}", serverURL, e.getMessage()), e));
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
	 * 
	 * Request that is aware of potential staging related errors and may attempt
	 * the request again on certain types of staging errors like Staging Not
	 * Finished errors.
	 * <p/>
	 * Because the set of client operations wrapped around this Request may be
	 * attempted again on certain types of errors, it's best to keep the set of
	 * client operations as minimal as possible, to avoid performing client
	 * operations again that had no errors.
	 * 
	 * <p/>
	 * Note that this should only be used around certain types of operations
	 * performed on a app that is already started, like fetching the staging
	 * logs, or app instances stats, as re-attempts on these operations due to
	 * staging related errors (e.g. staging not finished yet) is permissable.
	 * 
	 * <p/>
	 * However, operations not related an application being in a running state
	 * (e.g. creating a service, getting list of all apps), should not use this
	 * request.
	 */
	abstract class StagingAwareRequest<T> extends BehaviourRequest<T> {

		public StagingAwareRequest(String label) {
			super(label);
		}

		protected long getWaitInterval(Throwable exception, SubMonitor monitor) throws CoreException {

			if (exception instanceof CoreException) {
				exception = ((CoreException) exception).getCause();
			}

			if (exception instanceof NotFinishedStagingException) {
				return CloudOperationsConstants.ONE_SECOND_INTERVAL * 2;
			}
			else if (exception instanceof CloudFoundryException
					&& CloudErrorUtil.isAppStoppedStateError((CloudFoundryException) exception)) {
				return CloudOperationsConstants.ONE_SECOND_INTERVAL;
			}
			return -1;
		}

		protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	}

	/**
	 * 
	 * Reattempts the operation if a app in stopped state error is encountered.
	 * 
	 */
	abstract class AppInStoppedStateAwareRequest<T> extends BehaviourRequest<T> {

		public AppInStoppedStateAwareRequest(String label) {
			super(label);
		}

		protected long getWaitInterval(Throwable exception, SubMonitor monitor) throws CoreException {

			if (exception instanceof CoreException) {
				exception = ((CoreException) exception).getCause();
			}

			if (exception instanceof CloudFoundryException
					&& CloudErrorUtil.isAppStoppedStateError((CloudFoundryException) exception)) {
				return CloudOperationsConstants.ONE_SECOND_INTERVAL;
			}
			return -1;
		}

		protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	}

	/**
	 * Deploys an application and or starts it in regular or debug mode. If
	 * deployed in debug mode, an attempt will be made to connect the deployed
	 * application to a debugger. An operation should performed atomically PER
	 * APPLICATION.
	 * <p/>
	 * The operation performs some common tasks like checking that the
	 * application's deployment info is complete and valid, and that any refresh
	 * jobs running in the background are stopped prior to starting the
	 * operation, and restarted afterward.
	 * 
	 */
	protected abstract class ApplicationOperation extends AbstractDeploymentOperation {

		final protected IModule[] modules;

		private CloudFoundryApplicationModule appModule;

		protected ApplicationOperation(IModule[] modules) {
			super(CloudFoundryServerBehaviour.this);
			this.modules = modules;
		}

		public CloudFoundryApplicationModule getApplicationModule() {
			return appModule;
		}

		abstract protected String getOperationName();

		protected void performOperation(IProgressMonitor monitor) throws CoreException {

			appModule = prepareForDeployment(monitor);

			IStatus validationStatus = appModule.validateDeploymentInfo();
			if (!validationStatus.isOK()) {
				throw CloudErrorUtil.toCoreException(NLS.bind(Messages.ERROR_APP_DEPLOYMENT_VALIDATION_ERROR,
						appModule.getDeployedApplicationName(), validationStatus.getMessage()));

			}

			// Operation cancelled exceptions after an application has been
			// prepared for deployment should not be logged.
			try {

				CloudFoundryServer cloudServer = getCloudFoundryServer();

				// Stop any consoles
				CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, cloudServer);

				boolean debug = appModule.getDeploymentInfo().getDeploymentMode() == ApplicationAction.DEBUG;

				printlnToConsole(appModule, getOperationName(), true, true, monitor);

				performDeployment(appModule, monitor);

				if (debug) {
					new DebugCommandBuilder(modules, cloudServer).getDebugCommand(
							ApplicationAction.CONNECT_TO_DEBUGGER, null).run(monitor);
				}

				// In addition, starting, restarting, and update-restarting
				// a
				// caldecott app should always
				// disconnect existing tunnels.
				if (TunnelBehaviour.isCaldecottApp(appModule.getDeployedApplicationName())) {
					new TunnelBehaviour(cloudServer).stopAndDeleteAllTunnels(monitor);
				}

				// Refresh the application instance stats as well
				try {
					internalUpdateAppStats(appModule, monitor);
				}
				catch (CoreException e) {
					// Don't let errors in app instance stats stop the
					// completion of the ApplicationOperation
					CloudFoundryPlugin.logError(e);
				}

			}
			catch (OperationCanceledException e) {
				// ignore so webtools does not show an exception
				((Server) getServer()).setModuleState(modules, IServer.STATE_UNKNOWN);
			}

		}

		/**
		 * Prepares an application to either be deployed, started or restarted.
		 * The main purpose to ensure that the application's deployment
		 * information is complete. If incomplete, it will prompt the user for
		 * missing information.
		 * @param monitor
		 * @return Cloud Foundry application mapped to the deployed WST
		 * {@link IModule}. Must not be null. If null, it indicates error,
		 * therefore throw {@link CoreException} instead.
		 * @throws CoreException if any failure during or after the operation.
		 * @throws OperationCanceledException if the user cancelled deploying or
		 * starting the application. The application's deployment information
		 * should not be modified in this case.
		 */
		protected CloudFoundryApplicationModule prepareForDeployment(IProgressMonitor monitor) throws CoreException,
				OperationCanceledException {

			CloudFoundryApplicationModule appModule = getOrCreateCloudApplicationModule(modules);

			CloudFoundryServer cloudServer = getCloudFoundryServer();

			// prompt user for missing details
			CloudFoundryPlugin.getCallback().prepareForDeployment(cloudServer, appModule, monitor);

			return appModule;

		}

		/**
		 * This performs the primary operation of creating an application and
		 * then pushing the application contents to the server. These are
		 * performed in separate requests via the CF client. If the application
		 * does not exist, it is first created through an initial request. Once
		 * the application is created, or if it already exists, the next step is
		 * to upload (push) the application archive containing the application's
		 * resources. This is performed in a second separate request.
		 * <p/>
		 * To avoid replacing the deployment info in the app module, the mapping
		 * to the most recent {@link CloudApplication} in the app module is NOT
		 * updated with newly created application. It is up to the caller to set
		 * the mapping in {@link CloudFoundryApplicationModule}
		 * @param client
		 * @param appModule valid Cloud module with valid deployment info.
		 * @param monitor
		 * @throws CoreException if error creating the application
		 */
		protected void pushApplication(CloudFoundryOperations client, final CloudFoundryApplicationModule appModule,
				File warFile, ApplicationArchive applicationArchive, final IProgressMonitor monitor)
				throws CoreException {

			String appName = appModule.getDeploymentInfo().getDeploymentName();

			try {
				List<CloudApplication> existingApps = client.getApplications();
				boolean found = false;
				for (CloudApplication existingApp : existingApps) {
					if (existingApp.getName().equals(appName)) {
						found = true;
						break;
					}
				}

				// 1. Create the application if it doesn't already exist
				if (!found) {
					Staging staging = appModule.getDeploymentInfo().getStaging();
					List<String> uris = appModule.getDeploymentInfo().getUris() != null ? appModule.getDeploymentInfo()
							.getUris() : new ArrayList<String>(0);
					List<String> services = appModule.getDeploymentInfo().asServiceBindingList();

					if (staging == null) {
						// For v2, a non-null staging is required.
						staging = new Staging();
					}
					client.createApplication(appName, staging, appModule.getDeploymentInfo().getMemory(), uris,
							services);
				}

				// 2. Now push the application content.
				if (warFile != null) {
					client.uploadApplication(appName, warFile);
				}
				else if (applicationArchive != null) {
					// Handle the incremental publish case separately as it
					// requires
					// a partial war file generation of only the changed
					// resources
					// AFTER
					// the server determines the list of missing file names.
					if (applicationArchive instanceof CachingApplicationArchive) {
						final CachingApplicationArchive cachingArchive = (CachingApplicationArchive) applicationArchive;
						client.uploadApplication(appName, cachingArchive, new UploadStatusCallback() {

							public void onProcessMatchedResources(int length) {

							}

							public void onMatchedFileNames(Set<String> matchedFileNames) {
								cachingArchive.generatePartialWarFile(matchedFileNames);
							}

							public void onCheckResources() {

							}

							public boolean onProgress(String status) {
								return false;
							}
						});

						// Once the application has run, do a clean up of the
						// sha1
						// cache for deleted resources

					}
					else {
						printlnToConsole(appModule, "\nProcessing payload...", false, false, monitor);
						client.uploadApplication(appName, applicationArchive, new UploadStatusCallback() {

							public void onProcessMatchedResources(int length) {

							}

							public void onMatchedFileNames(Set<String> matchedFileNames) {
								try {
									printlnToConsole(appModule, ".", false, false, monitor);
								}
								catch (CoreException e) {
									CloudFoundryPlugin.logError(e);
								}
							}

							public void onCheckResources() {

							}

							public boolean onProgress(String status) {
								return false;
							}
						});

					}
				}
				else {
					throw CloudErrorUtil
							.toCoreException(NLS
									.bind("Failed to deploy application {0} since no deployable war or application archive file was generated.",
											appModule.getDeploymentInfo().getDeploymentName()));
				}
			}
			catch (IOException e) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(NLS.bind(
						"Failed to deploy application {0} due to {1}", appModule.getDeploymentInfo()
								.getDeploymentName(), e.getMessage()), e));
			}

		}

		/**
		 * 
		 * @param appModule to be deployed or started
		 * @param monitor
		 * @throws CoreException if error occurred during deployment or starting
		 * the app, or resolving the updated cloud application from the client.
		 */
		protected abstract void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
				throws CoreException;
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
	 * started. One of the notification is to the CF console to display the app
	 * logs in the CF console.
	 * <p/>
	 * Note that ALL client calls need to be wrapped around a Request operation,
	 * as the Request operation performs additional checks prior to invoking the
	 * call, as well as handles errors.
	 */
	protected class StartOrDeployOperation extends RestartOperation {

		final protected boolean waitForDeployment;

		final protected boolean incrementalPublish;

		public StartOrDeployOperation(boolean waitForDeployment, boolean incrementalPublish, IModule[] modules) {
			super(modules);
			this.waitForDeployment = waitForDeployment;
			this.incrementalPublish = incrementalPublish;
		}

		@Override
		protected String getOperationName() {
			return "Starting application";
		}

		protected CloudFoundryApplicationModule prepareForDeployment(IProgressMonitor monitor) throws CoreException {

			CloudFoundryApplicationModule appModule = super.prepareForDeployment(monitor);

			DeploymentInfoWorkingCopy workingCopy = appModule.resolveDeploymentInfoWorkingCopy(monitor);
			workingCopy.setIncrementalPublish(incrementalPublish);
			workingCopy.save();
			return appModule;
		}

		protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
				throws CoreException {
			final Server server = (Server) getServer();
			final CloudFoundryServer cloudServer = getCloudFoundryServer();
			final IModule module = modules[0];

			try {

				// Update the local cloud module representing the application
				// first.
				appModule.setErrorStatus(null);

				server.setModuleState(modules, IServer.STATE_STARTING);

				final String deploymentName = appModule.getDeploymentInfo().getDeploymentName();

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

					printlnToConsole(appModule, "Generating application archive", false, true, monitor);

					final ApplicationArchive applicationArchive = generateApplicationArchiveFile(
							appModule.getDeploymentInfo(), appModule, modules, server, monitor);
					File warFile = null;
					if (applicationArchive == null) {
						// Create a full war archive
						warFile = CloudUtil.createWarFile(modules, server, monitor);
						if (warFile == null || !warFile.exists()) {
							throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
									"Unable to create war file for application: " + deploymentName));
						}

						CloudFoundryPlugin.trace("War file " + warFile.getName() + " created");
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

					final File warFileFin = warFile;
					final CloudFoundryApplicationModule appModuleFin = appModule;
					// Now push the application resources to the server

					printlnToConsole(appModule, APP_PUSH_MESSAGE, false, true, monitor);

					new BehaviourRequest<Void>(NLS.bind("Pushing the application {0} ", deploymentName)) {
						@Override
						protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
								throws CoreException {

							pushApplication(client, appModuleFin, warFileFin, applicationArchive, progress);

							CloudFoundryPlugin.trace("Application " + deploymentName
									+ " pushed to Cloud Foundry server.");

							cloudServer.tagAsDeployed(module);

							return null;
						}

					}.run(monitor);

				}

				// Verify the application exists in the server
				CloudApplication application = getDeployedCloudApplication(client,
						appModule.getDeployedApplicationName(), monitor);

				if (application == null) {
					throw CloudErrorUtil
							.toCoreException("No cloud application obtained from the Cloud Foundry server for :  "
									+ appModule.getDeployedApplicationName()
									+ ". Application may not have deployed correctly to the Cloud Foundry server, or there are connection problems to the server.");
				}

				// At this stage, the app is either created or it already
				// exists.
				// Set the environment variables BEFORE starting the app, and
				// BEFORE updating
				// the cloud application mapping in the module, which will
				// replace the deployment info
				updateEnvironmentVariables(appModule, monitor);

				// Update instances if it is more than 1. By default, app starts
				// with 1 instance.
				int instances = appModule.getDeploymentInfo().getInstances();
				if (instances > 1) {
					updateApplicationInstances(appModule, instances, monitor);
				}

				// If reached here it means the application creation and content
				// pushing probably succeeded without errors, therefore attempt
				// to
				// start the application
				super.performDeployment(appModule, monitor);

			}
			catch (CoreException e) {
				appModule.setErrorStatus(e);
				server.setModulePublishState(modules, IServer.PUBLISH_STATE_UNKNOWN);
				throw e;
			}
		}
	}

	/**
	 * 
	 * @param descriptor that contains the application information, and that
	 * also will be updated with an archive containing the application resources
	 * to be deployed to the Cloud Foundry Server
	 * @param cloudModule the Cloud Foundry wrapper around the application
	 * module to be pushed to the server
	 * @param modules list of WTP modules.
	 * @param cloudServer cloud server where app should be pushed to
	 * @param monitor
	 * @throws CoreException if failure occurred while generated an archive file
	 * containing the application's payload
	 */
	protected ApplicationArchive generateApplicationArchiveFile(ApplicationDeploymentInfo deploymentInfo,
			CloudFoundryApplicationModule cloudModule, IModule[] modules, Server server, IProgressMonitor monitor)
			throws CoreException {

		// Perform local operations like building an archive file
		// and payload for the application
		// resources prior to pushing it to the server.

		// If the module is not external (meaning that it is
		// mapped to a local, accessible workspace project),
		// create an
		// archive file containing changes to the
		// application's
		// resources. Use incremental publishing if
		// possible.

		IApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(cloudModule.getLocalModule());

		ApplicationArchive archive = null;
		if (delegate != null && delegate.providesApplicationArchive(cloudModule.getLocalModule())) {
			IModuleResource[] resources = getResources(modules);

			archive = getApplicationArchive(cloudModule, monitor, delegate, resources);
		}

		// If no application archive was provided,then attempt an incremental
		// publish. Incremental publish is only supported for apps without child
		// modules.
		if (archive == null && deploymentInfo.isIncrementalPublish() && !hasChildModules(modules)) {
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

			archive = getIncrementalPublishArchive(deploymentInfo, modules);
		}
		return archive;

	}

	private ApplicationArchive getApplicationArchive(CloudFoundryApplicationModule cloudModule,
			IProgressMonitor monitor, IApplicationDelegate delegate, IModuleResource[] resources) throws CoreException {
		SubMonitor subProgress = SubMonitor.convert(monitor);
		subProgress.setTaskName("Creating application archive for: " + cloudModule.getDeployedApplicationName());

		ApplicationArchive archive = null;
		try {
			archive = delegate.getApplicationArchive(cloudModule, getCloudFoundryServer(), resources, monitor);
		}
		finally {
			subProgress.done();
		}
		return archive;
	}

	/**
	 * 
	 * @param appModule consoles are associated with a particular deployed
	 * application. This must not be null.
	 * @param message
	 * @param clearConsole true if console should be cleared. False, if message
	 * should be tailed to existing content in the console.
	 * @param runningOperation if it is a message related to an ongoing
	 * operation, which will append "..." to the message
	 * @throws CoreException
	 */
	protected void printlnToConsole(CloudFoundryApplicationModule appModule, String message, boolean clearConsole,
			boolean runningOperation, IProgressMonitor monitor) throws CoreException {
		if (runningOperation) {
			message += "...";
		}
		message += '\n';
		CloudFoundryPlugin.getCallback().printToConsole(getCloudFoundryServer(), appModule, message, clearConsole,
				false, monitor);
	}

	protected ApplicationArchive getIncrementalPublishArchive(final ApplicationDeploymentInfo deploymentInfo,
			IModule[] modules) {
		IModuleResource[] allResources = getResources(modules);
		IModuleResourceDelta[] deltas = getPublishedResourceDelta(modules);
		List<IModuleResource> changedResources = getChangedResources(deltas);
		ApplicationArchive moduleArchive = new CachingApplicationArchive(Arrays.asList(allResources), changedResources,
				modules[0], deploymentInfo.getDeploymentName());

		return moduleArchive;
	}

	/**
	 * 
	 * Attempts to start an application. It does not create an application, or
	 * incrementally or fully push the application's resources. It simply starts
	 * the application in the server with the application's currently published
	 * resources, regardless of local changes have occurred or not.
	 * 
	 */
	protected class RestartOperation extends ApplicationOperation {

		public RestartOperation(IModule[] modules) {
			super(modules);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cloudfoundry.ide.eclipse.internal.server.core.client.
		 * CloudFoundryServerBehaviour
		 * .DeployAction#performDeployment(org.cloudfoundry
		 * .ide.eclipse.internal.
		 * server.core.client.CloudFoundryApplicationModule,
		 * org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
				throws CoreException {
			final Server server = (Server) getServer();
			final CloudFoundryApplicationModule cloudModule = appModule;

			try {
				cloudModule.setErrorStatus(null);

				final String deploymentName = cloudModule.getDeploymentInfo().getDeploymentName();

				server.setModuleState(modules, IServer.STATE_STARTING);

				if (deploymentName == null) {
					server.setModuleState(modules, IServer.STATE_STOPPED);

					throw CloudErrorUtil
							.toCoreException("Unable to start application. Missing application deployment name in application deployment information.");
				}

				if (cloudModule.getDeploymentInfo().getDeploymentMode() != null) {
					// Start the application. Use a regular request rather than
					// a staging-aware request, as any staging errors should not
					// result in a reattempt, unlike other cases (e.g. get the
					// staging
					// logs or refreshing app instance stats after an app has
					// started).

					printlnToConsole(cloudModule, PRE_STAGING_MESSAGE, false, true, monitor);

					new BehaviourRequest<Void>(NLS.bind("Starting application {0}", deploymentName)) {
						@Override
						protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
								throws CoreException {
							CloudFoundryPlugin.trace("Application " + deploymentName + " starting");

							switch (cloudModule.getDeploymentInfo().getDeploymentMode()) {
							case DEBUG:
								// Only launch in Suspend mode
								client.debugApplication(deploymentName, DebugModeType.SUSPEND.getDebugMode());
								break;
							default:
								client.stopApplication(deploymentName);

								StartingInfo info = client.startApplication(deploymentName);
								if (info != null) {

									cloudModule.setStartingInfo(info);

									// Inform through callback that application
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
					new StagingAwareRequest<Void>(NLS.bind("Waiting for application to start: {0}", deploymentName)) {
						@Override
						protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
								throws CoreException {

							// Now verify that the application did start
							try {
								if (!waitForStart(client, deploymentName, progress)) {
									server.setModuleState(modules, IServer.STATE_STOPPED);

									throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
											NLS.bind("Starting of {0} timed out",
													cloudModule.getDeployedApplicationName())));
								}
							}
							catch (InterruptedException e) {
								server.setModuleState(modules, IServer.STATE_STOPPED);
								throw new OperationCanceledException();
							}

							server.setModuleState(modules, IServer.STATE_STARTED);

							CloudFoundryPlugin.trace("Application " + deploymentName + " started");

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

			}
			catch (CoreException e) {
				appModule.setErrorStatus(e);
				server.setModulePublishState(modules, IServer.PUBLISH_STATE_UNKNOWN);
				throw e;
			}
		}

		@Override
		protected String getOperationName() {
			return "Restarting application";
		}
	}

	abstract class FileRequest<T> extends StagingAwareRequest<T> {

		FileRequest() {
			super("Retrieving file");
		}
	}

	class StopApplicationOperation extends ApplicationOperation {

		protected StopApplicationOperation(IModule[] modules) {
			super(modules);
		}

		@Override
		protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
				throws CoreException {
			Server server = (Server) getServer();
			boolean succeeded = false;
			try {
				server.setModuleState(modules, IServer.STATE_STOPPING);

				CloudFoundryServer cloudServer = getCloudFoundryServer();
				final CloudFoundryApplicationModule cloudModule = cloudServer.getExistingCloudModule(modules[0]);

				if (cloudModule == null) {
					throw CloudErrorUtil.toCoreException("Unable to stop application as no cloud module found for: "
							+ modules[0].getName());
				}

				// CloudFoundryPlugin.getCallback().applicationStopping(getCloudFoundryServer(),
				// cloudModule);
				new BehaviourRequest<Void>(NLS.bind("Stopping application {0}",
						cloudModule.getDeployedApplicationName())) {
					@Override
					protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
						client.stopApplication(cloudModule.getDeployedApplicationName());
						return null;
					}
				}.run(monitor);

				server.setModuleState(modules, IServer.STATE_STOPPED);
				succeeded = true;
				printlnToConsole(appModule, "Application stopped.", true, false, monitor);
				CloudFoundryPlugin.getCallback().stopApplicationConsole(cloudModule, cloudServer);

				// If succeeded, stop all Caldecott tunnels if the app is
				// the
				// Caldecott app
				if (TunnelBehaviour.isCaldecottApp(cloudModule.getDeployedApplicationName())) {
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

		@Override
		protected String getOperationName() {
			return "Stopping application";
		}
	}

	abstract class BehaviourRequest<T> extends LocalServerRequest<T> {

		public BehaviourRequest(String label) {
			super(label);
		}

		@Override
		protected CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
			return CloudFoundryServerBehaviour.this.getClient(monitor);
		}

		@Override
		protected CloudFoundryServer getCloudServer() throws CoreException {
			return CloudFoundryServerBehaviour.this.getCloudFoundryServer();
		}

		@Override
		protected String getCloudServerUrl() throws CoreException {
			return getCloudServer().getUrl();
		}
	}

}
