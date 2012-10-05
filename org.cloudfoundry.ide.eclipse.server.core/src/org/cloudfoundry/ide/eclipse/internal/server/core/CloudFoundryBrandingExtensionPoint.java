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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudVersion;
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

	public static String ELEM_DEFAULT_URL = "defaultUrl";

	public static String ELEM_CLOUD_URL = "cloudUrl";

	public static String ELEM_WILDCARD = "wildcard";

	public static String ATTR_REMOTE_SYSTEM_TYPE_ID = "remoteSystemTypeId";

	public static String ATTR_SERVER_DISPLAY_NAME = "serverDisplayName";

	public static String ATTR_SERVER_TYPE_ID = "serverTypeId";

	public static String ATTR_NAME = "name";

	public static String ATTR_URL = "url";

	public static String ATTR_PROVIDE_SERVICES = "provideServices";

	public static String ATTR_WIZ_BAN = "wizardBanner";

	public static String ELEM_SERVICE = "service";

	public static String POINT_ID = "org.cloudfoundry.ide.eclipse.server.core.branding";

	private static Map<String, IConfigurationElement> brandingDefinitions = new HashMap<String, IConfigurationElement>();

	private static boolean read;

	public static class CloudURL {

		private final String name;

		private final String url;

		private final boolean userDefined;

		private final CloudVersion version;

		public CloudURL(String name, String url, boolean userDefined) {
			this(name, url, userDefined, null);
		}

		public CloudURL(String name, String url, boolean userDefined, CloudVersion version) {
			this.name = name;
			this.url = url;
			this.userDefined = userDefined;
			this.version = version;
		}

		public CloudVersion getCloudVersion() {
			return version;
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

		public CloudURL updateVersion(CloudVersion version) {
			return new CloudURL(getName(), getUrl(), getUserDefined(), version);
		}
	}

	public static IConfigurationElement getConfigurationElement(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		return brandingDefinitions.get(serverTypeId);
	}

	private static List<CloudURL> getUrls(String serverTypeId, String elementType) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			List<CloudURL> result = new ArrayList<CloudURL>();
			IConfigurationElement[] defaultUrls = config.getChildren(elementType);
			for (IConfigurationElement defaultUrl : defaultUrls) {
				String urlName = defaultUrl.getAttribute(ATTR_NAME);
				String url = defaultUrl.getAttribute(ATTR_URL);

				if (urlName != null && urlName.length() > 0 && url != null && url.length() > 0) {
					IConfigurationElement[] wildcards = defaultUrl.getChildren(ELEM_WILDCARD);
					for (IConfigurationElement wildcard : wildcards) {
						String wildcardName = wildcard.getAttribute(ATTR_NAME);
						url = url.replaceAll(wildcardName, "{" + wildcardName + "}");
					}
					result.add(new CloudURL(urlName, url, false));
				}
			}
			return result;
		}

		return null;
	}

	public static List<CloudURL> getCloudUrls(String serverTypeId) {
		return getUrls(serverTypeId, ELEM_CLOUD_URL);
	}

	public static CloudURL getDefaultUrl(String serverTypeId) {
		List<CloudURL> urls = getUrls(serverTypeId, ELEM_DEFAULT_URL);
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
			for (IExtension extension : brandingExtPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {
					String serverId = config.getAttribute(ATTR_SERVER_TYPE_ID);
					String name = config.getAttribute(ATTR_NAME);
					if (serverId != null && serverId.trim().length() > 0 && name != null && name.trim().length() > 0) {
						IConfigurationElement[] urls = config.getChildren(ELEM_DEFAULT_URL);
						if (urls != null && urls.length > 0) {
							brandingDefinitions.put(serverId, config);
						}
					}
				}
			}
		}

		read = true;
	}

	public static boolean supportsRegistration(String serverTypeId, String url) {
		return url != null && (url.endsWith("cloudfoundry.me") || url.endsWith("vcap.me"));
	}

}
