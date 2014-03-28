/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *     Keith Chong, IBM - Modify Sign-up so it's more brand-friendly
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public class CloudFoundryURLNavigation extends UIWebNavigationHelper {

	public static final CloudFoundryURLNavigation INSIGHT_URL = new CloudFoundryURLNavigation(
			"http://insight.cloudfoundry.com/");

	public CloudFoundryURLNavigation(String location) {
		super(location, "Opening " + location);
	}

	public static boolean canEnableCloudFoundryNavigation(CloudFoundryServer server) {
		if (server == null) {
			return false;
		}
		return canEnableCloudFoundryNavigation(server.getServerId(), server.getUrl());
	}

	public static boolean canEnableCloudFoundryNavigation(String serverTypeId, String url) {
		if (serverTypeId == null) {
			return false;
		}
		// If the signupURL attribute is defined in the extension, then it will enable the Signup button
		return CloudFoundryBrandingExtensionPoint.getSignupURL(serverTypeId, url) != null;
	}
}
