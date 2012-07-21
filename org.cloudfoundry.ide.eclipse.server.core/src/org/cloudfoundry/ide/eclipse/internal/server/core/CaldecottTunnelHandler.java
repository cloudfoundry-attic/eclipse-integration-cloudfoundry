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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.caldecott.TunnelException;
import org.cloudfoundry.caldecott.client.HttpTunnelFactory;
import org.cloudfoundry.caldecott.client.TunnelFactory;
import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.caldecott.client.TunnelServer;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour.Request;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
 * 
 */
public class CaldecottTunnelHandler {

	public static final String LOCAL_HOST = "127.0.0.1";

	private final CloudFoundryServer cloudServer;

	public static final int BASE_PORT = 10100;

	public static final int MAX_PORT = 49150;

	public CaldecottTunnelHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	public boolean bindServiceToCaldecottApp(String serviceName, IProgressMonitor monitor) throws CoreException {
		CloudApplication caldecottApp = getCaldecottApp(monitor);
		List<String> updateCaldecottServices = new ArrayList<String>();
		List<String> existingServices = caldecottApp.getServices();
		if (existingServices != null) {
			updateCaldecottServices.addAll(existingServices);
		}

		IModule caldecottModule = getCaldecottModule(monitor);

		if (!updateCaldecottServices.contains(serviceName)) {
			updateCaldecottServices.add(serviceName);
			CloudFoundryServerBehaviour behaviour = cloudServer.getBehaviour();
			behaviour.stopModule(new IModule[] { caldecottModule }, monitor);
			behaviour.updateServices(TunnelHelper.getTunnelAppName(), updateCaldecottServices, monitor);

			setDeploymentServices(serviceName, monitor);

			return caldecottApp.getServices().contains(serviceName);
		}
		else {
			return true;
		}

	}

	public static boolean isCaldecottApp(String appName) {
		return TunnelHelper.getTunnelAppName().equals(appName);
	}

	protected void startCaldecottApp(IProgressMonitor progress, final CloudFoundryClient client) throws CoreException {

		CloudApplication caldecottApp = getCaldecottApp(progress);

		new StartApplicationInWaitOperation(cloudServer, "Starting Caldecott application").run(progress, client,
				caldecottApp);

	}

	protected String getTunnelUri(final CloudFoundryClient client, IProgressMonitor progress) throws CoreException {
		int ticks = 10;
		long sleep = 3000;

		progress.setTaskName("Getting tunnel URL.");
		String url = new WaitWithProgressJob<String>(ticks, sleep) {

			@Override
			protected String runInWait(IProgressMonitor monitor) throws CoreException {
				return TunnelHelper.getTunnelUri(client);
			}

		}.run(progress);

		return url;
	}

	public synchronized CaldecottTunnelDescriptor startCaldecottTunnel(final String serviceName,
			final IProgressMonitor monitor) throws CoreException {

		final List<CaldecottTunnelDescriptor> tunnel = new ArrayList<CaldecottTunnelDescriptor>(1);

		cloudServer.getBehaviour().new Request<CaldecottTunnelDescriptor>() {

			@Override
			protected CaldecottTunnelDescriptor doRun(final CloudFoundryClient client, SubMonitor progress)
					throws CoreException {
				progress = SubMonitor.convert(progress);

				CloudApplication caldecottApp = getOrDeployCaldecottApp(progress);

				if (caldecottApp == null) {
					return null;
				}

				progress.setTaskName("Binding " + serviceName + " to Caldecott.");

				bindServiceToCaldecottApp(serviceName, progress);

				// The application must be started before creating a tunnel
				startCaldecottApp(progress, client);

				CaldecottTunnelDescriptor oldDescriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
						cloudServer, serviceName);

				if (oldDescriptor != null) {
					try {
						stopAndDeleteCaldecottTunnel(serviceName, monitor);
					}
					catch (CoreException e) {
						CloudFoundryPlugin.logError(NLS.bind(
								"Failed to stop existing tunnel for service {0}. Unable to create new tunnel.",
								new Object[] { serviceName }));
						return null;
					}
				}

				String url = getTunnelUri(client, progress);
				Map<String, String> info = getTunnelInfo(client, serviceName, progress);
				if (info == null) {
					CloudFoundryPlugin.logError(NLS.bind("Failed to obtain tunnel information for {0}.",
							new Object[] { serviceName }));

					return null;
				}

				String host = info.get("hostname");
				int port = Integer.valueOf(info.get("port"));
				String auth = TunnelHelper.getTunnelAuth(client);
				String serviceUserName = info.get("username");
				String servicePassword = info.get("password");
				String dataBase = getServiceVendor(serviceName, progress);

				String name = info.get("vhost");
				if (name == null) {
					name = info.get("db") != null ? info.get("db") : info.get("name");
				}

				TunnelFactory tunnelFactory = new HttpTunnelFactory(url, host, port, auth);

				List<TunnelServer> tunnelServers = new ArrayList<TunnelServer>(1);
				int localPort = getTunnelServer(tunnelFactory, tunnelServers);

				if (tunnelServers.isEmpty() || localPort == -1) {
					CloudFoundryPlugin
							.logError(NLS
									.bind("Caldecott tunnel information obtained for {0}, but failed to create tunnel server for ports between: {1} and {2}",
											new Object[] { serviceName, new Integer(BASE_PORT), new Integer(MAX_PORT) }));
					return null;
				}

				TunnelServer tunnelServer = tunnelServers.get(0);

				progress.setTaskName("Starting tunnel for " + serviceName);

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
				callBack.displayCaldecottTunnelConnections(cloudServer, descriptors);

				return descriptor;
			}

		}.run(monitor);

		return tunnel.size() > 0 ? tunnel.get(0) : null;
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

		ApplicationModule appModule = getCaldecottModule(monitor);

		if (appModule != null) {

			DeploymentInfo deploymentInfo = appModule.getLastDeploymentInfo();
			// Do NOT set a deployment info if one does not exist, as another
			// component of CF integration does it, only
			// add to the existing deployment info.
			if (deploymentInfo != null) {
				List<String> existingServices = deploymentInfo.getServices();
				List<String> updatedServices = new ArrayList<String>();
				if (existingServices != null) {
					updatedServices.addAll(existingServices);
				}

				if (!updatedServices.contains(serviceName)) {
					updatedServices.add(serviceName);
					deploymentInfo.setServices(updatedServices);
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
					return service.getVendor();
				}
			}
		}
		return null;
	}

	public Map<String, String> getTunnelInfo(final CloudFoundryClient client, final String serviceName,
			IProgressMonitor monitor) throws CoreException {

		int ticks = 5;
		long sleepTime = 2000;
		monitor.setTaskName("Getting tunnel information for " + serviceName);

		Map<String, String> info = new WaitWithProgressJob<Map<String, String>>(ticks, sleepTime) {

			@Override
			protected Map<String, String> runInWait(IProgressMonitor monitor) {
				return TunnelHelper.getTunnelServiceInfo(client, serviceName);
			}
		}.run(monitor);

		if (info == null) {
			CloudFoundryPlugin.logError("Timeout trying to obtain tunnel information for: " + serviceName
					+ ". Please wait a few seconds before trying again.");
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

	public synchronized CloudApplication getCaldecottApp(IProgressMonitor monitor) throws CoreException {

		Request<CloudApplication> request = cloudServer.getBehaviour().new Request<CloudApplication>() {

			@Override
			protected CloudApplication doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				CloudApplication caldecottApp = null;
				try {
					caldecottApp = client.getApplication(TunnelHelper.getTunnelAppName());
				}
				catch (Throwable e) {
					throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
				}
				return caldecottApp;
			}
		};
		return request.run(monitor);
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
	public synchronized CloudApplication getOrDeployCaldecottApp(IProgressMonitor monitor) throws CoreException {
		Request<CloudApplication> request = cloudServer.getBehaviour().new Request<CloudApplication>() {

			@Override
			protected CloudApplication doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				CloudApplication caldecottApp = null;
				try {
					caldecottApp = getCaldecottApp(progress);
				}
				catch (Throwable e) {
					// Ignore all first attempt.
				}

				if (caldecottApp == null) {
					deployCaldecottApp(progress);
				}

				try {
					caldecottApp = client.getApplication(TunnelHelper.getTunnelAppName());
				}
				catch (Throwable e) {
					throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
				}
				return caldecottApp;
			}
		};
		return request.run(monitor);
	}

	protected void deployCaldecottApp(IProgressMonitor monitor) throws CoreException {

		Request<Boolean> request = cloudServer.getBehaviour().new Request<Boolean>() {

			@Override
			protected Boolean doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				progress.setTaskName("Deploying Caldecott application.");
				Thread t = Thread.currentThread();
				ClassLoader oldLoader = t.getContextClassLoader();
				boolean deployed = false;
				try {
					t.setContextClassLoader(CloudFoundryServerBehaviour.class.getClassLoader());
					TunnelHelper.deployTunnelApp(client);
					deployed = true;
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
					cloudServer.getBehaviour().refreshModules(progress);
				}
				return deployed;
			}
		};
		request.run(monitor);

	}

	public synchronized ApplicationModule getCaldecottModule(IProgressMonitor monitor) throws CoreException {
		return cloudServer.getApplicationModule(TunnelHelper.getTunnelAppName());
	}

}
