/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.uaa.UaaAwareCloudFoundryClient;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.springframework.ide.eclipse.uaa.UaaPlugin;

/**
 * Create Cloud Foundry clients, including clients that are UAA aware.Note that
 * client/operation API should always be called within a specific Request
 * wrapper, unless performing standalone operations like validating credentials
 * or getting a list of organisations and spaces. Request wrappers do various
 * operations prior to invoking client API, including automatic client login and
 * proxy setting handling.
 * 
 * @see org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour.Request
 * 
 * 
 */
public class CloudFoundryClientFactory {

	// Must match the import package for the UAA plugin used for Spring UAA
	// services
	public static final String SPRING_IDE_UAA_BUNDLE_SYMBOLIC_NAME = "org.springframework.ide.eclipse.uaa";

	public CloudFoundryOperations getCloudFoundryOperations(boolean isUAAIDEAvailable, String userName,
			String password, URL url) {
		return getCloudFoundryOperations(isUAAIDEAvailable, getCredentials(userName, password), url);
	}

	public CloudFoundryOperations getCloudFoundryOperations(boolean isUAAIDEAvailable, CloudCredentials credentials,
			URL url) {
		return getCloudFoundryOperations(isUAAIDEAvailable, credentials, url, null);
	}

	public CloudFoundryOperations getCloudFoundryOperations(boolean isUAAIDEAvailable, CloudCredentials credentials,
			URL url, CloudSpace session) {
		if (isUAAIDEAvailable) {
			return new UaaAwareCloudFoundryClientAccessor().getCloudFoundryOperations(credentials, url, session);
		}
		else {
			return getCloudFoundryOperations(credentials, url, session);
		}
	}

	public CloudFoundryOperations getCloudFoundryOperations(String userName, String password, URL url) {
		CloudCredentials credentials = getCredentials(userName, password);
		return getCloudFoundryOperations(credentials, url, null);
	}

	protected static CloudCredentials getCredentials(String userName, String password) {
		return new CloudCredentials(userName, password);
	}

	public CloudFoundryOperations getCloudFoundryOperations(String cloudControllerUrl) throws MalformedURLException {
		URL url = new URL(cloudControllerUrl);
		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on client
		// creation
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(url, proxyConfiguration);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, CloudSpace session) {
		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on client
		// creation
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return session != null ? new CloudFoundryClient(credentials, url, session) : new CloudFoundryClient(
				credentials, url, proxyConfiguration);
	}

	static class UaaAwareCloudFoundryClientAccessor {

		public CloudFoundryOperations getCloudFoundryOperations(String userName, String password, URL url) {
			return getCloudFoundryOperations(getCredentials(userName, password), url);
		}

		public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url) {
			return getCloudFoundryOperations(credentials, url, null);
		}

		public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url,
				CloudSpace session) {
			try {
				HttpProxyConfiguration proxyConfiguration = getProxy(url);
				// If proxy is not updated now, it will still be updated on each
				// client request, so setting the proxy right now is not
				// critical
				return session != null ? new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), credentials, url,
						session) : new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), credentials, url,
						proxyConfiguration);
			}
			catch (MalformedURLException e) {
				CloudFoundryPlugin.logError("Failed to obtain Cloud Foundry operations for " + url.toString(), e);
			}
			return null;
		}
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
								return proxyHost != null ? new HttpProxyConfiguration(proxyHost, proxyPort) : null;
							}
						}
					}
				}
			}
		}

		return null;

	}
}
