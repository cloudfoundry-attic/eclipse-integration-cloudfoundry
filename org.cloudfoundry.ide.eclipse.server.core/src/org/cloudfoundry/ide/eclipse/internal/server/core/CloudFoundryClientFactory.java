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

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.ide.eclipse.internal.uaa.RequestFactory;
import org.cloudfoundry.ide.eclipse.internal.uaa.UaaAwareCloudFoundryClient;
import org.springframework.ide.eclipse.uaa.UaaPlugin;

/**
 * Create Cloud Foundry clients, including clients that are UAA aware.
 * 
 */
public class CloudFoundryClientFactory {

	// Must match the import package for the UAA plugin used for Spring UAA
	// services
	public static final String SPRING_IDE_UAA_BUNDLE_SYMBOLIC_NAME = "org.springframework.ide.eclipse.uaa";

	public CloudFoundryClient getCloudFoundryClient(boolean isUAAIDEAvailable, String userName, String password, URL url) {
		if (isUAAIDEAvailable) {
			return UaaAwareCloudFoundryClientAccessor.getCloudFoundryClient(userName, password, url);
		}
		else {
			return new CloudFoundryClient(userName, password, null, url, new RequestFactory());
		}
	}

	public CloudFoundryClient getCloudFoundryClient(String userName, String password, String url)
			throws MalformedURLException {
		return new CloudFoundryClient(userName, password, url);
	}

	public CloudFoundryClient getCloudFoundryClient(String cloudControllerUrl) throws MalformedURLException {
		return new CloudFoundryClient(cloudControllerUrl);
	}

	static class UaaAwareCloudFoundryClientAccessor {

		public static CloudFoundryClient getCloudFoundryClient(String userName, String password, URL url) {
			return new UaaAwareCloudFoundryClient(UaaPlugin.getUaaService(), userName, password, null, url,
					new RequestFactory());
		}
	}
}
