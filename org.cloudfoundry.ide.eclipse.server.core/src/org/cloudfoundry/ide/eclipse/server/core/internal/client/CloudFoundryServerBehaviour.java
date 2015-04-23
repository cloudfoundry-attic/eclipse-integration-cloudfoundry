/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
 *     IBM - wait for all module publish complete before finish up publish operation.
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.ide.eclipse.server.core.AbstractApplicationDelegate;
import org.cloudfoundry.ide.eclipse.server.core.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CachingApplicationArchive;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryLoginHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.ModuleResourceDeltaWrapper;
import org.cloudfoundry.ide.eclipse.server.core.internal.RefreshModulesHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.jrebel.CloudRebelAppHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudOrgsAndSpaces;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.internal.IModuleVisitor;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerPublishInfo;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.springframework.web.client.RestClientException;

/**
 * 
 * This is the primary interface to the underlying Java Cloud client that
 * performs actual requests to a target Cloud space. It contains API to start,
 * stop, restart, and publish applications to a Cloud space, as well as scale
 * application memory, instances, set environment variables and map application
 * URLs.
 * <p/>
 * This is intended as a lower-level interface to interact with the underlying
 * client, as well as to integrate with WST framework
 * <p/>
 * However, the majority of these operations require additional functionality
 * that are specific to the Cloud tooling, like firing refresh and server change
 * events. Therefore, it is advisable to obtain the appropriate
 * {@link ICloudFoundryOperation} for these operations through
 * {@link #operations()}.
 * <p/>
 * It's important to note that as of CF 1.6.1, all WST framework-based
 * publishing will result in server-level publishing, so even if deploying a
 * particular application, other applications that are already deployed and not
 * external (i.e. have a corresponding workspace project) that need republishing
 * may be republished as well.
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from
 * adopter so this class should not be moved or renamed to avoid breakage to
 * adopters.
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

	private RefreshModulesHandler refreshHandler;

	private ApplicationUrlLookupService applicationUrlLookup;

	private CloudBehaviourOperations cloudBehaviourOperations;

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

		new BehaviourRequest<Void>("Loggging in to " + cloudServer.getUrl()) { //$NON-NLS-1$
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

		getRefreshHandler().scheduleRefreshAll();

		ServerEventHandler.getDefault().fireServerEvent(
				new CloudServerEvent(getCloudFoundryServer(), CloudServerEvent.EVENT_SERVER_CONNECTED));
	}

	/**
	 * Cloud operations ( {@link ICloudFoundryOperation} ) that can be performed
	 * on the Cloud space targeted by the server behaviour.
	 * @return Non-Null Cloud Operations
	 */
	public CloudBehaviourOperations operations() {
		if (cloudBehaviourOperations == null) {
			cloudBehaviourOperations = new CloudBehaviourOperations(this);
		}
		return cloudBehaviourOperations;
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
	 * 
	 * @return Handles refresh of modules for this server behaviour. Never null.
	 */
	public RefreshModulesHandler getRefreshHandler() {
		if (refreshHandler == null) {
			CloudFoundryServer server = null;
			try {
				server = getCloudFoundryServer();
			}
			catch (CoreException ce) {
				CloudFoundryPlugin.logError(ce);
			}
			refreshHandler = new RefreshModulesHandler(server);
		}
		return refreshHandler;
	}

	/**
	 * Creates the given list of services
	 * @deprecated Use {@link #operations()} instead.
	 * @param services
	 * @param monitor
	 * @throws CoreException
	 */
	public void createService(final CloudService[] services, IProgressMonitor monitor) throws CoreException {
		operations().createServices(services).run(monitor);
	}

	public synchronized List<CloudDomain> getDomainsFromOrgs(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<List<CloudDomain>>("Getting domains for orgs") { //$NON-NLS-1$
			@Override
			protected List<CloudDomain> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getDomainsForOrg();
			}
		}.run(monitor);

	}

	public synchronized List<CloudDomain> getDomainsForSpace(IProgressMonitor monitor) throws CoreException {

		return new BehaviourRequest<List<CloudDomain>>(Messages.CloudFoundryServerBehaviour_DOMAINS_FOR_SPACE) {
			@Override
			protected List<CloudDomain> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getDomains();
			}
		}.run(monitor);
	}

	/**
	 * Deletes the given modules. Note that any refresh job is stopped while
	 * this operation is running, and restarted after its complete.
	 * @deprecated use {@link #operations()} instead
	 * @param modules
	 * @param deleteServices
	 * @param monitor
	 * @throws CoreException
	 */
	public void deleteModules(final IModule[] modules, final boolean deleteServices, IProgressMonitor monitor)
			throws CoreException {
		operations().deleteModules(modules, deleteServices).run(monitor);
	}

	/**
	 * Deletes a cloud application in the target Cloud space. May throw
	 * {@link CoreException} if the application no longer exists, or failed to
	 * delete..
	 * @param appName
	 * @param monitor
	 * @throws CoreException
	 */
	public void deleteApplication(String appName, IProgressMonitor monitor) throws CoreException {
		final String applicationName = appName;
		new BehaviourRequest<Void>("Deleting applications") { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteApplication(applicationName);

				return null;
			}
		}.run(monitor);

	}

	/**
	 * Deletes the list of services.
	 * @deprecated use {@link #operations()} instead
	 * @param services
	 * @throws CoreException if error occurred during service deletion.
	 */
	public ICloudFoundryOperation getDeleteServicesOperation(final List<String> services) throws CoreException {
		return operations().deleteServices(services);
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
						"Failed to create the Cloud Foundry Application URL lookup service due to {" + //$NON-NLS-1$
								e.getMessage(), e);
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

	/**
	 * Disconnects the local server from the remote CF server, and terminate the
	 * session. Note that this will stop any refresh operations, or console
	 * streaming, but will NOT stop any apps that are currently running. It may
	 * also clear any application module caches associated with the session.
	 * @param monitor
	 * @throws CoreException
	 */
	public void disconnect(IProgressMonitor monitor) throws CoreException {
		CloudFoundryPlugin.getCallback().disconnecting(getCloudFoundryServer());

		Server server = (Server) getServer();
		server.setServerState(IServer.STATE_STOPPING);

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

		ServerEventHandler.getDefault().fireServerEvent(
				new CloudServerEvent(getCloudFoundryServer(), CloudServerEvent.EVENT_SERVER_DISCONNECTED));
	}

	public void reconnect(IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		subMonitor.subTask(NLS.bind(Messages.CloudFoundryServerBehaviour_RECONNECTING_SERVER, getCloudFoundryServer()
				.getServer().getId()));

		disconnect(subMonitor.newChild(40));

		try {
			resetClient(subMonitor.newChild(20));
		}
		catch (CloudFoundryException cfe) {
			throw CloudErrorUtil.toCoreException(cfe);
		}

		connect(subMonitor.newChild(40));
	}

	@Override
	public void dispose() {
		super.dispose();
		getServer().removeServerListener(serverListener);
	}

	/**
	 * This method is API used by CloudFoundry Code.
	 */
	public CloudFoundryServer getCloudFoundryServer() throws CoreException {
		Server server = (Server) getServer();

		CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		if (cloudFoundryServer == null) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, "Fail to load server")); //$NON-NLS-1$
		}
		return cloudFoundryServer;
	}

	/**
	 * @deprecated use {@link #getCloudApplication(String, IProgressMonitor)}
	 * @param appName
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public CloudApplication getApplication(final String appName, IProgressMonitor monitor) throws CoreException {
		return getCloudApplication(appName, monitor);
	}

	/**
	 * Fetches an updated {@link CloudApplication} from the target Cloud space.
	 * 
	 * <p/>
	 * Note that his is a lower-level model of the application as presented by
	 * the underlying Cloud Java client and does not contain additional API used
	 * by the WST framework (for example, checking publish state or references
	 * to the module's workspace project and resources) or the Cloud tooling
	 * e.g. {@link DeploymentConfiguration}.
	 * 
	 * <p/>
	 * To obtain the application's associated module with the additional API,
	 * use {@link #updateCloudModule(String, IProgressMonitor)}
	 * @param appName
	 * @param monitor
	 * @return Cloud application. Not null.
	 * @throws CoreException if error occurs while resolving the Cloud
	 * application, or the application does not exist.
	 */
	public CloudApplication getCloudApplication(final String appName, IProgressMonitor monitor) throws CoreException {
		CloudApplication app = new BehaviourRequest<CloudApplication>(NLS.bind(
				Messages.CloudFoundryServerBehaviour_GET_APPLICATION, appName)) {
			@Override
			protected CloudApplication doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplication(appName);
			}
		}.run(monitor);

		return app;
	}

	/**
	 * Update the given module with application stats and instance information
	 * obtained from the Cloud space. Will only update the module if it is
	 * deployed.
	 * 
	 * @param appName
	 * @param monitor
	 * @return cloud module with updated instances for the give app name, or
	 * null if the app does not exist
	 * @throws CoreException
	 */
	public CloudFoundryApplicationModule updateCloudModuleWithInstances(IModule module, IProgressMonitor monitor)
			throws CoreException {
		CloudFoundryApplicationModule appModule = getCloudFoundryServer().getExistingCloudModule(module);
		String name = appModule != null ? appModule.getDeployedApplicationName() : null;
		if (name != null) {
			return updateCloudModuleWithInstances(name, monitor);
		}
		return null;
	}

	/**
	 * Updates the given module with an application request to the Cloud space.
	 * Returns null if the application no longer exists.
	 * @param module
	 * @param monitor
	 * @return Updated {@link CloudFoundryApplicationModule} or null if the
	 * application no longer exists in the Cloud Space
	 * @throws CoreException
	 */
	public CloudFoundryApplicationModule updateCloudModule(IModule module, IProgressMonitor monitor)
			throws CoreException {

		CloudFoundryApplicationModule appModule = getCloudFoundryServer().getExistingCloudModule(module);

		String name = appModule != null ? appModule.getDeployedApplicationName() : module.getName();
		return updateCloudModule(name, monitor);
	}

	/**
	 * Updates the given module with an application request to the Cloud space.
	 * Returns null if the application no longer exists.
	 * @param module
	 * @param monitor
	 * @return Updated {@link CloudFoundryApplicationModule} or null if the
	 * application no longer exists in the Cloud Space
	 * @throws CoreException if error occurs while resolving an updated
	 * {@link CloudApplication} from the Cloud space
	 */
	public CloudFoundryApplicationModule updateCloudModule(String appName, IProgressMonitor monitor)
			throws CoreException {
		CloudApplication updatedApp = null;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		subMonitor.subTask(NLS.bind(Messages.CloudFoundryServer_UPDATING_MODULE, appName, getServer().getId()));

		try {
			updatedApp = getCloudApplication(appName, subMonitor.newChild(50));
		}
		catch (CoreException e) {
			// Ignore if it is application not found error. If the application
			// does not exist
			// anymore, update the modules accordingly
			if (!CloudErrorUtil.isNotFoundException(e)) {
				throw e;
			}
		}
		return getCloudFoundryServer().updateModule(updatedApp, appName, subMonitor.newChild(50));
	}

	/**
	 * Update the given module with application stats and instance information
	 * obtained from the Cloud space.
	 * @param appName
	 * @param monitor
	 * @return cloud module with updated instances for the give app name, or
	 * null if the app does not exist
	 * @throws CoreException
	 */
	public CloudFoundryApplicationModule updateCloudModuleWithInstances(String appName, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		CloudFoundryApplicationModule appModule = updateCloudModule(appName, subMonitor.newChild(50));

		updateInstancesAndStats(appModule, subMonitor.newChild(50));

		return appModule;
	}

	/**
	 * Resets the module state of the module and all its children
	 */
	public void cleanModuleStates(IModule[] modules, IProgressMonitor monitor) {
		IServer iServer = this.getServer();
		if (iServer != null && iServer instanceof Server) {
			Server server = (Server) iServer;
			final ServerPublishInfo info = server.getServerPublishInfo();

			info.startCaching();
			info.clearCache();

			info.fill(modules);

			// The visit below will iterate through all children modules
			final List<IModule[]> modules2 = new ArrayList<IModule[]>();
			server.visit(new IModuleVisitor() {
				public boolean visit(IModule[] module) {
					info.fill(module);
					modules2.add(module);
					return true;
				}
			}, monitor);

			info.removeDeletedModulePublishInfo(server, modules2);

			info.save();
			super.setModulePublishState(modules, IServer.PUBLISH_STATE_NONE);
		}
	}

	/**
	 * Updates the instances for the given Cloud module. If the module is null,
	 * nothing will happen.
	 * @param appModule
	 * @param monitor
	 * @throws CoreException
	 */
	private void updateInstancesAndStats(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		// Module may have been deleted and application no longer exists.
		// Nothing to update
		if (appModule == null) {
			return;
		}
		try {
			ApplicationStats stats = getApplicationStats(appModule.getDeployedApplicationName(), monitor);
			InstancesInfo info = getInstancesInfo(appModule.getDeployedApplicationName(), monitor);
			appModule.setApplicationStats(stats);
			appModule.setInstancesInfo(info);
		}
		catch (CoreException e) {
			// Ignore if it is application not found error. If the application
			// does not exist
			// anymore, update the modules accordingly
			if (!CloudErrorUtil.isNotFoundException(e)) {
				throw e;
			}
		}
	}

	/**
	 * Fetches list of all applications in the Cloud space. No module updates
	 * occur, as this is a low-level API meant to interact with the underlying
	 * client directly. Callers should be responsible to update associated
	 * modules. Note that this may be a long-running operation. If fetching a
	 * known application , it is recommended to call
	 * {@link #getCloudApplication(String, IProgressMonitor)} or
	 * {@link #updateCloudModule(IModule, IProgressMonitor)} as it may be
	 * potentially faster
	 * @param monitor
	 * @return List of all applications in the Cloud space.
	 * @throws CoreException
	 */
	public List<CloudApplication> getApplications(IProgressMonitor monitor) throws CoreException {

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, getCloudFoundryServer()
				.getServer().getId());
		return new BehaviourRequest<List<CloudApplication>>(label) {
			@Override
			protected List<CloudApplication> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				return client.getApplications();
			}
		}.run(monitor);
	}

	public ApplicationStats getApplicationStats(final String applicationId, IProgressMonitor monitor)
			throws CoreException {
		return new StagingAwareRequest<ApplicationStats>(NLS.bind(Messages.CloudFoundryServerBehaviour_APP_STATS,
				applicationId)) {
			@Override
			protected ApplicationStats doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplicationStats(applicationId);
			}
		}.run(monitor);
	}

	public InstancesInfo getInstancesInfo(final String applicationId, IProgressMonitor monitor) throws CoreException {
		return new StagingAwareRequest<InstancesInfo>(NLS.bind(Messages.CloudFoundryServerBehaviour_APP_INFO,
				applicationId)) {
			@Override
			protected InstancesInfo doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplicationInstances(applicationId);
			}
		}.run(monitor);
	}

	public String getFile(final String applicationId, final int instanceIndex, final String path,
			IProgressMonitor monitor) throws CoreException {
		String label = NLS.bind(Messages.CloudFoundryServerBehaviour_FETCHING_FILE, path, applicationId);
		return new FileRequest<String>(label) {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, path);
			}
		}.run(monitor);
	}

	public String getFile(final String applicationId, final int instanceIndex, final String filePath,
			final int startPosition, IProgressMonitor monitor) throws CoreException {
		String label = NLS.bind(Messages.CloudFoundryServerBehaviour_FETCHING_FILE, filePath, applicationId);

		return new FileRequest<String>(label) {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(applicationId, instanceIndex, filePath, startPosition);
			}
		}.run(monitor);
	}

	public List<CloudServiceOffering> getServiceOfferings(IProgressMonitor monitor) throws CoreException {
		return new BehaviourRequest<List<CloudServiceOffering>>("Getting available service options") { //$NON-NLS-1$
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
		new BehaviourRequest<Object>("Deleting all applications") { //$NON-NLS-1$
			@Override
			protected Object doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteAllApplications();
				return null;
			}
		}.run(monitor);
	}

	public List<CloudService> getServices(IProgressMonitor monitor) throws CoreException {

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_SERVICES, getCloudFoundryServer()
				.getServer().getId());
		return new BehaviourRequest<List<CloudService>>(label) {
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
	 * @deprecated user {@link #getRefreshHandler()} instead
	 * @param monitor
	 */
	public void refreshModules(IProgressMonitor monitor) {
		getRefreshHandler().scheduleRefreshAll();
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
		cloudBehaviourOperations = null;
		refreshHandler = null;
	}

	@Override
	public void startModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		operations().applicationDeployment(modules, ApplicationAction.RESTART).run(monitor);
	}

	@Override
	public void stop(boolean force) {
		// This stops the server locally, it does NOT stop the remotely running
		// applications
		setServerState(IServer.STATE_STOPPED);

		try {
			ServerEventHandler.getDefault().fireServerEvent(
					new CloudServerEvent(getCloudFoundryServer(), CloudServerEvent.EVENT_SERVER_DISCONNECTED));
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	@Override
	public void stopModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		operations().applicationDeployment(modules, ApplicationAction.STOP).run(monitor);
	}

	/**
	 * @deprecated use {@link #operations()} instead
	 * @param modules
	 * @return
	 */
	public ICloudFoundryOperation getStopAppOperation(IModule[] modules) {
		try {
			return operations().applicationDeployment(modules, ApplicationAction.STOP);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	public StreamingLogToken addApplicationLogListener(final String appName, final ApplicationLogListener listener) {
		if (appName != null && listener != null) {
			try {
				return new BehaviourRequest<StreamingLogToken>("Adding application log listener") //$NON-NLS-1$
				{
					@Override
					protected StreamingLogToken doRun(CloudFoundryOperations client, SubMonitor progress)
							throws CoreException {
						return client.streamLogs(appName, listener);
					}

				}.run(new NullProgressMonitor());
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_APPLICATION_LOG_LISTENER, appName, e.getMessage()),
						e);
			}
		}

		return null;
	}

	public List<ApplicationLog> getRecentApplicationLogs(final String appName, IProgressMonitor monitor)
			throws CoreException {
		List<ApplicationLog> logs = null;
		if (appName != null) {
			logs = new BehaviourRequest<List<ApplicationLog>>("Getting existing application logs for: " + appName) //$NON-NLS-1$
			{

				@Override
				protected List<ApplicationLog> doRun(CloudFoundryOperations client, SubMonitor progress)
						throws CoreException {
					return client.getRecentLogs(appName);
				}

			}.run(monitor);
		}
		if (logs == null) {
			logs = Collections.emptyList();
		}
		return logs;
	}

	/**
	 * Note that this automatically restarts a module in the start mode it is
	 * currently, or was currently running in. It automatically detects if an
	 * application is running in debug mode or regular run mode, and restarts it
	 * in that same mode. Other API exists to restart an application in a
	 * specific mode, if automatic detection and restart in existing mode is not
	 * required.
	 */
	@Override
	public void restartModule(IModule[] modules, IProgressMonitor monitor) throws CoreException {
		operations().applicationDeployment(modules, ApplicationAction.RESTART).run(monitor);
	}

	/**
	 * Update restart republishes redeploys the application with changes. This
	 * is not the same as restarting an application which simply restarts the
	 * application in its current server version without receiving any local
	 * changes. It will only update restart an application in regular run mode.
	 * It does not support debug mode.Publishing of changes is done
	 * incrementally.
	 * @deprecated use {@link #operations()} instead
	 * @param module to update
	 * @throws CoreException
	 */
	public ICloudFoundryOperation getUpdateRestartOperation(IModule[] modules) throws CoreException {
		return operations().applicationDeployment(modules, ApplicationAction.UPDATE_RESTART);
	}

	/**
	 * This will restart an application in run mode. It does not restart an
	 * application in debug mode. Does not push application resources or create
	 * the application. The application must exist in the CloudFoundry server.
	 * @deprecated user {@link #operations()} instead
	 * @param modules
	 * @throws CoreException
	 */
	public ICloudFoundryOperation getRestartOperation(IModule[] modules) throws CoreException {
		return operations().applicationDeployment(modules, ApplicationAction.RESTART);
	}

	/**
	 * Updates an the number of application instances. Does not restart the
	 * application if the application is already running. The CF server does
	 * allow instance scaling to occur while the application is running.
	 * @deprecated Use {@link #operations()} instead.
	 * @param module representing the application. must not be null or empty
	 * @param instanceCount must be 1 or higher.
	 * @param monitor
	 * @throws CoreException if error occurred during or after instances are
	 * updated.
	 */
	public void updateApplicationInstances(final CloudFoundryApplicationModule module, final int instanceCount,
			IProgressMonitor monitor) throws CoreException {
		operations().instancesUpdate(module, instanceCount).run(monitor);
	}

	/**
	 * Updates an the number of application instances in the Cloud space, but
	 * does not update the associated application module. Does not restart the
	 * application if the application is already running. The CF server does
	 * allow instance scaling to occur while the application is running.
	 * @param module representing the application. must not be null or empty
	 * @param instanceCount must be 1 or higher.
	 * @param monitor
	 * @throws CoreException if error occurred during or after instances are
	 * updated.
	 */
	void updateApplicationInstances(final String appName, final int instanceCount, IProgressMonitor monitor)
			throws CoreException {
		new AppInStoppedStateAwareRequest<Void>("Updating application instances") { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationInstances(appName, instanceCount);
				return null;
			}
		}.run(monitor);

	}

	public void updatePassword(final String newPassword, IProgressMonitor monitor) throws CoreException {
		new BehaviourRequest<Void>("Updating password") { //$NON-NLS-1$

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
	 * @deprecated use {@link #operations()} instead
	 * @throws CoreException if error occurred during or after memory is scaled.
	 * Exception does not always mean that the memory changes did not take
	 * effect. Memory could have changed, but some post operation like
	 * refreshing may have failed.
	 */
	public void updateApplicationMemory(final CloudFoundryApplicationModule module, final int memory,
			IProgressMonitor monitor) throws CoreException {
		operations().memoryUpdate(module, memory).run(monitor);
	}

	/**
	 * @deprecated use {@link #operations()} instead
	 * @param appName
	 * @param uris
	 * @param monitor
	 * @throws CoreException
	 */
	public void updateApplicationUrls(final String appName, final List<String> uris, IProgressMonitor monitor)
			throws CoreException {
		operations().mappedUrlsUpdate(appName, uris).run(monitor);
	}

	/**
	 * deprecated Use {@link #operations()} instead.
	 */
	public void updateServices(String appName, List<String> services, IProgressMonitor monitor) throws CoreException {
		CloudFoundryApplicationModule appModule = getCloudFoundryServer().getExistingCloudModule(appName);
		operations().bindServices(appModule, services);
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

	public void register(final String email, final String password, IProgressMonitor monitor) throws CoreException {
		new BehaviourRequest<Void>("Registering account") { //$NON-NLS-1$
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

	boolean waitForStart(CloudFoundryOperations client, String deploymentId, IProgressMonitor monitor)
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

	@Override
	protected void initialize(IProgressMonitor monitor) {
		super.initialize(monitor);
		CloudRebelAppHandler.init();
		getServer().addServerListener(serverListener, ServerEvent.SERVER_CHANGE);

		try {
			getApplicationUrlLookup().refreshDomains(monitor);

			// Important: Must perform a refresh operation
			// as any operation that calls the CF client first
			// performs a server connection and sets server state.
			// The server connection is indirectly performed by this
			// first refresh call.
			getRefreshHandler().scheduleRefreshAll();

			ServerEventHandler.getDefault().fireServerEvent(
					new CloudServerEvent(getCloudFoundryServer(), CloudServerEvent.EVENT_SERVER_CONNECTED));
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	/**
	 * If found, will attempt to publish module with the given name, and it
	 * assumes it is being added for the first time. NOTE: This method is only
	 * intended to bypass the WST framework in cases not supported by WST (for
	 * example, drag/drop an application to a non-WST view or UI control).
	 * Otherwise, WST-based deployments of applications (e.g. Run on Server,
	 * drag/drop to Servers view) should rely on the framework to invoke the
	 * appropriate publish method in the behaviour.
	 * 
	 * @see #publishModule(int, int, IModule[], IProgressMonitor)
	 * @param moduleName
	 * @param monitor
	 * @return status of publish
	 */
	public IStatus publishAdd(String moduleName, IProgressMonitor monitor) {
		List<IModule[]> allModules = getAllModules();
		try {
			for (IModule[] module : allModules) {
				if (module[0].getName().equals(moduleName)) {
					operations().applicationDeployment(module, ApplicationAction.PUSH).run(monitor);
					return Status.OK_STATUS;
				}
			}
		}
		catch (CoreException ce) {
			handlePublishError(ce);
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	/**
	 * Judges whether there is a <code>CloudFoundryApplicationModule</code> with
	 * the given name in current server or not.
	 * 
	 * @param moduleName the module name to be checked
	 * @return true if there is a <code>CloudFoundryApplicationModule</code>
	 * with the given name in current server, false otherwise
	 */
	public boolean existCloudApplicationModule(String moduleName) {
		List<IModule[]> allModules = getAllModules();
		for (IModule[] modules : allModules) {
			if (modules[0] instanceof CloudFoundryApplicationModule && modules[0].getName().equals(moduleName)) {
				return true;
			}
		}
		return false;
	}

	protected void handlePublishError(CoreException e) {
		// Do not automatically delete apps on errors, even
		// if critical errors
		// as there may be features that may allow an app to
		// be redeployed without drag/drop (i.e. clicking
		// "Start").
		IStatus errorStatus = CloudFoundryPlugin.getErrorStatus(NLS.bind(Messages.ERROR_FAILED_TO_PUSH_APP,
				e.getMessage()));
		CloudFoundryPlugin.getCallback().handleError(errorStatus);
	}

	@Override
	protected void publishModules(int kind, List modules, List deltaKind2, MultiStatus multi, IProgressMonitor monitor) {
		// NOTE: this is a workaround to avoid server-wide publish when removing
		// a module (i.e., deleting an application) as
		// well as publishing
		// an application for the first time. The issue: If there
		// are other
		// modules aside from the module being added or removed, that also have
		// changes, those modules
		// will be republished. There is a WST preference (
		// ServerPreferences#setAutoPublishing) that prevent modules from being
		// published automatically on
		// add/delete, but since this is a global preference, and it
		// affects all WST server contributions, not just Cloud Foundry.
		// Therefore,
		// preventing server-wide publish for just Cloud Foundry servers by
		// setting this preference is not advisable. Until WST supports per-app
		// add/delete without triggering a server publish, this seems to be a
		// suitable
		// workaround.
		if (modules != null && deltaKind2 != null) {
			List<IModule[]> filteredModules = new ArrayList<IModule[]>(modules.size());
			List<Integer> filteredDeltaKinds = new ArrayList<Integer>(deltaKind2.size());

			// To prevent server-wide publish. Only filter in the following
			// modules:
			// 1. Those being added
			// 2. Those being deleted
			// If neither is present, it means modules only have CHANGE or
			// NOCHANGE delta kinds
			// which means the publish operation was probably requested through
			// an actual Server publish action. In this case,
			// no filter should occur
			for (int i = 0; i < modules.size() && i < deltaKind2.size(); i++) {

				if (monitor.isCanceled()) {
					return;
				}

				// should skip this publish
				IModule[] module = (IModule[]) modules.get(i);

				if (module.length == 0) {
					continue;
				}

				IModule m = module[module.length - 1];

				if (shouldIgnorePublishRequest(m)) {
					continue;
				}

				int knd = (Integer) deltaKind2.get(i);
				if (ServerBehaviourDelegate.ADDED == knd || ServerBehaviourDelegate.REMOVED == knd) {
					filteredModules.add(module);
					filteredDeltaKinds.add(knd);
				}

			}

			if (!filteredModules.isEmpty()) {
				modules = filteredModules;
				deltaKind2 = filteredDeltaKinds;
			}
		}

		super.publishModules(kind, modules, deltaKind2, multi, monitor);
	}

	@Override
	protected void publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {
		super.publishModule(kind, deltaKind, module, monitor);

		try {
			// If the delta indicates that the module has been removed, remove
			// it
			// from the server.
			// Note that although the "module" parameter is of IModule[] type,
			// documentation
			// (and the name of the parameter) indicates that it is always one
			// module
			if (deltaKind == REMOVED) {
				final CloudFoundryServer cloudServer = getCloudFoundryServer();
				final CloudFoundryApplicationModule cloudModule = cloudServer.getCloudModule(module[0]);
				if (cloudModule.getApplication() != null) {
					new BehaviourRequest<Void>(NLS.bind(Messages.DELETING_MODULE,
							cloudModule.getDeployedApplicationName())) {
						@Override
						protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
							client.deleteApplication(cloudModule.getDeployedApplicationName());
							return null;
						}
					}.run(monitor);
				}

			}
			else if (!module[0].isExternal()) {
				// These operations must ONLY be performed on NON-EXTERNAL
				// applications (apps with associated accessible workspace
				// projects).
				// Do not perform any updates or restarts on non-workspace
				// (external) apps, as some spaces may contain long-running
				// applications that
				// should not be restarted.
				int publishState = getServer().getModulePublishState(module);
				ICloudFoundryOperation op = null;
				if (deltaKind == ServerBehaviourDelegate.ADDED || publishState == IServer.PUBLISH_STATE_UNKNOWN) {
					// Application has not been published, so do a full
					// publish
					op = operations().applicationDeployment(module, ApplicationAction.PUSH);
				}
				else if (deltaKind == ServerBehaviourDelegate.CHANGED) {
					op = operations().applicationDeployment(module, ApplicationAction.UPDATE_RESTART);
				}
				// Republish the root module if any of the child module requires
				// republish
				else if (isChildModuleChanged(module, monitor)) {
					op = operations().applicationDeployment(module, ApplicationAction.UPDATE_RESTART);
				}

				// NOTE: No need to run this as a separate Job, as publish
				// operations
				// are already run in a PublishJob. To better integrate with
				// WST, ensure publish operation
				// is run to completion in the PublishJob, unless launching
				// asynch events to notify other components while the main
				// publish operation is being run (e.g refresh UI, etc..).
				if (op != null) {
					op.run(monitor);
				}
			}
		}
		catch (CoreException e) {
			handlePublishError(e);
			throw e;
		}
	}

	private boolean isChildModuleChanged(IModule[] module, IProgressMonitor monitor) {
		if (module == null || module.length == 0) {
			return false;
		}

		IServer myserver = this.getServer();
		IModule[] childModules = myserver.getChildModules(module, monitor);

		if (childModules != null && childModules.length > 0) {
			// Compose the full structure of the child module
			IModule[] currentChild = new IModule[module.length + 1];
			for (int i = 0; i < module.length; i++) {
				currentChild[i] = module[i];
			}
			for (IModule child : childModules) {
				currentChild[module.length] = child;

				if (myserver.getModulePublishState(currentChild) != IServer.PUBLISH_STATE_NONE
						|| isChildModuleChanged(currentChild, monitor)) {
					return true;
				}
			}
		}
		return false;
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
		if (appModule != null) {
			try {
				updateInstancesAndStats(appModule, monitor);
				ApplicationStats stats = appModule.getApplicationStats();
				InstancesInfo info = appModule.getInstancesInfo();
				return stats != null && info != null && appModule.isDeployed()
						&& isApplicationReady(appModule.getApplication());
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
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
		return new BehaviourRequest<CloudOrgsAndSpaces>("Getting orgs and spaces") { //$NON-NLS-1$

			@Override
			protected CloudOrgsAndSpaces doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return internalGetCloudSpaces(client);
			}

		}.run(monitor);
	}

	public List<CloudRoute> getRoutes(final String domainName, IProgressMonitor monitor) throws CoreException {

		List<CloudRoute> routes = new BehaviourRequest<List<CloudRoute>>(NLS.bind(Messages.ROUTES, domainName)) {
			@Override
			protected List<CloudRoute> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getRoutes(domainName);
			}
		}.run(monitor);

		return routes;
	}

	public void deleteRoute(final List<CloudRoute> routes, IProgressMonitor monitor) throws CoreException {

		if (routes == null || routes.isEmpty()) {
			return;
		}
		new BehaviourRequest<Void>("Deleting routes") { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				for (CloudRoute route : routes) {
					client.deleteRoute(route.getHost(), route.getDomain().getName());
				}
				return null;

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

		final CloudFoundryOperations operations = CloudFoundryServerBehaviour.createExternalClientLogin(url,
				credentials.getEmail(), credentials.getPassword(), selfSigned, monitor);

		return new ClientRequest<CloudOrgsAndSpaces>("Getting orgs and spaces") { //$NON-NLS-1$
			@Override
			protected CloudOrgsAndSpaces doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return internalGetCloudSpaces(client);
			}

			@Override
			protected CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
				return operations;
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

	public static void validate(final String location, String userName, String password, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException {
		createExternalClientLogin(location, userName, password, selfSigned, monitor);
	}

	public static CloudFoundryOperations createExternalClientLogin(final String location, String userName,
			String password, boolean selfSigned, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		try {
			final CloudFoundryOperations client = createClient(location, userName, password, selfSigned);

			new ClientRequest<Void>(Messages.VALIDATING_CREDENTIALS) {

				@Override
				protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
					CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);
					int attempts = 5;
					operationsHandler.login(progress, attempts, CloudOperationsConstants.LOGIN_INTERVAL);
					return null;
				}

				@Override
				protected CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
					return client;
				}

			}.run(monitor);
			return client;
		}
		catch (RuntimeException t) {
			throw CloudErrorUtil.checkServerCommunicationError(t);
		}
		finally {
			progress.done();
		}
	}

	public static void register(String location, String userName, String password, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Connecting", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		try {
			CloudFoundryOperations client = createClient(location, userName, password, selfSigned);
			client.register(userName, password);
		}
		catch (RestClientException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
		catch (RuntimeException e) {
			// try to guard against IOException in parsing response
			throw CloudErrorUtil.checkServerCommunicationError(e);

		}
		finally {
			progress.done();
		}
	}

	/**
	 * Resets publish state of the given modules to
	 * {@link IServer#PUBLISH_STATE_NONE}
	 * @param modules
	 */
	void resetPublishState(IModule[] modules) {
		setModulePublishState(modules, IServer.PUBLISH_STATE_NONE);
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
			password = ""; //$NON-NLS-1$
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
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
					"The server url " + serverURL + " is invalid: " + e.getMessage(), e)); //$NON-NLS-1$ //$NON-NLS-2$
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

	protected boolean hasChildModules(IModule[] modules) {
		IWebModule webModule = CloudUtil.getWebModule(modules);
		return webModule != null && webModule.getModules() != null && webModule.getModules().length > 0;
	}

	/**
	 * 
	 * @param descriptor that contains the application information, and that
	 * also will be updated with an archive containing the application resources
	 * to be deployed to the Cloud Foundry Server
	 * @param cloudModule the Cloud Foundry wrapper around the application
	 * module to be pushed to the server
	 * @param modules list of WTP modules.
	 * @param server where app should be pushed to
	 * @param
	 * @param monitor
	 * @throws CoreException if failure occurred while generated an archive file
	 * containing the application's payload
	 */
	protected ApplicationArchive generateApplicationArchiveFile(ApplicationDeploymentInfo deploymentInfo,
			CloudFoundryApplicationModule cloudModule, IModule[] modules, Server server, boolean incrementalPublish,
			IProgressMonitor monitor) throws CoreException {

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

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(cloudModule.getLocalModule());

		ApplicationArchive archive = null;
		if (delegate != null && delegate.providesApplicationArchive(cloudModule.getLocalModule())) {
			IModuleResource[] resources = getResources(modules);

			archive = getApplicationArchive(cloudModule, monitor, delegate, resources);
		}

		// If no application archive was provided,then attempt an incremental
		// publish. Incremental publish is only supported for apps without child
		// modules.
		if (archive == null && incrementalPublish && !hasChildModules(modules)) {
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
			IProgressMonitor monitor, AbstractApplicationDelegate delegate, IModuleResource[] resources)
			throws CoreException {
		return delegate.getApplicationArchive(cloudModule, getCloudFoundryServer(), resources, monitor);
	}

	/**
	 * Note that consoles may be mapped to an application's deployment name. If
	 * during deployment, the application name has changed, then this may result
	 * in two separate consoles.
	 * 
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
	protected void clearAndPrintlnConsole(CloudFoundryApplicationModule appModule, String message) throws CoreException {
		message += '\n';
		printToConsole(appModule, message, true, false);
	}

	protected void printlnToConsole(CloudFoundryApplicationModule appModule, String message) throws CoreException {
		message += '\n';
		printToConsole(appModule, message, false, false);
	}

	protected void printErrorlnToConsole(CloudFoundryApplicationModule appModule, String message) throws CoreException {
		message = NLS.bind(Messages.CONSOLE_ERROR_MESSAGE + '\n', message);
		printToConsole(appModule, message, false, true);
	}

	/**
	 * Note that consoles may be mapped to an application's deployment name. If
	 * during deployment, the application name has changed, then this may result
	 * in two separate consoles.
	 * 
	 */
	protected void printToConsole(CloudFoundryApplicationModule appModule, String message, boolean clearConsole,
			boolean isError) throws CoreException {
		CloudFoundryPlugin.getCallback().printToConsole(getCloudFoundryServer(), appModule, message, clearConsole,
				isError);
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

	abstract class FileRequest<T> extends StagingAwareRequest<T> {
		FileRequest(String label) {
			super(label);
		}
	}

	/**
	 * Keep track on all the publish operation to be completed
	 * <p/>
	 * NS: Keeping in case a similar job monitor is needed in the future.
	 * @author eyuen
	 */
	static class PublishJobMonitor extends JobChangeAdapter {

		private List<Job> jobLst = new ArrayList<Job>();

		void init() {
			// Clean all existing jobs
			synchronized (jobLst) {
				jobLst.clear();
			}
		}

		@Override
		public void done(IJobChangeEvent event) {
			super.done(event);
			synchronized (jobLst) {
				jobLst.remove(event.getJob());
			}
		}

		void monitorJob(Job curJob) {
			curJob.addJobChangeListener(this);
			synchronized (jobLst) {
				jobLst.add(curJob);
			}
		}

		boolean isAllJobCompleted() {
			return jobLst.size() == 0;
		}

		/**
		 * Wait for all job to be completed or the monitor is cancelled.
		 * @param monitor
		 */
		void waitForJobCompletion(IProgressMonitor monitor) {
			while ((monitor == null || !monitor.isCanceled()) && jobLst.size() > 0) {
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
					// Do nothing
				}
			}
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

	}

	BaseClientRequest<?> getUpdateApplicationMemoryRequest(final CloudFoundryApplicationModule appModule,
			final int memory) {
		return new AppInStoppedStateAwareRequest<Void>(NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_APP_MEMORY,
				appModule.getDeployedApplicationName())) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationMemory(appModule.getDeployedApplicationName(), memory);
				return null;
			}
		};
	}

	BaseClientRequest<?> getUpdateAppUrlsRequest(final String appName, final List<String> urls) {
		return new AppInStoppedStateAwareRequest<Void>(NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_APP_URLS,
				appName)) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				// Look up the existing urls locally first to avoid a client
				// call
				CloudFoundryApplicationModule existingAppModule = getCloudFoundryServer().getExistingCloudModule(
						appName);

				client.updateApplicationUris(appName, urls);
				if (existingAppModule != null) {
					List<String> oldUrls = existingAppModule.getDeploymentInfo() != null ? existingAppModule
							.getDeploymentInfo().getUris() : client.getApplication(appName).getUris();
					ServerEventHandler.getDefault().fireServerEvent(
							new AppUrlChangeEvent(getCloudFoundryServer(), CloudServerEvent.EVENT_APP_URL_CHANGED,
									existingAppModule.getLocalModule(), Status.OK_STATUS, oldUrls, urls));

				}
				return null;
			}
		};
	}

	BaseClientRequest<?> getUpdateServicesRequest(final String appName, final List<String> services) {
		return new StagingAwareRequest<Void>(NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_SERVICE_BINDING,
				appName)) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				client.updateApplicationServices(appName, services);
				return null;
			}
		};
	}

	protected BaseClientRequest<Void> getUpdateEnvVarRequest(final String appName,
			final List<EnvironmentVariable> variables) {
		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_ENV_VARS, appName);
		return new BehaviourRequest<Void>(label) {

			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				// Update environment variables.
				Map<String, String> varsMap = new HashMap<String, String>();

				if (variables != null) {
					for (EnvironmentVariable var : variables) {
						varsMap.put(var.getVariable(), var.getValue());
					}
				}

				client.updateApplicationEnv(appName, varsMap);

				return null;
			}

		};
	}

	BaseClientRequest<List<CloudService>> getDeleteServicesRequest(final List<String> services) {
		return new BehaviourRequest<List<CloudService>>(Messages.CloudFoundryServerBehaviour_DELETE_SERVICES) {
			@Override
			protected List<CloudService> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				SubMonitor serviceProgress = SubMonitor.convert(progress, services.size());

				for (String service : services) {
					serviceProgress.subTask(NLS.bind(Messages.CloudFoundryServerBehaviour_DELETING_SERVICE, service));
					client.deleteService(service);
					serviceProgress.worked(1);
				}
				return client.getServices();
			}
		};
	}

	BaseClientRequest<List<CloudService>> getCreateServicesRequest(final CloudService[] services) {
		return new BehaviourRequest<List<CloudService>>(Messages.CloudFoundryServerBehaviour_CREATE_SERVICES) {
			@Override
			protected List<CloudService> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				SubMonitor serviceProgress = SubMonitor.convert(progress, services.length);

				for (CloudService service : services) {
					serviceProgress.subTask(NLS.bind(Messages.CloudFoundryServerBehaviour_CREATING_SERVICE,
							service.getName()));
					client.createService(service);
					serviceProgress.worked(1);
				}
				return client.getServices();
			}
		};
	}
}
