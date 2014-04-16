/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.Messages;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Stores self-signed certificate preferences per server URL.
 */
public class SelfSignedStore {

	public static final String SELF_SIGNED_PREF = CloudFoundryPlugin.PLUGIN_ID + ".selfsigned.certificate";

	private final String serverURL;

	private final static ObjectMapper mapper = new ObjectMapper();

	public static final String VALUE_SEPARATOR = " ";

	public SelfSignedStore(String serverURL) {
		Assert.isNotNull(serverURL);
		this.serverURL = serverURL;
	}

	/**
	 * 
	 * @return True if server uses self-signed certificates. False otherwise.
	 * @throws CoreException if error occurred
	 */
	public boolean isSelfSignedCert() throws CoreException {
		SelfSignedServers servers = getStoredServers();
		return servers != null && servers.getServers().get(serverURL) != null && servers.getServers().get(serverURL);
	}

	/**
	 * 
	 * @param selfSigned stores the self-signed certificate preference for the
	 * given server URL.
	 * @throws CoreException if failed to store value.
	 */
	public void setSelfSignedCert(boolean selfSigned) throws CoreException {
		SelfSignedServers servers = getStoredServers();
		if (servers == null) {
			servers = new SelfSignedServers();
		}
		if (selfSigned) {
			servers.getServers().put(serverURL, selfSigned);
		}
		else {
			// Remove it instead of retaining the entry. Only store servers that
			// require self-signed certificates
			servers.getServers().remove(serverURL);
		}

		String asString = null;
		if (mapper.canSerialize(servers.getClass())) {
			try {
				asString = mapper.writeValueAsString(servers);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(
						NLS.bind(Messages.ERROR_FAILED_STORE_SELF_SIGNED_PREFS, serverURL), e);
			}
		}
		else {
			throw CloudErrorUtil.toCoreException(NLS.bind(Messages.ERROR_FAILED_STORE_SELF_SIGNED_PREFS, serverURL));
		}

		if (asString != null) {
			IEclipsePreferences prefs = CloudFoundryPlugin.getDefault().getPreferences();
			prefs.put(SELF_SIGNED_PREF, asString);
			try {
				prefs.flush();
			}
			catch (BackingStoreException e) {
				throw CloudErrorUtil.toCoreException(
						NLS.bind(Messages.ERROR_FAILED_STORE_SELF_SIGNED_PREFS, serverURL), e);

			}
		}

	}

	/**
	 * 
	 * @return stored self-signed cert servers, or null if nothing is stored.
	 * @throws CoreException if error occurred while reading servers from
	 * storage
	 */
	protected SelfSignedServers getStoredServers() throws CoreException {
		String storedValue = CloudFoundryPlugin.getDefault().getPreferences().get(SELF_SIGNED_PREF, null);
		SelfSignedServers servers = null;
		if (storedValue != null) {
			try {
				servers = mapper.readValue(storedValue, SelfSignedServers.class);
			}
			catch (IOException e) {
				String message = NLS.bind(Messages.ERROR_FAILED_READ_SELF_SIGNED_PREFS, serverURL);
				throw CloudErrorUtil.toCoreException(message, e);
			}
		}
		return servers;
	}

	public static class SelfSignedServers {

		private Map<String, Boolean> servers;

		public SelfSignedServers() {
			servers = new HashMap<String, Boolean>();
		}

		public Map<String, Boolean> getServers() {
			return servers;
		}

		public void setServers(Map<String, Boolean> servers) {
			this.servers.clear();
			if (servers != null) {
				this.servers.putAll(servers);
			}
		}
	}

}
