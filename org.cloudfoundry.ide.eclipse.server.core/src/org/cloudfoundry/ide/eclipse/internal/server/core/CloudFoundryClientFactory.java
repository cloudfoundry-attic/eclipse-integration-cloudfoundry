/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.uaa.UaaAwareCloudFoundryClient;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.springframework.ide.eclipse.uaa.UaaPlugin;

/**
 * Create Cloud Foundry clients, including clients that are UAA aware.
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
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(url, proxyConfiguration);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, CloudSpace session) {
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return session != null ? new CloudFoundryClient(credentials, url, proxyConfiguration, session)
				: new CloudFoundryClient(credentials, url, proxyConfiguration);
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
				return session != null ? new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), credentials, url,
						proxyConfiguration, session) : new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(),
						credentials, url, proxyConfiguration);
			}
			catch (MalformedURLException e) {
				CloudFoundryPlugin.logError("Failed to obtain Cloud Foundry operations for " + url.toString(), e);
			}
			return null;
		}
	}

	protected static HttpProxyConfiguration getProxy(URL url) {

		IProxyService proxyService = CloudFoundryPlugin.getDefault().getProxyService();
		IProxyData[] existingProxies = proxyService.getProxyData();
		if (existingProxies != null && existingProxies.length > 0) {
			for (IProxyData data : existingProxies) {
				if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType())) {
					int proxyPort = existingProxies[0].getPort();
					String proxyHost = existingProxies[0].getHost();
					return proxyHost != null ? new HttpProxyConfiguration(proxyHost, proxyPort) : null;
				}
			}
		}

		return null;

	}
}
