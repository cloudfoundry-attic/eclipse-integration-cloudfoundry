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
 *     Keith Chong, IBM - Modify Sign-up so it's more brand-friendly
 *     IBM - Switching to use the more generic AbstractCloudFoundryUrl
 *     		instead concrete CloudServerURL
 ********************************************************************************/

package org.eclipse.cft.server.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.core.ICloudFoundryUrlProvider;
import org.eclipse.cft.server.core.AbstractCloudFoundryUrl.Wildcard;
import org.eclipse.core.runtime.CoreException;
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
	
	public static String ATTR_URL_PROVIDER_CLASS = "urlProviderClass"; //$NON-NLS-1$

	public static String POINT_ID = "org.eclipse.cft.server.core.branding"; //$NON-NLS-1$

	private static Map<String, IConfigurationElement> brandingDefinitions = new HashMap<String, IConfigurationElement>();
	
	private static Map<String, ICloudFoundryUrlProvider> urlProviders = new HashMap<String, ICloudFoundryUrlProvider> ();

	private static List<String> brandingServerTypeIds = new ArrayList<String>();

	private static boolean read;

	/**
	 * It's not recommended to use this class directly. Instead
	 * create your own instance of {@link AbstractCloudFoundryUrl}
	 */
	public static class CloudServerURL extends AbstractCloudFoundryUrl {

		private final boolean userDefined;

		private final boolean selfSigned;

		public CloudServerURL(String name, String url, boolean userDefined) {
			this(name, url, userDefined, null, false);
		}

		public CloudServerURL(String name, String url, boolean userDefined, String signupURL) {
			this (name, url, userDefined, signupURL, false);
		}

		public CloudServerURL(String name, String url, boolean userDefined, boolean selfSigned) {
			this (name, url, userDefined, null, selfSigned);
		}
		
		public CloudServerURL(String name, String url, boolean userDefined, String signUpURL, boolean selfSigned) {
			super (name, url, null, signUpURL);
			this.userDefined = userDefined;
			this.signUpUrl = signUpURL;
			this.selfSigned = selfSigned;
		}

		public boolean getUserDefined() {
			return userDefined;
		}

		public boolean getSelfSigned() {
			return selfSigned;
		}
	}
	
	/**
	 * Singleton utility class used to indicate that an extension does not have an 
	 * associated dynamic Cloud Foundry Url Provider (i.e. all its contributions
	 * are done statically via plugin.xml)
	 */
	private static class NoCloudFoundryUrlProvider implements ICloudFoundryUrlProvider {
		private static NoCloudFoundryUrlProvider instance;
		
		public static ICloudFoundryUrlProvider getInstance () {
			if (instance == null) {
				instance = new NoCloudFoundryUrlProvider();
			}			
			return instance;
		}

		@Override
		public AbstractCloudFoundryUrl getDefaultUrl() {
			return null;
		}

		@Override
		public List<AbstractCloudFoundryUrl> getNonDefaultUrls() {
			return null;
		}
	}

	public static IConfigurationElement getConfigurationElement(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		return brandingDefinitions.get(serverTypeId);
	}

	private static List<AbstractCloudFoundryUrl> getStaticUrlsFromExtension(String serverTypeId, String elementType) {
		if (!read) {
			readBrandingDefinitions();
		}
		IConfigurationElement config = brandingDefinitions.get(serverTypeId);
		if (config != null) {
			List<AbstractCloudFoundryUrl> result = new ArrayList<AbstractCloudFoundryUrl>();
			IConfigurationElement[] defaultUrls = config.getChildren(elementType);
			for (IConfigurationElement defaultUrl : defaultUrls) {
				String urlName = defaultUrl.getAttribute(ATTR_NAME);
				String url = defaultUrl.getAttribute(ATTR_URL);

				if (urlName != null && urlName.length() > 0 && url != null && url.length() > 0) {
					String signupURL = defaultUrl.getAttribute(ATTR_SIGNUP_URL);
					List <Wildcard> wildcards = new ArrayList<Wildcard>();
					IConfigurationElement[] _wildcards = defaultUrl.getChildren(ELEM_WILDCARD);
					for (IConfigurationElement wildcard : _wildcards) {
						String wildcardName = wildcard.getAttribute(ATTR_NAME);
						wildcards.add (new Wildcard(wildcardName));
					}
					result.add(new AbstractCloudFoundryUrl(urlName, url, wildcards, signupURL) {});
				}
			}
			return result;
		}

		return null;
	}
	
	/**
	 * Get the Url provider for a given server type (if needed, read it from the ext-point configuration).
	 */
	private static ICloudFoundryUrlProvider getCloudFoundryUrlProvider (String serverTypeId) {
		String urlProviderClass = null;
		ICloudFoundryUrlProvider provider = urlProviders.get(serverTypeId);
		if (provider == null) {
			try {
				IConfigurationElement config = brandingDefinitions.get(serverTypeId);
				if (config != null) {
					urlProviderClass = config.getAttribute(ATTR_URL_PROVIDER_CLASS);
					if (urlProviderClass != null && !urlProviderClass.isEmpty()) {						
						Object providerObject = config.createExecutableExtension(ATTR_URL_PROVIDER_CLASS);
						provider = (ICloudFoundryUrlProvider) providerObject;
						// Cache the provider instance so we don't have
						// to instantiate it more than once (that will
						// help in case class initialization takes too long)
						urlProviders.put(serverTypeId, provider);
					} else {
						// If no dynamic provider is available, store the special type NoCloudFoundryUrlProvider
						// as an indicator that no valid provider exists for this serverTypeId
						urlProviders.put(serverTypeId, NoCloudFoundryUrlProvider.getInstance());
					}
				} else {
					// If no configuration is available for this serverTypeId, store 
					// the special type NoCloudFoundryUrlProvider as an indicator 
					// that no valid provider exists for this serverTypeId
					urlProviders.put(serverTypeId, NoCloudFoundryUrlProvider.getInstance());
				}
			} catch (CoreException e) {
				// Any exception should be conceived as an unavailable provider, 
				// so store the special type NoCloudFoundryUrlProvider for this serverTypeId
				urlProviders.put(serverTypeId, NoCloudFoundryUrlProvider.getInstance());
				
				// This exception will be caused when the url provider class could not
				// be instantiated, in such case, log the error
				CloudFoundryPlugin.logError("Unable to instantiate " + urlProviderClass); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// Any exception should be conceived as an unavailable provider, 
				// so store the special type NoCloudFoundryUrlProvider for this serverTypeId
				urlProviders.put(serverTypeId, NoCloudFoundryUrlProvider.getInstance());
				
				// This exception will happen if the provider object could not be
				// casted to ICloudFoundryUrlProvider
				CloudFoundryPlugin.logError(urlProviderClass + 
						" must implement ICloudFoundryUrlProvider"); //$NON-NLS-1$
			}
		} else {
			// We may have a non-null Url provider at this point, but if it happens to be
			// the special type NoCloudFoundryUrlProvider, it means that actually
			// this server type was already processed but it does not have a dynamic
			// provider, in whose case we should return null
			if (provider.equals(NoCloudFoundryUrlProvider.getInstance())) {
				provider = null;
			}
		}
		
		return provider;
	}
	
	/**
	 * Returns a list of all usable (non default) Urls that can be obtained from an
	 * ICloudFoundryUrlProvider for a given server type.
	 */
	private static List<AbstractCloudFoundryUrl> getNonDefaultCloudFoundryUrlsFromProvider(String serverTypeId) {
		List<AbstractCloudFoundryUrl> result = null;
		ICloudFoundryUrlProvider provider = getCloudFoundryUrlProvider (serverTypeId);
		if (provider != null) {
			List <AbstractCloudFoundryUrl> cloudUrls = provider.getNonDefaultUrls();
			if (cloudUrls != null) {
				result = new ArrayList<AbstractCloudFoundryUrl>();
				for (AbstractCloudFoundryUrl cloudUrl : cloudUrls) {
					result.add(cloudUrl);
				}
			}
		}		
		return result;
	}
	
	/**
	 * Returns the default Url from a ICloudFoundryUrlProvider for a given server type
	 */
	private static AbstractCloudFoundryUrl getDefaultCloudFoundryUrlFromProvider(String serverTypeId) {
		AbstractCloudFoundryUrl defaultUrl = null;
		ICloudFoundryUrlProvider provider = getCloudFoundryUrlProvider (serverTypeId);
		if (provider != null) {
			defaultUrl = provider.getDefaultUrl();
		}		
		return defaultUrl;
	}

	/**
	 * Provides the list of cloud Urls (non-default) for this server.
	 * 
	 * This method merges all the cloud Urls provided either dynamically
	 * through a provider class or statically specified through contributions
	 * to the extension-point.
	 * 
	 * If a default Url is provided by a dynamic Url provider, that one will take
	 * precedence as default, which means that if there is another one specified 
	 * statically, it will be considered as a simple cloud Url 
	 * (then, returned as part of this method as well) 
	 * 
	 * @param serverTypeId
	 * @return the list of non-default Urls.
	 */
	public static List<AbstractCloudFoundryUrl> getCloudUrls(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		AbstractCloudFoundryUrl contributorDefaultUrl = getDefaultCloudFoundryUrlFromProvider(serverTypeId);
		List <AbstractCloudFoundryUrl> urls = new ArrayList<AbstractCloudFoundryUrl>();
		
		// If there is a dynamic default Url and and a static one (fixedly
		// contributed by a <defaultUrl> setting for this extension), then
		// this second one should be treated as an standard cloud Url, added
		// to the list of results we will be returning from this function.
		if (contributorDefaultUrl != null) {
			List <AbstractCloudFoundryUrl> defaultUrls = getStaticUrlsFromExtension(serverTypeId, ELEM_DEFAULT_URL);
			if (defaultUrls != null && defaultUrls.size() == 1) {
				urls.addAll(defaultUrls);
			}
		}
		
		List <AbstractCloudFoundryUrl> contributorCloudUrls = getNonDefaultCloudFoundryUrlsFromProvider(serverTypeId);
		if (contributorCloudUrls != null) {
			urls.addAll(contributorCloudUrls);
		}
		
		List <AbstractCloudFoundryUrl> cloudUrls = getStaticUrlsFromExtension(serverTypeId, ELEM_CLOUD_URL);
		if (cloudUrls != null) {
			urls.addAll(cloudUrls);
		}
		
		if (urls.size() != 0) {
			return urls;
		}
		
		return null;
	}
	
	/**
	 * Returns the default URL to be used for this server. If one is
	 * provided dynamically through a provider then that one is returned
	 * otherwise, the one contributed statically will be returned.
	 * 
	 * @param serverTypeId
	 * @return A default Url if one is available, null in other case.
	 */
	public static AbstractCloudFoundryUrl getDefaultUrl(String serverTypeId) {
		if (!read) {
			readBrandingDefinitions();
		}
		AbstractCloudFoundryUrl providerDefaultUrl = getDefaultCloudFoundryUrlFromProvider(serverTypeId);
		if (providerDefaultUrl != null) {
			// Dynamically provided default Url has priority
			return providerDefaultUrl;
		}
		
		// If no default Url was provided dynamically, then fallback to get it
		// from the static extensions
		List<AbstractCloudFoundryUrl> urls = getStaticUrlsFromExtension(serverTypeId, ELEM_DEFAULT_URL);
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
						// Either having statically provided default / cloud urls (burned in the
						// contribution), or an Url provider class means this server type 
						// should be considered as valid
						IConfigurationElement[] defaultUrl = config.getChildren(ELEM_DEFAULT_URL);
						IConfigurationElement[] cloudUrls = config.getChildren(ELEM_CLOUD_URL);
						String urlProviderClass = config.getAttribute(ATTR_URL_PROVIDER_CLASS);
						
						if ((defaultUrl != null && defaultUrl.length > 0) ||
								(cloudUrls != null && cloudUrls.length > 0) ||
								urlProviderClass != null) {
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
			AbstractCloudFoundryUrl defaultUrl = getDefaultUrl(serverTypeId);
			if (defaultUrl != null && defaultUrl.getUrl() != null && defaultUrl.getUrl().equals(url)) {
				return defaultUrl.getSignUpUrl();
			}
			// Then check if the cloudURLs have it
			List<AbstractCloudFoundryUrl> cloudUrls = getCloudUrls(serverTypeId);
			if (cloudUrls != null) {
				for (AbstractCloudFoundryUrl aUrl : cloudUrls) {
					if (aUrl.getUrl().equals(url)) {
						return aUrl.getSignUpUrl();
					}
				}
			}
		}
		return null;
	}	
}
