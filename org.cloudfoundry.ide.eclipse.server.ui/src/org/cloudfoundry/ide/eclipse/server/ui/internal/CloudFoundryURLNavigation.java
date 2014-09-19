/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 *     Keith Chong, IBM - Modify Sign-up so it's more brand-friendly
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.eclipse.osgi.util.NLS;

public class CloudFoundryURLNavigation extends UIWebNavigationHelper {

	public static final CloudFoundryURLNavigation INSIGHT_URL = new CloudFoundryURLNavigation(
			"http://insight.cloudfoundry.com/"); //$NON-NLS-1$

	public CloudFoundryURLNavigation(String location) {
		super(location, NLS.bind(Messages.CloudFoundryURLNavigation_TEXT_OPEN_LABEL, location));
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
		// If the signupURL attribute is defined in the extension, then it will
		// enable the Signup button
		return CloudFoundryBrandingExtensionPoint.getSignupURL(serverTypeId, url) != null;
	}
}
