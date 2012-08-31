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
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.internal.uaa.UaaAwareCloudFoundryClient;
import org.eclipse.core.runtime.URIUtil;
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
			return UaaAwareCloudFoundryClientAccessor.getCloudFoundryClient(userName, password, url);
		}
		else {
			return getCloudFoundryOperations(userName, password, url);
		}
	}

	public CloudFoundryOperations getCloudFoundryClient(String userName, String password, String url)
			throws MalformedURLException {
		return getCloudFoundryOperations(userName, password, new URL(url));
	}

	public CloudFoundryOperations getCloudFoundryOperations(String userName, String password, URL url) {
		CloudCredentials credentials = getCredentials(userName, password, url);

		try {
			return new CloudFoundryClient(credentials, url);
		}
		catch (MalformedURLException e) {
			CloudFoundryPlugin.logError("Failed to obtain Cloud Foundry operations for " + url.toString(), e);
		}
		return null;
	}

	protected static CloudCredentials getCredentials(String userName, String password, URL url) {
		CloudCredentials credentials = new CloudCredentials(userName, password);

		return credentials;
	}

	public CloudFoundryOperations getCloudFoundryClient(String cloudControllerUrl) throws MalformedURLException {
		return new CloudFoundryClient(new URL(cloudControllerUrl));
	}

	static class UaaAwareCloudFoundryClientAccessor {

		public static CloudFoundryOperations getCloudFoundryClient(String userName, String password, URL url) {
			try {
				CloudCredentials credentials = getCredentials(userName, password, url);
				return new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), credentials, url);
			}
			catch (MalformedURLException e) {
				CloudFoundryPlugin.logError("Failed to obtain Cloud Foundry operations for " + url.toString(), e);
			}
			return null;
		}
	}

	protected static String getProxy(URL url) {

		List<Proxy> proxies = null;

		try {
			URI uri = URIUtil.toURI(url);

			if (uri != null) {
				proxies = ProxySelector.getDefault().select(uri);
			}
		}
		catch (URISyntaxException e) {
			// Ignore
		}
		return proxies != null && proxies.size() > 0 ? proxies.get(0).toString() : null;
	}
}
