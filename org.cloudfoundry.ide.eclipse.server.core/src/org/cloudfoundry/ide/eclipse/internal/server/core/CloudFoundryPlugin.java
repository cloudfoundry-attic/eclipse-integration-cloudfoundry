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

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelCache;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.PredefinedServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelServiceCommandStore;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.util.ClassUtils;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Terry Denney
 */
@SuppressWarnings("rawtypes")
public class CloudFoundryPlugin extends Plugin {

	private static final boolean UAA_AVAILABLE = ClassUtils.isPresent("org.springframework.uaa.client.UaaService",
			CloudFoundryPlugin.class.getClassLoader());

	private static class ExtensionPointReader {

		private static final String ELEMENT_CALLBACK = "callback";

		private static final String ELEMENT_CLASS = "class";

		private static final String EXTENSION_ID_CALLBACK = PLUGIN_ID + ".callback";

		public static CloudFoundryCallback readExtension() {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_ID_CALLBACK);
			IExtension[] extensions = extensionPoint.getExtensions();
			for (IExtension extension : extensions) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					if (element.getName().compareTo(ELEMENT_CALLBACK) == 0) {
						return readCallbackExtension(element);
					}
				}
			}
			return null;
		}

		private static CloudFoundryCallback readCallbackExtension(IConfigurationElement configurationElement) {
			try {
				Object object = configurationElement.createExecutableExtension(ELEMENT_CLASS);
				if (!(object instanceof CloudFoundryCallback)) {
					getDefault().getLog().log(
							new Status(IStatus.ERROR, PLUGIN_ID, "Could not load "
									+ object.getClass().getCanonicalName() + " must implement "
									+ CloudFoundryCallback.class.getCanonicalName()));
					return null;
				}

				return (CloudFoundryCallback) object;
			}
			catch (CoreException e) {
				getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, "Could not load callback extension", e));
			}
			return null;
		}

	}

	public static final boolean isUAAIDEAvailable() {

		boolean isAvailable = UAA_AVAILABLE;
		if (isAvailable) {
			Bundle bundle = Platform.getBundle(CloudFoundryClientFactory.SPRING_IDE_UAA_BUNDLE_SYMBOLIC_NAME);
			// If it is available in any state, assume it is available. Further
			// checks can be performed for bundle in RESOLVED, STARTING or
			// ACTIVE
			// state.
			isAvailable = bundle != null;
		}

		return isAvailable;
	}

	private static class NullCallback extends CloudFoundryCallback {

		@Override
		public void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
			// ignore
		}

		@Override
		public void applicationStopping(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
			// ignore
		}

		@Override
		public void disconnecting(CloudFoundryServer server) {
			// ignore
		}

		@Override
		public void applicationStopped(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {

		}

		@Override
		public void getCredentials(CloudFoundryServer server) {
			throw new OperationCanceledException();
		}

		@Override
		public DeploymentDescriptor prepareForDeployment(CloudFoundryServer server,
				CloudFoundryApplicationModule module, IProgressMonitor monitor) {
			throw new OperationCanceledException();
		}

		@Override
		public void deleteServices(List<String> services, CloudFoundryServer server) {
			// ignore
		}

		@Override
		public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
			// ignore
		}

		@Override
		public void displayCaldecottTunnelConnections(CloudFoundryServer cloudServer,
				List<CaldecottTunnelDescriptor> descriptor) {
			// ignore
		}

		@Override
		public void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
			// TODO Auto-generated method stub

		}

	}

	// public static final String CLOUD_CONTROLLER_DEFAULT_URL_ATTRIBUTE =
	// "cloudfoundry.cloudcontroller.url.default";

	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.core";

	public static final String ENABLE_INCREMENTAL_PUBLISH_PREFERENCE = PLUGIN_ID + ".publish.incremental.properties";

	public static final boolean DEFAULT_INCREMENTAL_PUBLISH_PREFERENCE_VAL = true;

	private static CloudFoundryCallback callback;

	private static ModuleCache moduleCache;

	private static CloudFoundryPlugin plugin;

	private static IProxyService proxyService;

	private DeployedResourceCache sha1Cache = new DeployedResourceCache();

	private InstanceScope INSTANCE_SCOPE = new InstanceScope();

	private static CaldecottTunnelCache caldecottCache = new CaldecottTunnelCache();

	private TunnelServiceCommandStore serviceCommandsStore;

	public static CaldecottTunnelCache getCaldecottTunnelCache() {
		return caldecottCache;
	}

	public static synchronized CloudFoundryCallback getCallback() {
		if (callback == null) {
			callback = ExtensionPointReader.readExtension();
			if (callback == null) {
				callback = new NullCallback();
			}
		}
		return callback;
	}

	public synchronized void setIncrementalPublish(boolean incrementalPublish) {
		IEclipsePreferences prefs = getPreferences();
		prefs.putBoolean(ENABLE_INCREMENTAL_PUBLISH_PREFERENCE, incrementalPublish);
		try {
			prefs.flush();
		}
		catch (BackingStoreException e) {
			logError(e);
		}
	}

	public synchronized boolean getIncrementalPublish() {
		return getPreferences().getBoolean(ENABLE_INCREMENTAL_PUBLISH_PREFERENCE,
				DEFAULT_INCREMENTAL_PUBLISH_PREFERENCE_VAL);
	}

	public IEclipsePreferences getPreferences() {
		return INSTANCE_SCOPE.getNode(PLUGIN_ID);
	}

	public synchronized DeployedResourceCache getDeployedResourcesCache() {
		return sha1Cache;
	}

	public static synchronized void setCallback(CloudFoundryCallback callback) {
		CloudFoundryPlugin.callback = callback;
	}

	public static CloudFoundryPlugin getDefault() {
		return plugin;
	}

	public static synchronized ModuleCache getModuleCache() {
		if (moduleCache == null) {
			moduleCache = new ModuleCache();
		}
		return moduleCache;
	}

	public synchronized TunnelServiceCommandStore getTunnelCommandsStore() {
		if (serviceCommandsStore == null) {
			serviceCommandsStore = new TunnelServiceCommandStore(new PredefinedServiceCommands());
		}
		return serviceCommandsStore;
	}

	private final List<CloudServerListener> applicationListeners = new CopyOnWriteArrayList<CloudServerListener>();

	private ServiceTracker tracker;

	private CloudFoundryClientFactory factory;

	public CloudFoundryPlugin() {
	}

	public void addServerListener(CloudServerListener listener) {
		applicationListeners.add(listener);
	}

	/**
	 * Returns a non-null client factory. A default factory is always used if a
	 * factory has not been defined
	 * @return non-null client factory
	 */
	public synchronized CloudFoundryClientFactory getCloudFoundryClientFactory() {
		if (factory == null) {
			factory = new CloudFoundryClientFactory();
		}
		return factory;
	}

	public synchronized void setCloudFoundryClientFactory(CloudFoundryClientFactory factory) {
		this.factory = factory;
	}

	private void fireServerEvent(CloudServerEvent event) {
		CloudServerListener[] listeners = applicationListeners.toArray(new CloudServerListener[0]);
		for (CloudServerListener listener : listeners) {
			listener.serverChanged(event);
		}
	}

	public void fireInstancesUpdated(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_INSTANCES));
	}

	public void fireServicesUpdated(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_SERVICES));
	}

	public void firePasswordUpdated(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_PASSWORD));
	}

	public void fireServerRefreshed(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server));
	}

	@SuppressWarnings("unchecked")
	public synchronized IProxyService getProxyService() {
		if (proxyService == null) {
			if (tracker == null) {
				tracker = new ServiceTracker(getBundle().getBundleContext(), IProxyService.class.getName(), null);
				tracker.open();
			}

			proxyService = (IProxyService) tracker.getService();
		}
		return proxyService;
	}

	public void removeServerListener(CloudServerListener listener) {
		applicationListeners.remove(listener);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns a non-null Cloud Foundry Java client which may be UAA aware if
	 * UAA support is available.
	 * @param userName
	 * @param password
	 * @param url
	 * @return non-null Cloud Foundry Java client, including a UAA aware client
	 * if UAA is available
	 */
	public CloudFoundryOperations getCloudFoundryClient(String userName, String password, URL url) {
		return getCloudFoundryClientFactory().getCloudFoundryOperations(isUAAIDEAvailable(), userName, password, url);
	}

	public CloudFoundryOperations getCloudFoundryClient(CloudCredentials credentials, URL url) {
		return getCloudFoundryClientFactory().getCloudFoundryOperations(isUAAIDEAvailable(), credentials, url);
	}

	public CloudFoundryOperations getCloudFoundryClient(CloudCredentials credentials, CloudSpace session, URL url) {
		return getCloudFoundryClientFactory().getCloudFoundryOperations(isUAAIDEAvailable(), credentials, url, session);
	}

	public static void trace(String string) {
		// System.err.println(string);
	}

	public static void logError(Throwable e) {
		logError(e.getLocalizedMessage());
	}

	public static void logError(String message) {
		logError(getErrorStatus(message));
	}

	public static void logError(String message, Throwable t) {
		logError(getErrorStatus(message, t));
	}

	public static IStatus getErrorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message, t);
	}

	public static IStatus getErrorStatus(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message);
	}

	public static IStatus getErrorStatus(Throwable t) {
		return new Status(IStatus.ERROR, PLUGIN_ID, t.getLocalizedMessage(), t);
	}

	public static void logError(IStatus status) {
		if (plugin != null) {
			plugin.getLog().log(status);
		}
	}

}
