/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
 *     IBM - add appStateTracker extension support
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryClientFactory;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentConfiguration;
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
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Terry Denney
 */
@SuppressWarnings("rawtypes")
public class CloudFoundryPlugin extends Plugin {

	// NOTE: Avoid adding API that accesses the plugin instance for CF
	// operations
	// (i.e. using CloudFoundryPlugin.getDefault()), as the plugin activator
	// may not always be available, in particular for background CF Eclipse jobs
	// that may
	// still be running while the workbench is shutting down. If adding API
	// to access the plugin instance (e.g. logging an error), always check
	// if the plugin activator is available (i.e not null)

	private static class ExtensionPointReader {

		private static final String ELEMENT_CALLBACK = "callback";

		private static final String ELEMENT_CLASS = "class";

		private static final String EXTENSION_ID_CALLBACK = PLUGIN_ID + ".callback";

		private static final String EXTENSION_ID_APP_STATE_TRACKER = "appStateTracker";

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

		/**
		 * Load the app state trackers.
		 */
		private static synchronized void readAppStateTrackerExtension() {
			if (appStateTrackerEntries != null)
				return;

			// if (Trace.CONFIG) {
			// Trace.trace(Trace.STRING_CONFIG,
			// "->- Loading .appStateTracker extension point ->-");
			// }
			appStateTrackerEntries = new ArrayList<AppStateTrackerEntry>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] cf = registry
					.getConfigurationElementsFor(PLUGIN_ID, EXTENSION_ID_APP_STATE_TRACKER);

			for (IConfigurationElement curConfigElement : cf) {
				// load the extension in a safe environment. If there are NPEs
				// or miss configurations they will be caught by try/catch
				String[] curServerTypeIds = tokenize(curConfigElement.getAttribute("serverTypeIds"), ",");
				String[] curModuleTypeIds = tokenize(curConfigElement.getAttribute("moduleTypeIds"), ",");
				appStateTrackerEntries.add(new AppStateTrackerEntry(curServerTypeIds, curModuleTypeIds,
						curConfigElement));
			}
			// if (Trace.CONFIG) {
			// Trace.trace(Trace.STRING_CONFIG,
			// "-<- Done loading .appStateTracker extension point -<-");
			// }
		}

		/**
		 * Utility method to tokenize a string into an array.
		 * 
		 * @param str a string to be parsed
		 * @param delim the delimiters
		 * @return an array containing the tokenized string
		 */
		private static String[] tokenize(String str, String delim) {
			if (str == null)
				return new String[0];

			List<String> list = new ArrayList<String>();

			StringTokenizer st = new StringTokenizer(str, delim);
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				if (s != null && s.length() > 0)
					list.add(s.trim());
			}

			String[] s = new String[list.size()];
			list.toArray(s);
			return s;
		}
	}

	private static class NullCallback extends CloudFoundryCallback {

		@Override
		public void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
			// ignore
		}

		@Override
		public void disconnecting(CloudFoundryServer server) {
			// ignore
		}

		@Override
		public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {

		}

		@Override
		public void getCredentials(CloudFoundryServer server) {
			throw new OperationCanceledException();
		}

		@Override
		public DeploymentConfiguration prepareForDeployment(CloudFoundryServer server,
				CloudFoundryApplicationModule module, IProgressMonitor monitor) throws CoreException {
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

	private static class AppStateTrackerEntry {
		private String[] serverTypeIds;

		private String[] moduleTypeIds;

		private IConfigurationElement configElement;

		private AppStateTrackerEntry(String[] curServerTypeIds, String[] curModuleTypeIds,
				IConfigurationElement curConfigElement) {
			serverTypeIds = curServerTypeIds;
			moduleTypeIds = curModuleTypeIds;
			configElement = curConfigElement;
		}

		private String[] getModuleTypeIds() {
			return moduleTypeIds;
		}

		private String[] getServerTypeIds() {
			return serverTypeIds;
		}

		private IConfigurationElement getConfigElement() {
			return configElement;
		}

		private AbstractAppStateTracker createAppStateTracker() {
			AbstractAppStateTracker tracker = null;
			try {
				tracker = (AbstractAppStateTracker) configElement.createExecutableExtension("class");
				// if (Trace.CONFIG) {
				// Trace.trace(Trace.STRING_CONFIG,
				// "  Loaded .appStateTracker: " +
				// curConfigElement.getAttribute("id")
				// + ", loaded class=" + tracker);
				// }
			}
			catch (Throwable t) {
				// if (Trace.SEVERE) {
				// Trace.trace(Trace.STRING_SEVERE,
				// "  Could not load .appStateTracker: " +
				// curConfigElement.getAttribute("id"), t);
				// }
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_FAILED_APP_START_TRACKER, t.getMessage()), t);
			}
			return tracker;
		}
	}

	// public static final String CLOUD_CONTROLLER_DEFAULT_URL_ATTRIBUTE =
	// "cloudfoundry.cloudcontroller.url.default";

	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.core";

	public static final String ENABLE_INCREMENTAL_PUBLISH_PREFERENCE = PLUGIN_ID + ".publish.incremental.properties";

	public static final boolean DEFAULT_INCREMENTAL_PUBLISH_PREFERENCE_VAL = true;

	private static CloudFoundryCallback callback;

	// Cached copy of app state tracker
	private static List<AppStateTrackerEntry> appStateTrackerEntries;

	private static ModuleCache moduleCache;

	private static CloudFoundryPlugin plugin;

	private static IProxyService proxyService;

	private DeployedResourceCache sha1Cache = new DeployedResourceCache();

	private InstanceScope INSTANCE_SCOPE = new InstanceScope();

	private static CaldecottTunnelCache caldecottCache = new CaldecottTunnelCache();

	private TunnelServiceCommandStore serviceCommandsStore;

	/**
	 * Returns the app state tracker for a given server type. Only the first
	 * matched tracker will be return in case there are multiple tracker defined
	 * to support a particular server type.
	 *
	 * @return the app state tracker or null if no app state tracker has been
	 * registered to support the given type.
	 */
	public static AbstractAppStateTracker getAppStateTracker(String serverTypeId,
			CloudFoundryApplicationModule application) {
		if (serverTypeId == null || application == null || application.getLocalModule() == null) {
			return null;
		}

		if (appStateTrackerEntries == null) {
			ExtensionPointReader.readAppStateTrackerExtension();
		}

		String moduleTypeId = application.getLocalModule().getModuleType().getId();

		for (AppStateTrackerEntry curTrackerEntry : appStateTrackerEntries) {
			String[] curServerTypeIds = curTrackerEntry.getServerTypeIds();
			if (supportsType(serverTypeId, curServerTypeIds)) {
				String[] curModuleTypeIds = curTrackerEntry.getModuleTypeIds();
				if (supportsType(moduleTypeId, curModuleTypeIds)) {
					return curTrackerEntry.createAppStateTracker();
				}
			}
		}

		return null;
	}

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

	private ServiceTracker tracker;

	private static CloudFoundryClientFactory factory;

	public CloudFoundryPlugin() {
	}

	/**
	 * Returns a non-null client factory. A default factory is always used if a
	 * factory has not been defined
	 * @return non-null client factory
	 */
	public static synchronized CloudFoundryClientFactory getCloudFoundryClientFactory() {
		if (factory == null) {
			factory = new CloudFoundryClientFactory();
		}
		return factory;
	}

	public static synchronized void setCloudFoundryClientFactory(CloudFoundryClientFactory factory) {
		CloudFoundryPlugin.factory = factory;
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

		if (moduleCache != null) {
			moduleCache.dispose();
			moduleCache = null;
		}

		plugin = null;
		super.stop(context);
	}

	private static boolean supportsType(String typeId, String[] supportedTypeIds) {
		if (typeId == null || typeId.length() == 0)
			return false;

		if (supportedTypeIds == null)
			return false;

		int size = supportedTypeIds.length;
		for (int i = 0; i < size; i++) {
			if (supportedTypeIds[i].endsWith("*")) {
				if (typeId.length() >= supportedTypeIds[i].length()
						&& typeId.startsWith(supportedTypeIds[i].substring(0, supportedTypeIds[i].length() - 1)))
					return true;
			}
			else if (typeId.equals(supportedTypeIds[i]))
				return true;
		}
		return false;
	}

	public static void trace(String string) {
		// System.err.println(string);
	}

	public static void log(CoreException ce) {
		log(ce.getStatus());
	}

	public static void logError(Throwable e) {
		log(getErrorStatus(e));
	}

	public static void logError(String message) {
		log(getErrorStatus(message));
	}

	public static void logError(String message, Throwable t) {
		log(getErrorStatus(message, t));
	}

	public static IStatus getErrorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message, t);
	}

	public static IStatus getErrorStatus(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message);
	}

	public static IStatus getStatus(String message, int type) {
		return new Status(type, PLUGIN_ID, message);
	}

	public static IStatus getErrorStatus(Throwable t) {
		return new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t);
	}

	public static void log(IStatus status) {
		if (plugin != null) {
			plugin.getLog().log(status);
		}
	}

	public static void logWarning(String message) {
		if (plugin != null && message != null) {
			plugin.getLog().log(getStatus(message, IStatus.WARNING));
		}
	}

}
