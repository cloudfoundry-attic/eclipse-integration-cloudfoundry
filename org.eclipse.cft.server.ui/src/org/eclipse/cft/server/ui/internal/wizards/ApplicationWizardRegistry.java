/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.application.ApplicationProvider;
import org.eclipse.cft.server.core.internal.application.ApplicationRegistry;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IModule;

public class ApplicationWizardRegistry {

	private static Map<String, ApplicationWizardProvider> wizardProviders;

	private static final String APPLICATION_DELEGATE_EXT_ELEMENT = "applicationWizardDelegate"; //$NON-NLS-1$

	public static String EXTENSION_POINT = "org.eclipse.cft.server.ui.applicationWizard"; //$NON-NLS-1$

	public static ApplicationWizardDelegate getWizardProvider(IModule module) {
		// See if there is a corresponding application delegate
		ApplicationProvider applicationProvider = ApplicationRegistry.getApplicationProvider(module);
		return getWizardDelegate(applicationProvider);

	}

	/**
	 * Gets the corresponding application wizard delegate that matches the
	 * provider ID specified by the given application provider, or null if it
	 * could not find.
	 * @param applicationProvider
	 * @return application wizard delegate that matches the provider ID, or
	 * null.
	 */
	protected static ApplicationWizardDelegate getWizardDelegate(ApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			return null;
		}
		if (wizardProviders == null) {
			load();
		}
		ApplicationWizardProvider wizardProvider = wizardProviders.get(applicationProvider.getProviderID());
		if (wizardProvider != null) {
			return wizardProvider.getDelegate(applicationProvider.getDelegate());
		}

		return null;
	}

	public static IApplicationWizardDelegate getDefaultJavaWebWizardDelegate() {
		ApplicationProvider javaWebProvider = ApplicationRegistry.getDefaultJavaWebApplicationProvider();
		return getWizardDelegate(javaWebProvider);
	}

	private static void load() {
		wizardProviders = new HashMap<String, ApplicationWizardProvider>();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT);

		if (extensionPoint == null) {
			CloudFoundryPlugin.logError("Failed to load Cloud Foundry application wizard providers from: " //$NON-NLS-1$
					+ EXTENSION_POINT);
		}
		else {
			for (IExtension extension : extensionPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {

					if (APPLICATION_DELEGATE_EXT_ELEMENT.equals(config.getName())) {
						ApplicationWizardProvider wizardProvider = new ApplicationWizardProvider(config,
								EXTENSION_POINT);
						String providerID = wizardProvider.getProviderID();
						if (providerID == null) {
							CloudFoundryPlugin
									.logError("Failed to load application wizard provider from extension point: " //$NON-NLS-1$
											+ EXTENSION_POINT + ". Missing provider ID."); //$NON-NLS-1$
						}
						else {
							wizardProviders.put(providerID, wizardProvider);
						}
					}
				}
			}
		}
	}

}
