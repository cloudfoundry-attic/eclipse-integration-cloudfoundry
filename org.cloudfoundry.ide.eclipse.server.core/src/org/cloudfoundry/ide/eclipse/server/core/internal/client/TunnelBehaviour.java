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

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.caldecott.TunnelException;
import org.cloudfoundry.caldecott.client.HttpTunnelFactory;
import org.cloudfoundry.caldecott.client.TunnelFactory;
import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.caldecott.client.TunnelServer;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Primary handler for all Caldecott operations, like starting and stopping a
 * tunnel.
 * <p/>
 * If a Caldecott app is not yet deployed, this handler will automatically
 * deploy and start it.
 * <p/>
 * 
 * When creating a tunnel for a service that hasn't been bound, this handler
 * will automatically bind the service first to the Caldecott application before
 * attempting to create a tunnel.
 * <p/>
 * NOTE: CloudFoundryOperation calls should ALWAYS be done through a
 * org.cloudfoundry
 * .ide.eclipse.internal.server.core.CloudFoundryServerBehaviour.Request object,
 * as the Request wraps around all client calls. Only exception is if doing
 * standalone calls to a CF server, like validating credentials or getting a
 * list of organisations and spaces.
 * 
 */
public class TunnelBehaviour {

	public static final String LOCAL_HOST = "127.0.0.1"; //$NON-NLS-1$

	private final CloudFoundryServer cloudServer;

	public static final int BASE_PORT = 10100;

	public static final int MAX_PORT = 49150;

	public TunnelBehaviour(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	protected boolean bindServiceToCaldecottApp(String serviceName, CloudFoundryOperations client, SubMonitor monitor)
			throws CoreException {

		CloudApplication caldecottApp = getCaldecottApp(client);
		List<String> updateCaldecottServices = new ArrayList<String>();
		List<String> existingServices = caldecottApp.getServices();
		if (existingServices != null) {
			// Must iterate to filter out possible null service names
			for (String existing : existingServices) {
				if (existing != null) {
					updateCaldecottServices.add(existing);
				}
			}
		}

		IModule caldecottModule = getCaldecottModule(monitor.newChild(1));

		if (!updateCaldecottServices.contains(serviceName)) {
			monitor.setTaskName("Binding service " + serviceName + " to tunnel application"); //$NON-NLS-1$ //$NON-NLS-2$

			updateCaldecottServices.add(serviceName);
			CloudFoundryServerBehaviour behaviour = cloudServer.getBehaviour();
			behaviour.stopModule(new IModule[] { caldecottModule }, monitor.newChild(1));
			behaviour.updateServices(TunnelHelper.getTunnelAppName(), updateCaldecottServices, monitor.newChild(1));

			setDeploymentServices(serviceName, monitor.newChild(1));

			return caldecottApp.getServices().contains(serviceName);
		}
		else {
			return true;
		}

	}

	public static boolean isCaldecottApp(String appName) {
		return TunnelHelper.getTunnelAppName().equals(appName);
	}

	protected void startCaldecottApp(IProgressMonitor progress, final CloudFoundryOperations client)
			throws CoreException {
		progress.setTaskName("Starting tunnel application"); //$NON-NLS-1$
		CloudApplication caldecottApp = getCaldecottApp(client);

		if (caldecottApp == null) {
			throw CloudErrorUtil.toCoreException("No Caldecott application found. Unable to create tunnel."); //$NON-NLS-1$
		}
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(caldecottApp.getName());
		if (appModule == null) {
			throw CloudErrorUtil
					.toCoreException("No local Caldecott application module found. Application may not have finished deploying. Unable to create tunnel."); //$NON-NLS-1$
		}

		cloudServer.getBehaviour().startModule(new IModule[] { appModule.getLocalModule() }, progress);

		// Wait til application has started
		new WaitApplicationToStartOp(cloudServer, appModule).run(progress);

	}

	protected String getTunnelUri(final CloudFoundryOperations client, IProgressMonitor progress) throws CoreException {
		int attempts = 10;
		long sleep = 3000;

		progress.setTaskName("Getting tunnel URL"); //$NON-NLS-1$
		String url = new AbstractWaitWithProgressJob<String>(attempts, sleep) {

			@Override
			protected String runInWait(IProgressMonitor monitor) throws CoreException {
				if (client instanceof CloudFoundryClient) {
					return TunnelHelper.getTunnelUri((CloudFoundryClient) client);
				}
				return null;
			}

			protected boolean shouldRetryOnError(Throwable t) {
				// Try several times in case 404 errors are thrown
				return true;
			}

		}.run(progress);

		return url;
	}

	protected String getTunnelAuthorisation(CloudFoundryOperations operations) {
		if (operations instanceof CloudFoundryClient) {
			return TunnelHelper.getTunnelAuth((CloudFoundryClient) operations);
		}
		return null;
	}

	public synchronized CaldecottTunnelDescriptor startCaldecottTunnel(final String serviceName,
			IProgressMonitor monitor, final boolean shouldShowTunnelInformation) throws CoreException {

		final List<CaldecottTunnelDescriptor> tunnel = new ArrayList<CaldecottTunnelDescriptor>(1);

		new LocalServerRequest<CaldecottTunnelDescriptor>("Opening Tunnel") { //$NON-NLS-1$

			@Override
			protected CaldecottTunnelDescriptor doRun(final CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				int totalWorkTicks = 100;
				int worked = 10;

				progress = SubMonitor.convert(progress, totalWorkTicks);

				CloudApplication caldecottApp = getOrDeployCaldecottApp(getSubMonitor(worked, progress), client);

				if (caldecottApp == null) {
					return null;
				}

				bindServiceToCaldecottApp(serviceName, client, getSubMonitor(worked, progress));

				// The application must be started before creating a tunnel

				startCaldecottApp(getSubMonitor(worked, progress), client);

				CaldecottTunnelDescriptor oldDescriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
						cloudServer, serviceName);

				if (oldDescriptor != null) {
					try {
						progress.setTaskName("Stopping existing tunnel"); //$NON-NLS-1$
						stopAndDeleteCaldecottTunnel(serviceName, getSubMonitor(worked, progress));

					}
					catch (CoreException e) {
						CloudFoundryPlugin.logError("Failed to stop existing tunnel for service " + new Object[] { serviceName } + ". Unable to create new tunnel."); //$NON-NLS-1$ //$NON-NLS-2$
						return null;
					}
				}

				String url = getTunnelUri(client, getSubMonitor(worked, progress));

				Map<String, String> info = getTunnelInfo(client, serviceName, getSubMonitor(worked, progress));
				if (info == null) {
					CloudFoundryPlugin.logError("Failed to obtain tunnel information for " + new Object[] { serviceName }); //$NON-NLS-1$
					return null;
				}

				String host = info.get("hostname"); //$NON-NLS-1$
				int port = Integer.valueOf(info.get("port")); //$NON-NLS-1$
				String auth = getTunnelAuthorisation(client);
				String serviceUserName = info.get("username"); //$NON-NLS-1$
				String servicePassword = info.get("password"); //$NON-NLS-1$
				String dataBase = getServiceVendor(serviceName, getSubMonitor(worked, progress));

				String name = info.get("vhost"); //$NON-NLS-1$
				if (name == null) {
					name = info.get("db") != null ? info.get("db") : info.get("name"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

				// Use proxy settings if they exist

				HttpProxyConfiguration proxyConfiguration = null;

				try {
					URL urlOb = new URL(url);
					proxyConfiguration = CloudFoundryClientFactory.getProxy(urlOb);
				}
				catch (MalformedURLException e) {
					// Unable to handle proxy URL. Attempt to connect anyway.
				}

				TunnelFactory tunnelFactory = new HttpTunnelFactory(url, host, port, auth, proxyConfiguration);

				List<TunnelServer> tunnelServers = new ArrayList<TunnelServer>(1);
				int localPort = getTunnelServer(tunnelFactory, tunnelServers);

				if (tunnelServers.isEmpty() || localPort == -1) {
					CloudFoundryPlugin
							.logError("Tunnel information obtained for " + serviceName + //$NON-NLS-1$
									", but failed to create tunnel server for ports between: " + new Integer(BASE_PORT) + " and " + new Integer(MAX_PORT)); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}

				TunnelServer tunnelServer = tunnelServers.get(0);

				progress.setTaskName("Starting tunnel server"); //$NON-NLS-1$
				tunnelServer.start();

				CaldecottTunnelDescriptor descriptor = new CaldecottTunnelDescriptor(serviceUserName, servicePassword,
						name, serviceName, dataBase, tunnelServer, localPort);

				CloudFoundryPlugin.getCaldecottTunnelCache().addDescriptor(cloudServer, descriptor);
				tunnel.add(descriptor);

				CloudFoundryCallback callBack = CloudFoundryPlugin.getCallback();
				List<CaldecottTunnelDescriptor> descriptors = new ArrayList<CaldecottTunnelDescriptor>();
				descriptors.add(descriptor);

				// Update any UI that needs to be notified that a tunnel was
				// created
				if (shouldShowTunnelInformation) {
					callBack.displayCaldecottTunnelConnections(cloudServer, descriptors);
				}

				return descriptor;
			}

			@Override
			protected CloudFoundryServer getCloudServer() throws CoreException {
				return cloudServer;
			}

		}.run(monitor);

		return tunnel.size() > 0 ? tunnel.get(0) : null;
	}

	/**
	 * Updates the monitor for work done, and also checks if the monitor has
	 * been cancelled.
	 * @param workDoneInTicks
	 * @param monitor
	 * @throws OperationCanceledException if operation is cancelled.
	 */
	protected SubMonitor getSubMonitor(int workDoneInTicks, SubMonitor monitor) {
		if (!monitor.isCanceled()) {
			return monitor.newChild(workDoneInTicks);
		}
		else {
			throw new OperationCanceledException();
		}
	}

	/**
	 * 
	 * @param tunnelFactory
	 * @param server non null, where created tunnel will be stored.
	 * @return -1 if port failed to open
	 * @throws CoreException
	 */
	protected int getTunnelServer(TunnelFactory tunnelFactory, List<TunnelServer> tunnelServers) throws CoreException {

		RuntimeException se = null;

		int port = -1;
		TunnelServer tunnelServer = null;
		for (int i = BASE_PORT; i <= MAX_PORT; i++) {

			try {
				InetSocketAddress local = new InetSocketAddress(LOCAL_HOST, i);
				tunnelServer = new TunnelServer(local, tunnelFactory, getTunnelServerThreadExecutor());
				tunnelServers.add(tunnelServer);
				port = i;
				break;
			}
			catch (TunnelException e) {
				se = e;
			}
			catch (SecurityException e) {
				se = e;
			}

		}
		// Only rethrow security exception if all ports have failed
		if (tunnelServer == null && se != null) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(se));
		}
		return port;

	}

	/**
	 * Sets the new service in the existing deployment info for an existing
	 * Caldecott ApplicationModule, if and only if the application module
	 * already has a deployment info and does not yet contain the service.
	 * Returns true if the latter iff condition is met, false any other case.
	 * @param serviceName
	 * @param monitor
	 * @return
	 */
	protected boolean setDeploymentServices(String serviceName, IProgressMonitor monitor) throws CoreException {

		boolean serviceChanges = false;

		CloudFoundryApplicationModule appModule = getCaldecottModule(monitor);

		if (appModule != null) {

			DeploymentInfoWorkingCopy deploymentInfo = appModule.resolveDeploymentInfoWorkingCopy(monitor);

			if (deploymentInfo != null) {
				List<CloudService> existingServices = deploymentInfo.getServices();
				List<CloudService> updatedServices = new ArrayList<CloudService>();
				if (existingServices != null) {
					updatedServices.addAll(existingServices);
				}

				CloudService existingService = null;
				for (CloudService service : updatedServices) {
					if (service.getName().equals(serviceName)) {
						existingService = service;
						break;
					}
				}

				if (existingService == null) {
					updatedServices.add(new LocalCloudService(serviceName));
					deploymentInfo.setServices(updatedServices);
					deploymentInfo.save();
					serviceChanges = true;
				}
			}
		}

		return serviceChanges;
	}

	protected TaskExecutor getTunnelServerThreadExecutor() {
		int defaultPoolSize = 20;
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(defaultPoolSize);
		te.setMaxPoolSize(defaultPoolSize * 2);
		te.setQueueCapacity(100);
		return te;
	}

	protected String getServiceVendor(String serviceName, IProgressMonitor monitor) throws CoreException {
		List<CloudService> services = cloudServer.getBehaviour().getServices(monitor);
		if (services != null) {
			for (CloudService service : services) {
				if (serviceName.equals(service.getName())) {
					return service.getLabel();
				}
			}
		}
		return null;
	}

	protected Map<String, String> getTunnelInfo(final CloudFoundryOperations client, final String serviceName,
			IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName("Getting tunnel information"); //$NON-NLS-1$
		int attempts = 10;
		long sleepTime = 2000;

		Map<String, String> info = new AbstractWaitWithProgressJob<Map<String, String>>(attempts, sleepTime) {

			@Override
			protected Map<String, String> runInWait(IProgressMonitor monitor) {
				if (client instanceof CloudFoundryClient) {
					return TunnelHelper.getTunnelServiceInfo((CloudFoundryClient) client, serviceName);
				}
				return null;
			}

			@Override
			protected boolean shouldRetryOnError(Throwable t) {
				// Try several times in case 404 errors are thrown
				return true;
			}
		}.run(monitor);

		if (info == null) {
			CloudFoundryPlugin.logError("Timeout trying to obtain tunnel information for: " + serviceName //$NON-NLS-1$
					+ ". Please wait a few seconds before trying again."); //$NON-NLS-1$
		}

		return info;
	}

	public synchronized CaldecottTunnelDescriptor stopAndDeleteCaldecottTunnel(String serviceName,
			IProgressMonitor monitor) throws CoreException {

		CaldecottTunnelDescriptor tunnelDescriptor = stopCaldecottTunnel(serviceName);
		if (tunnelDescriptor != null) {
			CloudFoundryPlugin.getCaldecottTunnelCache().removeDescriptor(cloudServer,
					tunnelDescriptor.getServiceName());
		}
		return tunnelDescriptor;

	}

	/**
	 * Stops and deletes all Caldecott tunnels for the given server.
	 * @param monitor
	 * @throws CoreException
	 */
	public synchronized void stopAndDeleteAllTunnels(IProgressMonitor monitor) throws CoreException {
		Collection<CaldecottTunnelDescriptor> descriptors = CloudFoundryPlugin.getCaldecottTunnelCache()
				.getDescriptors(cloudServer);
		if (descriptors != null) {
			for (CaldecottTunnelDescriptor desc : descriptors) {
				stopAndDeleteCaldecottTunnel(desc.getServiceName(), monitor);
			}
		}
	}

	public synchronized CaldecottTunnelDescriptor stopCaldecottTunnel(String serviceName) throws CoreException {

		CaldecottTunnelDescriptor tunnelDescriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
				cloudServer, serviceName);
		if (tunnelDescriptor != null) {
			tunnelDescriptor.getTunnelServer().stop();
		}
		return tunnelDescriptor;
	}

	public synchronized boolean hasCaldecottTunnels() {
		Collection<CaldecottTunnelDescriptor> descriptors = CloudFoundryPlugin.getCaldecottTunnelCache()
				.getDescriptors(cloudServer);
		return descriptors != null && descriptors.size() > 0;
	}

	/**
	 * Returns an a tunnel descriptor if the service currently is connected via
	 * a tunnel, or null if no open tunnel exists
	 * @param serviceName
	 * @return
	 */
	public synchronized CaldecottTunnelDescriptor getCaldecottTunnel(String serviceName) {

		return CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(cloudServer, serviceName);
	}

	public synchronized boolean hasCaldecottTunnel(String serviceName) {
		return getCaldecottTunnel(serviceName) != null;
	}

	protected synchronized CloudApplication getCaldecottApp(CloudFoundryOperations client) throws CoreException {

		CloudApplication caldecottApp = null;
		try {
			caldecottApp = client.getApplication(TunnelHelper.getTunnelAppName());
		}
		catch (Throwable e) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
		}
		return caldecottApp;
	}

	/**
	 * Retrieves the actual Caldecott Cloud Application from the server. It does
	 * not rely on webtools IModule. May be a long running operation and
	 * experience network I/O timeouts. SHould only be called when other
	 * potential long running operations are performed.
	 * @param client
	 * @param monitor
	 * @return
	 */
	protected synchronized CloudApplication getOrDeployCaldecottApp(IProgressMonitor monitor,
			CloudFoundryOperations client) throws CoreException {
		monitor.setTaskName("Obtaining tunnel application"); //$NON-NLS-1$
		CloudApplication caldecottApp = null;
		try {
			caldecottApp = getCaldecottApp(client);
		}
		catch (Throwable e) {
			// Ignore all first attempt.
		}

		if (caldecottApp == null) {
			deployCaldecottApp(monitor, client);
		}

		try {
			caldecottApp = client.getApplication(TunnelHelper.getTunnelAppName());
		}
		catch (Throwable e) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
		}
		return caldecottApp;
	}

	protected void deployCaldecottApp(IProgressMonitor monitor, CloudFoundryOperations client) throws CoreException {
		monitor.setTaskName("Publishing tunnel application"); //$NON-NLS-1$
		Thread t = Thread.currentThread();
		ClassLoader oldLoader = t.getContextClassLoader();
		boolean deployed = false;
		try {
			t.setContextClassLoader(CloudFoundryServerBehaviour.class.getClassLoader());
			if (client instanceof CloudFoundryClient) {
				TunnelHelper.deployTunnelApp((CloudFoundryClient) client);
				deployed = true;
			}

		}
		catch (TunnelException te) {
			CloudFoundryPlugin.logError(te);
		}
		finally {
			t.setContextClassLoader(oldLoader);
		}

		// refresh the list of modules to create a module for the
		// deployed Caldecott App
		if (deployed) {
			cloudServer.getBehaviour().refreshModules(monitor);
		}

	}

	public synchronized CloudFoundryApplicationModule getCaldecottModule(IProgressMonitor monitor) throws CoreException {
		return cloudServer.getExistingCloudModule(TunnelHelper.getTunnelAppName());
	}
}
