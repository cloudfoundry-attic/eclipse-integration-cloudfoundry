/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.core.internal;

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
 * IMPORTANT NOTE: This class can be referred by the branding extension from
 * adopter so this class should not be moved or renamed to avoid breakage to
 * adopters.
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
	 * subdomain, or throws {@link CoreException} if unable to generate valid
	 * URL.
	 * @param subDomain
	 * @return Non-null, valid Cloud Application URL using an existing domain.
	 */
	public CloudApplicationURL getDefaultApplicationURL(String subDomain) throws CoreException {

		List<CloudDomain> domains = getDomains();
		if (domains == null || domains.isEmpty()) {

			throw CloudErrorUtil.toCoreException(NLS.bind(
					Messages.ApplicationUrlLookupService_ERROR_GETDEFAULT_APP_URL,
					cloudServer.getServerId(), subDomain));
		}

		CloudApplicationURL appURL = validateCloudApplicationUrl(new CloudApplicationURL(subDomain, domains.get(0)
				.getName()));

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
							.getErrorStatus(Messages.ApplicationUrlLookupService_ERROR_GET_CLOUD_URL));
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
		String parsedDomainName = null;
		String parsedSubdomainName = null;
		if (authority != null) {
			for (CloudDomain domain : domainsPerActiveSpace) {
				// Be sure to check for last segment rather than last String
				// value
				// otherwise: Example: "validdomain" is a valid domain:
				// sub.domainvaliddomain will be parsed
				// successfully as a valid application URL, even though
				// "domainvaliddomain" is not a valid domain. Instead, this
				// should be the correct
				// URL: sub.domain.validdomain. A URL with just "validdomain"
				// should also
				// parse the domain part correctly (but no subdomain)
				String domainName = domain.getName();
				String domainSegment = '.' + domainName;
				if (authority.equals(domainName)) {
					parsedDomainName = domainName;
					break;
				}
				else if (authority.endsWith(domainSegment)) {
					parsedDomainName = domainName;
					// Any portion of the authority before the separating '.' is
					// the
					// subdomain. To avoid including the separating '.' between
					// subdomain and domain itself as being part of the
					// subdomain, only parse the subdomain if there
					// is an actual '.' before the domain value in the authority
					if (domainSegment.length() < authority.length()) {
						parsedSubdomainName = authority.substring(0, authority.lastIndexOf(domainSegment));
					}
					break;
				}
			}
		}

		if (parsedDomainName == null || parsedDomainName.trim().length() == 0) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(NLS.bind(
					Messages.ERROR_NO_DOMAIN_RESOLVED_FOR_URL, url)));
		}
		if (parsedSubdomainName == null || parsedSubdomainName.trim().length() == 0l) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(NLS.bind(Messages.ERROR_INVALID_SUBDOMAIN, url,
					parsedDomainName)));
		}
		return new CloudApplicationURL(parsedSubdomainName, parsedDomainName);
	}

	/**
	 * @return Non-null validated CloudApplication URL based on given URL, or
	 * throws {@link CoreException} if error occurred.
	 */
	public CloudApplicationURL validateCloudApplicationUrl(CloudApplicationURL url) throws CoreException {
		return getCloudApplicationURL(url.getUrl());
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

	/**
	 * Refreshes the current URL lookup service with up-to-date domain
	 * information. This may be a long-running process, therefore passing a
	 * progress monitor is recommended.
	 * @param cloudServer
	 * @param monitor
	 * @return non-null URL lookup service.
	 * @throws CoreException if error occurred while refreshing lookup service.
	 */
	public static ApplicationUrlLookupService update(CloudFoundryServer cloudServer, IProgressMonitor monitor)
			throws CoreException {
		ApplicationUrlLookupService lookUp = getCurrentLookup(cloudServer);
		lookUp.refreshDomains(monitor);
		return lookUp;
	}

}
