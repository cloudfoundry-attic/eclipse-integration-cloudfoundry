/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * Create Cloud Foundry clients, including clients that are UAA aware.Note that
 * client/operation API should always be called within a specific Request
 * wrapper, unless performing standalone operations like validating credentials
 * or getting a list of organisations and spaces. Request wrappers do various
 * operations prior to invoking client API, including automatic client login and
 * proxy setting handling.
 * 
 * @see org.cloudfoundry.ide.eclipse.server.core.internal.client.ClientRequest
 * 
 * 
 */
public class CloudFoundryClientFactory {

	private static CloudFoundryClientFactory sessionFactory = null;

	public static CloudFoundryClientFactory getDefault() {
		if (sessionFactory == null) {
			sessionFactory = new CloudFoundryClientFactory();
		}
		return sessionFactory;
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, boolean selfSigned) {
		return getCloudFoundryOperations(credentials, url, null, selfSigned);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, CloudSpace session,
			boolean selfSigned) {

		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on
		// client
		// creation

		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return session != null ? new CloudFoundryClient(credentials, url, session, selfSigned)
				: new CloudFoundryClient(credentials, url, proxyConfiguration, selfSigned);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, String orgName,
			String spaceName, boolean selfsigned) {

		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on
		// client
		// creation
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(credentials, url, orgName, spaceName, proxyConfiguration, selfsigned);
	}

	public CloudFoundryOperations getCloudFoundryOperations(String cloudControllerUrl) throws MalformedURLException {
		return getCloudFoundryOperations(cloudControllerUrl, false);
	}

	public CloudFoundryOperations getCloudFoundryOperations(String cloudControllerUrl, boolean selfSigned)
			throws MalformedURLException {
		URL url = new URL(cloudControllerUrl);
		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on client
		// creation
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(url, proxyConfiguration, selfSigned);
	}

	protected static CloudCredentials getCredentials(String userName, String password) {
		return new CloudCredentials(userName, password);
	}

	protected static String getNormalisedProtocol(String protocol) {
		return protocol.toUpperCase();
	}

	public static HttpProxyConfiguration getProxy(URL url) {

		// URL must be set and have a valid protocol in order to determine
		// which proxy to use
		if (url == null || url.getProtocol() == null) {
			return null;
		}
		// In certain cases, the activator would have stopped and the plugin may
		// no longer be available. Usually onl happens on shutdown.

		CloudFoundryPlugin plugin = CloudFoundryPlugin.getDefault();

		if (plugin != null) {
			IProxyService proxyService = plugin.getProxyService();

			// Only set proxies IF proxies are enabled (i.e a user has selected
			// MANUAL provider configuration in network preferences. If it is
			// direct,
			// then skip proxy settings.
			if (proxyService != null && proxyService.isProxiesEnabled()) {
				IProxyData[] existingProxies = proxyService.getProxyData();

				if (existingProxies != null) {

					// Now determine the protocol to obtain the correct proxy
					// type
					String normalisedURLProtocol = getNormalisedProtocol(url.getProtocol());

					// Resolve the correct proxy data type based on the URL
					// protocol
					String[] proxyDataTypes = { IProxyData.HTTP_PROXY_TYPE, IProxyData.HTTPS_PROXY_TYPE,
							IProxyData.SOCKS_PROXY_TYPE };
					String matchedProxyData = null;
					for (String proxyDataType : proxyDataTypes) {
						String normalised = getNormalisedProtocol(proxyDataType);
						if (normalised.equals(normalisedURLProtocol)) {
							matchedProxyData = proxyDataType;
							break;
						}
					}

					if (matchedProxyData != null) {
						for (IProxyData data : existingProxies) {

							if (matchedProxyData.equals(data.getType())) {
								int proxyPort = data.getPort();
								String proxyHost = data.getHost();
								String user = data.getUserId();
								String password = data.getPassword();
								return proxyHost != null ? new HttpProxyConfiguration(proxyHost, proxyPort,
										data.isRequiresAuthentication(), user, password) : null;
							}
						}
					}
				}
			}
		}

		return null;

	}
}
