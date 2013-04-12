/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IModule;

/**
 * Given WST application module, this registry determines what frameworks and
 * runtimes are applicable to that module, and also provides the related
 * application delegate for that module, which is responsible for providing
 * additional information, for example, whether that application requires URLs,
 * and if that application has its own mechanism to generate an archive used by
 * the Cloud Foundry plugin framework to push the application's resources to a
 * Cloud Foundry server.
 * 
 */
public class ApplicationRegistry {

	public enum Priority {
		High, Medium, Low
	}

	private static Map<Priority, List<ApplicationProvider>> delegates;

	private static final String APPLICATION_DELEGATE_EXT_ELEMENT = "applicationDelegate";

	public static String EXTENSION_POINT = "org.cloudfoundry.ide.eclipse.server.core.application";

	public static ApplicationFramework getApplicationFramework(IModule module) {
		IApplicationDelegate delegate = getApplicationDelegate(module, null);
		if (delegate != null) {
			try {
				return delegate.getFramework(module);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(
						"Failed to load Cloud Foundry application framework for " + module.getName()
								+ " of module type: " + module.getModuleType().getId(), e);
			}
		}
		return null;
	}

	public static IApplicationDelegate getApplicationDelegate(IModule module, String framework) {
		ApplicationProvider provider = getApplicationProvider(module);
		if (provider != null) {
			IApplicationDelegate delegate = provider.getDelegate();

			try {
				if ((module != null && delegate.getFramework(module) != null)
						|| (framework != null && delegate.isSupportedFramework(framework))) {
					return delegate;
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError("Failed to load Cloud Foundry application delegate for " + module.getName()
						+ " of module type: " + module.getModuleType().getId(), e);
			}
		}

		return null;
	}

	public static ApplicationProvider getApplicationProvider(IModule module) {

		if (delegates == null) {
			delegates = load();
		}

		ApplicationProvider provider = null;

		for (Priority priority : Priority.values()) {

			List<ApplicationProvider> providerList = delegates.get(priority);
			if (providerList != null) {
				for (ApplicationProvider prv : providerList) {
					// First do a check based on static extension point
					// information about the
					// provider based
					// on the application ID that the provider supports.

					if (supportsModule(module, prv)) {
						provider = prv;
						break;
					}

				}
			}
		}

		return provider;
	}

	public static boolean isSupportedModule(IModule module) {
		String moduleID = module != null && module.getModuleType() != null ? module.getModuleType().getId() : null;

		if (moduleID == null) {
			return false;
		}

		if (delegates == null) {
			delegates = load();
		}

		for (List<ApplicationProvider> providerList : delegates.values()) {

			if (providerList != null) {
				for (ApplicationProvider provider : providerList) {

					if (supportsModule(module, provider)) {
						return true;
					}
				}
			}
		}

		return false;

	}

	private static boolean supportsModule(IModule module, ApplicationProvider provider) {
		if (module == null) {
			return false;
		}

		String moduleID = module != null && module.getModuleType() != null ? module.getModuleType().getId() : null;

		if (moduleID != null) {
			List<String> supportedModuleIDs = provider.getModuleIDs();
			if (supportedModuleIDs != null) {
				for (String supportedID : supportedModuleIDs) {
					if (moduleID.equals(supportedID)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static Map<Priority, List<ApplicationProvider>> load() {
		Map<Priority, List<ApplicationProvider>> providerMap = new HashMap<Priority, List<ApplicationProvider>>();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT);

		if (extensionPoint == null) {
			CloudFoundryPlugin.logError("Failed to load application type providers from: " + EXTENSION_POINT);
		}
		else {
			for (IExtension extension : extensionPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {

					if (APPLICATION_DELEGATE_EXT_ELEMENT.equals(config.getName())) {
						ApplicationProvider provider = new ApplicationProvider(config, EXTENSION_POINT);
						Priority priority = provider.getPriority();
						String providerID = provider.getProviderID();
						if (priority == null || providerID == null) {
							CloudFoundryPlugin
									.logError("Failed to load Cloud Foundry application provider from extension point: "
											+ EXTENSION_POINT + ". Missing provider ID and priority values");
						}
						else {
							List<ApplicationProvider> providers = providerMap.get(priority);
							if (providers == null) {
								providers = new ArrayList<ApplicationProvider>();
								providerMap.put(priority, providers);
							}
							providers.add(provider);
						}
					}
				}
			}
		}

		return providerMap;
	}

}
