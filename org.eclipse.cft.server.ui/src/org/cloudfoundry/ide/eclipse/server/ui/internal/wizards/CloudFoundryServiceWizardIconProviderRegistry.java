/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software Inc and IBM Corporation. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.ui.ICloudFoundryServiceWizardIconProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudFoundryServiceWizardIconProviderRegistry {

	private static final String ELEMENT = "serviceIconProvider"; //$NON-NLS-1$

	public static final String EXTENSION_POINT = "org.eclipse.cft.server.ui.serviceWizardIconProvider"; //$NON-NLS-1$

	// Static variables
	private static CloudFoundryServiceWizardIconProviderRegistry instance = new CloudFoundryServiceWizardIconProviderRegistry();

	public static CloudFoundryServiceWizardIconProviderRegistry getInstance() {
		synchronized (instance) {
			// Load on first access
			if (!instance.isLoaded) {
				instance.load();
			}

		}
		return instance;
	}

	// Member variables

	private Map<String /* server runtime type id */, ICloudFoundryServiceWizardIconProvider> providers = new HashMap<String, ICloudFoundryServiceWizardIconProvider>();

	boolean isLoaded = false;

	public ICloudFoundryServiceWizardIconProvider getIconProvider(String runtimeTypeId) {
		synchronized (this) {
			return providers.get(runtimeTypeId.toLowerCase());
		}
	}

	private void load() {

		isLoaded = true;

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT);

		if (extensionPoint == null) {
			CloudFoundryPlugin.logError("Failed to load Cloud Foundry service icon extension providers from: " //$NON-NLS-1$
					+ EXTENSION_POINT);
		}
		else {

			for (IExtension extension : extensionPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {

					if (ELEMENT.equals(config.getName())) {
						try {
							ICloudFoundryServiceWizardIconProvider provider = (ICloudFoundryServiceWizardIconProvider) config
									.createExecutableExtension("providerClass"); //$NON-NLS-1$

							String runtimeTypeId =config.getAttribute("runtimeTypeId"); //$NON-NLS-1$
							
							if(runtimeTypeId != null) {
								providers.put(runtimeTypeId, provider);
							} else {
								CloudFoundryPlugin.logError("Invalid server icon entry from:"+EXTENSION_POINT); //$NON-NLS-1$
							}

						}
						catch (CoreException e) {
							e.printStackTrace();
						}

					}
				}
			}
		}
	}

}
