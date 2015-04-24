/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Stores a property for a particular server. Property is persisted.
 * 
 */
public abstract class ServerPropertyStore {

	private final String serverURL;

	private final static ObjectMapper mapper = new ObjectMapper();

	public static final String VALUE_SEPARATOR = " "; //$NON-NLS-1$

	public ServerPropertyStore(String serverURL) {
		Assert.isNotNull(serverURL);
		this.serverURL = serverURL;
	}

	/**
	 * 
	 * @return True if server has property. False otherwise.
	 * @throws CoreException if error occurred
	 */
	public boolean hasProperty() throws CoreException {
		BooleanProperties servers = getStoredProperties();
		return servers != null && servers.getProperties().get(serverURL) != null
				&& servers.getProperties().get(serverURL);
	}

	/**
	 * 
	 * @param property value.
	 * @throws CoreException if failed to store value.
	 */
	public void setProperty(boolean value) throws CoreException {
		BooleanProperties properties = getStoredProperties();
		if (properties == null) {
			properties = new BooleanProperties();
		}
		if (value) {
			properties.getProperties().put(serverURL, value);
		}
		else {
			properties.getProperties().remove(serverURL);
		}

		String asString = null;
		if (mapper.canSerialize(properties.getClass())) {
			try {
				asString = mapper.writeValueAsString(properties);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(getError(), e);
			}
		}
		else {
			throw CloudErrorUtil.toCoreException(getError());
		}

		if (asString != null) {
			IEclipsePreferences prefs = CloudFoundryPlugin.getDefault().getPreferences();
			prefs.put(getPropertyID(), asString);
			try {
				prefs.flush();
			}
			catch (BackingStoreException e) {
				throw CloudErrorUtil.toCoreException(getError(), e);
			}
		}
	}

	abstract protected String getPropertyID();

	abstract protected String getError();

	protected String getServerUrl() {
		return serverURL;
	}

	/**
	 * 
	 * @return stored property for the servers
	 * @throws CoreException if error occurred while reading servers from
	 * storage
	 */
	protected BooleanProperties getStoredProperties() throws CoreException {
		String storedValue = CloudFoundryPlugin.getDefault().getPreferences().get(getPropertyID(), null);
		BooleanProperties properties = null;
		if (storedValue != null) {
			try {
				properties = mapper.readValue(storedValue, BooleanProperties.class);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(getError(), e);
			}
		}
		return properties;
	}

	public static class BooleanProperties {

		private Map<String, Boolean> properties;

		public BooleanProperties() {
			properties = new HashMap<String, Boolean>();
		}

		public Map<String, Boolean> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, Boolean> servers) {
			this.properties.clear();
			if (servers != null) {
				this.properties.putAll(servers);
			}
		}
	}
}
