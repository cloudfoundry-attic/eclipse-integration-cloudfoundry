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
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * 
 * Provider that wraps around values and classes that are created from an
 * extension point configuration element
 * 
 * @param <T> Type of delegate that this provider is creating from an extension
 * point definition
 */
public class FrameworkProvider<T> {

	private static final String CLASS_ELEMENT = "class"; //$NON-NLS-1$

	private static final String PROVIDER_ID_ATTRIBUTE = "providerID"; //$NON-NLS-1$

	private T delegate;

	protected final IConfigurationElement configurationElement;

	private String providerID;

	private final String extensionPointID;

	protected FrameworkProvider(IConfigurationElement configurationElement, String extensionPointID) {
		this.configurationElement = configurationElement;
		this.extensionPointID = extensionPointID;
	}

	public String getProviderID() {
		if (providerID == null && configurationElement != null) {
			providerID = configurationElement.getAttribute(PROVIDER_ID_ATTRIBUTE);
		}
		return providerID;
	}

	public T getDelegate() {
		if (delegate == null && configurationElement != null) {
			try {
				Object object = configurationElement.createExecutableExtension(CLASS_ELEMENT);
				if (object == null) {
					CloudFoundryPlugin
							.logError("No delegate class found. Must implement a delegate class. See extension point: " //$NON-NLS-1$
									+ extensionPointID + " for more details."); //$NON-NLS-1$
				}
				else {
					delegate = (T) object;
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return delegate;
	}

}
