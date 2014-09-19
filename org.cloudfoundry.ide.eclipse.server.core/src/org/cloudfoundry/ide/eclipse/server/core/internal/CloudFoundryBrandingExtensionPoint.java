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

package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 * @author Terry Denney
 */
public class CloudFoundryBrandingExtensionPoint {

	public static String ELEM_DEFAULT_URL = "defaultUrl"; //$NON-NLS-1$

	public static String ELEM_CLOUD_URL = "cloudUrl"; //$NON-NLS-1$

	public static String ELEM_WILDCARD = "wildcard"; //$NON-NLS-1$

	public static String ATTR_REMOTE_SYSTEM_TYPE_ID = "remoteSystemTypeId"; //$NON-NLS-1$

	public static String ATTR_SERVER_DISPLAY_NAME = "serverDisplayName"; //$NON-NLS-1$

	public static String ATTR_SERVER_TYPE_ID = "serverTypeId"; //$NON-NLS-1$

	public static String ATTR_NAME = "name"; //$NON-NLS-1$

	public static String ATTR_URL = "url"; //$NON-NLS-1$

	public static String ATTR_PROVIDE_SERVICES = "provideServices"; //$NON-NLS-1$

	public static String ATTR_WIZ_BAN = "wizardBanner"; //$NON-NLS-1$

	public static String ELEM_SERVICE = "service"; //$NON-NLS-1$

	public static String ATTR_SIGNUP_URL = "signupURL"; //$NON-NLS-1$

	public static String POINT_ID = "org.cloudfoundry.ide.eclipse.server.core.branding"; //$NON-NLS-1$

	private static Map<String, IConfigurationElement> brandingDefinitions = new HashMap<String, IConfigurationElement>();

	private static List<String> brandingServerTypeIds = new ArrayList<String>();

	private static boolean read;

	public static class CloudServerURL {

		private final String name;

		private final String url;

		private final boolean userDefined;

		private final String signupURL;

		public CloudServerURL(String name, String url, boolean userDefined) {
			this(name, url, userDefined, null);
		}

		public CloudServerURL(String name, String url, boolean userDefined, String signupURL) {
			this.name = name;
			this.url = url;
			this.userDefined = userDefined;
			this.signupURL = signupURL;
		}

		public String getName() {
			return name;
		}

		public String getUrl() {
			return url;
		}

		public boolean getUserDefined() {
			return userDefined;
		}

		public String getSignupURL() {
			return signupURL;
		}

	}

	public static IConfigurationElement getConfigurationElement(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		return brandingDefinitions.get(serverTypeId);
	}

	private static List<CloudServerURL> getUrls(String serverTypeId, String elementType) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			List<CloudServerURL> result = new ArrayList<CloudServerURL>();
			IConfigurationElement[] defaultUrls = config.getChildren(elementType);
			for (IConfigurationElement defaultUrl : defaultUrls) {
				String urlName = defaultUrl.getAttribute(ATTR_NAME);
				String url = defaultUrl.getAttribute(ATTR_URL);
				String signupURL = defaultUrl.getAttribute(ATTR_SIGNUP_URL);

				if (urlName != null && urlName.length() > 0 && url != null && url.length() > 0) {
					IConfigurationElement[] wildcards = defaultUrl.getChildren(ELEM_WILDCARD);
					for (IConfigurationElement wildcard : wildcards) {
						String wildcardName = wildcard.getAttribute(ATTR_NAME);
						url = url.replaceAll(wildcardName, "{" + wildcardName + "}"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					result.add(new CloudServerURL(urlName, url, false, signupURL));
				}
			}
			return result;
		}

		return null;
	}

	public static List<CloudServerURL> getCloudUrls(String serverTypeId) {
		return getUrls(serverTypeId, ELEM_CLOUD_URL);
	}

	public static CloudServerURL getDefaultUrl(String serverTypeId) {
		List<CloudServerURL> urls = getUrls(serverTypeId, ELEM_DEFAULT_URL);
		if (urls != null && urls.size() == 1) {
			return urls.get(0);
		}

		return null;
	}

	public static String getRemoteSystemTypeId(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			return config.getAttribute(ATTR_REMOTE_SYSTEM_TYPE_ID);
		}
		return null;
	}

	public static String getServerDisplayName(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			return config.getAttribute(ATTR_SERVER_DISPLAY_NAME);
		}
		return null;
	}

	public static String getServiceName(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			return config.getAttribute(ATTR_NAME);
		}
		return null;
	}

	public static boolean getProvideServices(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			return Boolean.valueOf(config.getAttribute(ATTR_PROVIDE_SERVICES));
		}
		return false;
	}

	public static List<String> getServerTypeIds() {
		if (!read) {
			readBrandingDefinitions();
		}

		return brandingServerTypeIds;
	}

	public static String getWizardBannerPath(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			return config.getAttribute(ATTR_WIZ_BAN);
		}
		return null;
	}

	private static void readBrandingDefinitions() {
		IExtensionPoint brandingExtPoint = Platform.getExtensionRegistry().getExtensionPoint(POINT_ID);
		if (brandingExtPoint != null) {
			brandingServerTypeIds.clear();
			for (IExtension extension : brandingExtPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {
					String serverId = config.getAttribute(ATTR_SERVER_TYPE_ID);
					String name = config.getAttribute(ATTR_NAME);
					if (serverId != null && serverId.trim().length() > 0 && name != null && name.trim().length() > 0) {
						IConfigurationElement[] urls = config.getChildren(ELEM_DEFAULT_URL);
						if (urls != null && urls.length > 0) {
							brandingDefinitions.put(serverId, config);
							brandingServerTypeIds.add(serverId);
						}
					}
				}
			}
		}

		read = true;
	}

	public static boolean supportsRegistration(String serverTypeId, String url) {
		return url != null && (url.endsWith("cloudfoundry.me") || url.endsWith("vcap.me")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getSignupURL(String serverTypeId, String url) {
		if (url != null) {
			// First check the defaultURL to see if there is an associated
			// signup URL
			CloudServerURL defaultUrl = getDefaultUrl(serverTypeId);
			if (defaultUrl.getUrl().equals(url)) {
				return defaultUrl.getSignupURL();
			}
			// Then check if the cloudURLs have it
			List<CloudServerURL> cloudUrls = getCloudUrls(serverTypeId);
			for (CloudServerURL aUrl : cloudUrls) {
				if (aUrl.getUrl().equals(url)) {
					return aUrl.getSignupURL();
				}
			}
		}
		return null;
	}

}
