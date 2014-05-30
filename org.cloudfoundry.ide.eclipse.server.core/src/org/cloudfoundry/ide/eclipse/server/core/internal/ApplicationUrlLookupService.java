/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.net.URI;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Verifies if a given application URL is valid, and checks if the host and
 * domain portions of the URL are correct. In particular, it verifies that the
 * domain portion (the last segments of the URL: e.g, "cfapps.io" in
 * "myapp.cfapps.io") actually exists in the server.
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters. 
 */
public class ApplicationUrlLookupService {

	private final CloudFoundryServer cloudServer;

	private List<CloudDomain> domainsPerActiveSpace;

	private ApplicationUrlValidator validator;

	public ApplicationUrlLookupService(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		validator = new ApplicationUrlValidator();
	}

	public void refreshDomains(IProgressMonitor monitor) throws CoreException {
		domainsPerActiveSpace = cloudServer.getBehaviour().getDomainsForSpace(monitor);
	}

	/**
	 * Returns cached list of domains. If null or empty, refresh the list of
	 * domains separately.
	 * @return
	 */
	public List<CloudDomain> getDomains() {
		return domainsPerActiveSpace;
	}

	/**
	 * Either returns a valid, available Cloud Application URL with the given
	 * host, or null
	 * @param subDomain
	 * @return Valid, available Cloud Application URL.
	 */
	public CloudApplicationURL getDefaultApplicationURL(String subDomain) throws CoreException {

		List<CloudDomain> domains = getDomains();
		if (domains == null || domains.isEmpty()) {

			throw CloudErrorUtil.toCoreException(NLS.bind(
					"No application domains resolved for {0}. Unable to generate a default application URL for {1}",
					cloudServer.getServerId(), subDomain));
		}

		CloudApplicationURL appURL = null;
		CoreException lastError = null;
		for (CloudDomain domain : domains) {
			String suggestedURL = subDomain + "." + domain.getName();
			try {
				appURL = getCloudApplicationURL(suggestedURL);
				break;
			}
			catch (CoreException ce) {
				lastError = ce;
			}
		}
		if (appURL == null) {
			if (lastError == null) {
				lastError = CloudErrorUtil.toCoreException(NLS.bind(
						"Unable to generate a default application URL for {0} in server {1}", subDomain,
						cloudServer.getServerId()));
			}
			throw lastError;
		}

		return appURL;
	}

	/**
	 * Performs base URL validation (checking if it is empty or has invalid
	 * characters), but does not perform any checks against existing domains.
	 * @param url
	 * @return OK status if valid. Error or Warning status otherwise.
	 */
	public IStatus simpleValidation(String url) {
		return validator.isValid(url);
	}

	/**
	 * Note: See org.cloudfoundry.client.lib.rest.CloudControllerClientImpl.
	 * 
	 * TODO: This is duplicated for CF 1.5.1/vcap-java-client-lib 0.8.6, but it
	 * should be pushed down to the client lib and host/domain abstraction
	 * exposed via the appropriate client abstraction.
	 * <p/>
	 * Either returns a valid, non-null Cloud application URL, whose domain
	 * matches the domains listed for the active session space, or throws
	 * CoreException if error occurred, including invalid URL.
	 * <p/>
	 * It does NOT check if the URL is taken already, even if valid.
	 * 
	 * @return non-null valid Cloud Application URL. Never returns null. If
	 * error, exception is thrown instead.
	 * @throws CoreException if unable to retrieve list of domains to check the
	 * URL, or URL is invalid, including invalid subdomain or domain.
	 */
	public CloudApplicationURL getCloudApplicationURL(String url) throws CoreException {

		IStatus isValidStatus = simpleValidation(url);
		if (!isValidStatus.isOK()) {
			throw new CoreException(isValidStatus);
		}

		if (domainsPerActiveSpace == null || domainsPerActiveSpace.isEmpty()) {
			throw new CoreException(
					CloudFoundryPlugin
							.getErrorStatus("No domains found for the current active space. Unable to generate a default application URL."));
		}

		// String url = domain.getName();
		// url = url.replace("http://", "");
		URI newUri;
		try {
			newUri = URI.create(url);
		}
		catch (IllegalArgumentException e) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
		}

		String authority = newUri.getScheme() != null ? newUri.getAuthority() : newUri.getPath();
		String domainName = null;
		String host = null;
		for (CloudDomain domain : domainsPerActiveSpace) {
			if (authority != null && authority.endsWith(domain.getName())) {
				domainName = domain.getName();
				if (domainName.length() < authority.length()) {
					host = authority.substring(0, authority.indexOf(domainName) - 1);
				}
				break;
			}
		}
		if (domainName == null || domainName.trim().length() == 0) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus("Domain not found for URL " + url));
		}
		if (host == null || host.trim().length() == 0l) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus("Invalid URL " + url
					+ " -- subdomain not specified for domain " + domainName));

		}
		return new CloudApplicationURL(host, domainName);
	}

	/**
	 * 
	 * @param cloudServer
	 * @return Cloud Application URL look service. Is never null.
	 */
	public static ApplicationUrlLookupService getCurrentLookup(CloudFoundryServer cloudServer) {
		ApplicationUrlLookupService service = cloudServer.getBehaviour().getApplicationUrlLookup();
		if (service == null) {
			service = new ApplicationUrlLookupService(cloudServer);
		}
		return service;
	}

}
