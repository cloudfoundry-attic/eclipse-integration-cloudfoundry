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

	public CloudFoundryOperations getCloudFoundryClient(boolean isUAAIDEAvailable, String userName, String password,
			URL url) {
		if (isUAAIDEAvailable) {
			return new UaaAwareCloudFoundryClientAccessor().getCloudFoundryClient(userName, password, url);
		}
		else {
			return getCloudFoundryOperations(userName, password, url);
		}
	}

	public CloudFoundryOperations getCloudFoundryClient(boolean isUAAIDEAvailable, CloudCredentials credentials, URL url) {
		if (isUAAIDEAvailable) {
			return new UaaAwareCloudFoundryClientAccessor().getCloudFoundryClient(credentials, url);
		}
		else {
			return getCloudFoundryOperations(credentials, url);
		}
	}

	public CloudFoundryOperations getCloudFoundryClient(String userName, String password, String url)
			throws MalformedURLException {
		return getCloudFoundryOperations(userName, password, new URL(url));
	}

	public CloudFoundryOperations getCloudFoundryOperations(String userName, String password, URL url) {
		CloudCredentials credentials = getCredentials(userName, password, url);
		return getCloudFoundryOperations(credentials, url);
	}

	protected static CloudCredentials getCredentials(String userName, String password, URL url) {
		return new CloudCredentials(userName, password);
	}

	public CloudFoundryOperations getCloudFoundryClient(String cloudControllerUrl) throws MalformedURLException {
		URL url = new URL(cloudControllerUrl);
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(url, proxyConfiguration);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url) {
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(credentials, url, proxyConfiguration);
	}

	static class UaaAwareCloudFoundryClientAccessor {

		public CloudFoundryOperations getCloudFoundryClient(String userName, String password, URL url) {
			try {
				CloudCredentials credentials = getCredentials(userName, password, url);
				HttpProxyConfiguration proxyConfiguration = getProxy(url);
				return new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), credentials, url, proxyConfiguration);
			}
			catch (MalformedURLException e) {
				CloudFoundryPlugin.logError("Failed to obtain Cloud Foundry operations for " + url.toString(), e);
			}
			return null;
		}

		public CloudFoundryOperations getCloudFoundryClient(CloudCredentials credentials, URL url) {
			try {
				HttpProxyConfiguration proxyConfiguration = getProxy(url);
				return new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), credentials, url, proxyConfiguration);
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
