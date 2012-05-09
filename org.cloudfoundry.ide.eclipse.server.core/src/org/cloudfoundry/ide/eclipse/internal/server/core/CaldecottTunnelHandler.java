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
import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.caldecott.client.TunnelServer;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour.Request;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

public class CaldecottTunnelHandler {

	public static final String LOCAL_HOST = "127.0.0.1";

	private final CloudFoundryServer cloudServer;

	public CaldecottTunnelHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	public boolean bindServiceToCaldecottApp(CloudApplication caldecottApp, String serviceName, IProgressMonitor monitor)
			throws CoreException {

		List<String> updateCaldecottServices = new ArrayList<String>();
		List<String> existingServices = caldecottApp.getServices();
		if (existingServices != null) {
			updateCaldecottServices.addAll(existingServices);
		}

		IModule caldecottModule = getCaldecottModule();

		if (!updateCaldecottServices.contains(serviceName)) {
			updateCaldecottServices.add(serviceName);
			CloudFoundryServerBehaviour behaviour = cloudServer.getBehaviour();
			behaviour.stopModule(new IModule[] { caldecottModule }, monitor);
			behaviour.updateServices(TunnelHelper.getTunnelAppName(), updateCaldecottServices, monitor);
			behaviour.startModule(new IModule[] { caldecottModule }, monitor);

			return caldecottApp.getServices().contains(serviceName);
		}
		else {
			return true;
		}

	}

	public synchronized CaldecottTunnelDescriptor startCaldecottTunnel(final String serviceName,
			final IProgressMonitor monitor) throws CoreException {

		final List<CaldecottTunnelDescriptor> tunnel = new ArrayList<CaldecottTunnelDescriptor>(1);

		cloudServer.getBehaviour().new Request<CaldecottTunnelDescriptor>() {

			@Override
			protected CaldecottTunnelDescriptor doRun(CloudFoundryClient client, SubMonitor progress)
					throws CoreException {
				CloudApplication caldecottApp = getCaldecottApp(monitor);
				if (caldecottApp == null) {
					return null;
				}

				bindServiceToCaldecottApp(caldecottApp, serviceName, monitor);

				// First get an unused port, even if there may be an
				// existing tunnel, as deleting an existing tunnel
				// right away
				// may not necessarily free the port immediately on
				// the server side.
				int unusedPort = CloudFoundryPlugin.getCaldecottTunnelCache().getUnusedPort();

				CaldecottTunnelDescriptor oldDescriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
						cloudServer, serviceName);

				if (oldDescriptor != null) {
					try {
						stopCaldecottTunnel(serviceName);
					}
					catch (CoreException e) {
						CloudFoundryPlugin
								.logError(NLS
										.bind("Failed to stop existing tunnel for the following service {0} on port {1}. Attempting to create a new tunnel on a different port {2}.",
												new Object[] { serviceName, oldDescriptor.tunnelPort(), unusedPort }));
					}
				}

				InetSocketAddress local = new InetSocketAddress(LOCAL_HOST, unusedPort);
				String url = TunnelHelper.getTunnelUri(client);
				Map<String, String> info = TunnelHelper.getTunnelServiceInfo(client, serviceName);
				String host = info.get("hostname");
				int port = Integer.valueOf(info.get("port"));
				String auth = TunnelHelper.getTunnelAuth(client);
				String serviceUserName = info.get("username");
				String servicePassword = info.get("password");

				TunnelServer tunnelServer = new TunnelServer(local, new HttpTunnelFactory(url, host, port, auth));
				tunnelServer.start();

				// Delete the old tunnel
				if (oldDescriptor != null) {
					CloudFoundryPlugin.getCaldecottTunnelCache().removeDescriptor(cloudServer, serviceName);
				}

				CaldecottTunnelDescriptor descriptor = new CaldecottTunnelDescriptor(serviceUserName, servicePassword,
						serviceName, tunnelServer, unusedPort);

				CloudFoundryPlugin.getCaldecottTunnelCache().addDescriptor(cloudServer, descriptor);
				tunnel.add(descriptor);

				CloudFoundryCallback callBack = CloudFoundryPlugin.getCallback();
				callBack.displayCaldecottTunnelConnections(cloudServer);
				return descriptor;
			}

		}.run(monitor);

		return tunnel.size() > 0 ? tunnel.get(0) : null;
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

	public synchronized CaldecottTunnelDescriptor stopCaldecottTunnel(String serviceName) throws CoreException {

		CaldecottTunnelDescriptor tunnelDescriptor = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptor(
				cloudServer, serviceName);
		if (tunnelDescriptor != null) {
			tunnelDescriptor.getTunnelServer().stop();

		}
		return tunnelDescriptor;
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
	public synchronized CloudApplication getCaldecottApp(IProgressMonitor monitor) throws CoreException {

		Request<CloudApplication> request = cloudServer.getBehaviour().new Request<CloudApplication>() {

			@Override
			protected CloudApplication doRun(CloudFoundryClient client, SubMonitor progress) throws CoreException {
				CloudApplication caldecottApp = null;
				try {

					try {
						caldecottApp = client.getApplication(TunnelHelper.getTunnelAppName());
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
					catch (Exception e) {
						throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
					}

					if (caldecottApp != null && !caldecottApp.getState().equals(CloudApplication.AppState.STARTED)) {
						client.startApplication(caldecottApp.getName());
					}

				}
				catch (CoreException ce) {
					CloudFoundryPlugin.logError("Failed to deploy Caldecott app. Unable to create service tunnel.", ce);
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
				Thread t = Thread.currentThread();
				ClassLoader oldLoader = t.getContextClassLoader();
				try {
					t.setContextClassLoader(CloudFoundryServerBehaviour.class.getClassLoader());
					TunnelHelper.deployTunnelApp(client);
					return true;
				}
				catch (TunnelException te) {
					CloudFoundryPlugin.logError(te);
				}
				finally {
					t.setContextClassLoader(oldLoader);
				}
				return false;
			}
		};
		request.run(monitor);

	}

	public synchronized IModule getCaldecottModule() {
		Collection<ApplicationModule> modules = cloudServer.getApplications();
		if (modules != null) {
			String caldecottAppName = TunnelHelper.getTunnelAppName();
			for (ApplicationModule module : modules) {
				if (caldecottAppName.equals(module.getApplicationId())) {
					return module;
				}
			}
		}
		return null;
	}

}
