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
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Stores or reads a {@link ServerProperty} in a preference store
 * 
 */
public abstract class ServerPropertyStore {

	private final ServerProperty property;

	private final static ObjectMapper mapper = new ObjectMapper();

	public ServerPropertyStore(ServerProperty property) {
		Assert.isNotNull(property);
		this.property = property;
	}

	/**
	 * 
	 * @return True if the property exists in the store. False otherwise
	 * @throws CoreException if error occurred reading the store
	 */
	public boolean hasProperty() throws CoreException {
		ServerProperties properties = getStoredProperties();
		return properties != null && properties.getProperties().contains(property);
	}

	/**
	 * 
	 * @param value true if the property should be stored. False if it should be
	 * removed.
	 * @throws CoreException if failed to add or remove the property in the
	 * store
	 */
	public void setProperty(boolean value) throws CoreException {
		ServerProperties properties = getStoredProperties();
		if (properties == null) {
			properties = new ServerProperties();
		}
		if (value) {
			properties.getProperties().add(property);
		}
		else {
			properties.getProperties().remove(property);
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

	protected ServerProperty getProperty() {
		return property;
	}

	/**
	 * Gets all the the stored {@link ServerProperty}
	 * @return All stored properties or null if no properties are stored
	 * @throws CoreException if error occurred while reading properties from
	 * store
	 */
	protected ServerProperties getStoredProperties() throws CoreException {
		String storedValue = CloudFoundryPlugin.getDefault().getPreferences().get(getPropertyID(), null);
		ServerProperties properties = null;
		if (storedValue != null) {
			try {
				properties = mapper.readValue(storedValue, ServerProperties.class);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(getError(), e);
			}
		}
		return properties;
	}

	public static class ServerProperties {

		final private List<ServerProperty> properties;

		public ServerProperties() {
			properties = new ArrayList<ServerProperty>();
		}

		public List<ServerProperty> getProperties() {
			return properties;
		}
	}
}
