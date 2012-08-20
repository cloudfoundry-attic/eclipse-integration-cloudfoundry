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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudApplication;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Manages the cloud state of the modules. This can not be managed in the server
 * or behavior delegate since those get disposed every time a working copy is
 * saved.
 * @author Steffen Pingel
 */
public class ModuleCache {

	public static class ServerData {

		private final List<ApplicationModule> applications = new ArrayList<ApplicationModule>();

		/** Cached password in case secure store fails. */
		private String password;

		private IServer server;

		/**
		 * Modules added in this session.
		 */
		private final List<IModule> undeployedModules = new ArrayList<IModule>();

		private final Map<String, RepublishModule> automaticRepublishModules = new HashMap<String, RepublishModule>();

		private int[] applicationMemoryChoices;

		ServerData(IServer server) {
			this.server = server;
		}

		public synchronized void clear() {
			applications.clear();
		}

		public synchronized ApplicationModule createModule(CloudApplication application) {
			ApplicationModule appModule = new ApplicationModule(null, application.getName(), server);
			appModule.setCloudApplication(application);
			add(appModule);
			return appModule;
		}

		public synchronized void updateModule(ApplicationModule module) {
			Map<String, String> mapping = getModuleIdToApplicationId();
			if (module.getLocalModule() != null) {
				mapping.put(module.getLocalModule().getId(), module.getApplicationId());
				setMapping(mapping);
			}
		}

		public synchronized Collection<ApplicationModule> getApplications() {
			return new ArrayList<ApplicationModule>(applications);
		}

		public synchronized String getPassword() {
			return password;
		}

		public synchronized boolean isUndeployed(IModule module) {
			return undeployedModules.contains(module);
		}

		public synchronized void remove(ApplicationModule module) {
			applications.remove(module);
			Map<String, String> mapping = getModuleIdToApplicationId();
			if (module.getLocalModule() != null) {
				mapping.remove(module.getLocalModule().getId());
				setMapping(mapping);
			}
		}

		public synchronized void removeObsoleteModules(Set<ApplicationModule> allModules) {
			HashSet<ApplicationModule> deletedModules = new HashSet<ApplicationModule>(applications);
			deletedModules.removeAll(allModules);
			if (deletedModules.size() > 0) {
				Map<String, String> mapping = getModuleIdToApplicationId();
				boolean mappingModified = false;
				for (ApplicationModule deletedModule : deletedModules) {
					if (deletedModule.getLocalModule() != null) {
						mappingModified |= mapping.remove(deletedModule.getLocalModule().getId()) != null;
					}
				}
				if (mappingModified) {
					setMapping(mapping);
				}
			}
		}

		public synchronized void setPassword(String password) {
			this.password = password;
		}

		public synchronized void tagAsDeployed(IModule module) {
			undeployedModules.remove(module);
		}

		public synchronized void tagAsUndeployed(IModule module) {
			undeployedModules.add(module);
		}

		public synchronized void tagForAutomaticRepublish(RepublishModule module) {
			automaticRepublishModules.put(module.getModule().getName(), module);
		}

		public synchronized RepublishModule untagForAutomaticRepublish(IModule module) {
			return automaticRepublishModules.remove(module.getName());
		}

		private void add(ApplicationModule module) {
			applications.add(module);
		}

		private String convertMapToString(Map<String, String> map) {
			if (map == null) {
				return "";
			}
			StringBuilder result = new StringBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				result.append(entry.getKey());
				result.append(",");
				result.append(entry.getValue());
				result.append(",");
			}
			return result.toString();
		}

		private Map<String, String> convertStringToMap(String str) {
			if (str == null) {
				return new HashMap<String, String>();
			}
			Map<String, String> result = new HashMap<String, String>();
			String[] tokens = str.split(",");
			for (int i = 0; i < tokens.length - 1; i += 2) {
				result.put(tokens[i], tokens[i + 1]);
			}
			return result;
		}

		private Map<String, String> getModuleIdToApplicationId() {
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			String string = node.get(KEY_MODULE_MAPPING_LIST + ":" + getServerId(), "");
			return convertStringToMap(string);
		}

		private ApplicationModule getModuleByApplicationId(String applicationId) {
			for (ApplicationModule module : applications) {
				if (applicationId.equals(module.getApplicationId())) {
					return module;
				}
			}
			return null;
		}

		private ApplicationModule getModuleByModuleName(String moduleName) {
			for (ApplicationModule module : applications) {
				if (moduleName.equals(module.getName())) {
					return module;
				}
			}
			return null;
		}

		private String getServerId() {
			return server.getAttribute(CloudFoundryServer.PROP_SERVER_ID, (String) null);
		}

		private void setMapping(Map<String, String> list) {
			String string = convertMapToString(list);
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			CloudFoundryPlugin.trace("Updated mapping: " + string);
			node.put(KEY_MODULE_MAPPING_LIST + ":" + getServerId(), string);
			try {
				node.flush();
			}
			catch (BackingStoreException e) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Failed to update application mappings", e));
			}
		}

		synchronized ApplicationModule getOrCreateApplicationModule(IModule module) {
			ApplicationModule appModule = getModuleByModuleName(module.getName());
			if (appModule != null) {
				return appModule;
			}

			// lookup mapping for module
			String applicationId = getModuleIdToApplicationId().get(module.getId());
			if (applicationId != null) {
				appModule = getModuleByApplicationId(applicationId);
				if (appModule != null) {
					return appModule;
				}
			}
			else {
				// assume that application ID and module name match
				applicationId = module.getName();
			}

			// no mapping found, create new module
			appModule = new ApplicationModule(module, module.getName(), server);
			appModule.setApplicationId(applicationId);
			add(appModule);
			return appModule;
		}

		void updateServerId(String oldServerId, String newServerId) {
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			String string = node.get(KEY_MODULE_MAPPING_LIST + ":" + oldServerId, "");
			node.remove(KEY_MODULE_MAPPING_LIST + ":" + oldServerId);
			node.put(KEY_MODULE_MAPPING_LIST + ":" + newServerId, string);
		}

		public synchronized void setApplicationMemoryChoices(int[] applicationMemoryChoices) {
			this.applicationMemoryChoices = applicationMemoryChoices;
		}

		public synchronized int[] getApplicationMemoryChoices() {
			return applicationMemoryChoices;
		}
	}

	/**
	 * List of appName, module id pairs.
	 */
	static final String KEY_MODULE_MAPPING_LIST = "org.cloudfoundry.ide.eclipse.moduleMapping";

	private Map<IServer, ServerData> dataByServer;

	private IServerLifecycleListener listener = new IServerLifecycleListener() {

		public void serverAdded(IServer server) {
			// ignore
		}

		public void serverChanged(IServer server) {
			// ignore

		}

		public void serverRemoved(IServer server) {
			remove(server);
		}
	};

	public ModuleCache() {
		dataByServer = new HashMap<IServer, ServerData>();
		ServerCore.addServerLifecycleListener(listener);
	}

	public void dispose() {
		ServerCore.removeServerLifecycleListener(listener);
	}

	public synchronized ServerData getData(IServer server) {
		ServerData data = dataByServer.get(server);
		if (data == null) {
			data = new ServerData(server);
			dataByServer.put(server, data);
		}
		return data;
	}

	protected synchronized void remove(IServer server) {
		dataByServer.remove(server);

		String serverId = server.getAttribute(CloudFoundryServer.PROP_SERVER_ID, (String) null);
		if (serverId != null) {
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			node.remove(KEY_MODULE_MAPPING_LIST + ":" + serverId);
			try {
				node.flush();
			}
			catch (BackingStoreException e) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Failed to remove application mappings", e));
			}
		}
	}

}
