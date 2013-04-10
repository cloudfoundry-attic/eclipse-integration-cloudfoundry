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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
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
public abstract class AbstractApplicationProvider<T> {

	private static final String CLASS_ELEMENT = "class";

	private static final String PROVIDER_ID_ATTRIBUTE = "providerID";

	private T delegate;

	protected final IConfigurationElement configurationElement;

	private String providerID;

	private final String extensionPointID;

	protected AbstractApplicationProvider(IConfigurationElement configurationElement, String extensionPointID) {
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
							.logError("No delegate class found. Must implement a delegate class. See extension point: "
									+ extensionPointID + " for more details.");
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
