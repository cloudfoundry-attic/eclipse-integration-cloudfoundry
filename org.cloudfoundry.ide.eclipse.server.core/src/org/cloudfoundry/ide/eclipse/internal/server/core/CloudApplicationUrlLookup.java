/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.net.URI;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * Verifies if a given application URL is valid, and checks if the host and
 * domain portions of the URL are correct. In particular, it verifies that the
 * domain portion (the last segments of the URL: e.g, "cfapps.io" in
 * "myapp.cfapps.io") actually exists in the server.
 */
public class CloudApplicationUrlLookup {

	private final CloudFoundryServer cloudServer;

	private List<CloudDomain> domainsPerActiveSpace;

	private ApplicationUrlValidator validator;

	public CloudApplicationUrlLookup(CloudFoundryServer cloudServer) {
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
	 * @param host
	 * @return Valid, available Cloud Application URL.
	 */
	public CloudApplicationURL getDefaultApplicationURL(String host) {

		List<CloudDomain> domains = getDomains();
		if (domains == null || domains.isEmpty()) {
			return null;
		}

		CloudApplicationURL appURL = null;

		for (CloudDomain domain : domains) {
			String suggestedURL = host + "." + domain.getName();
			try {
				appURL = getCloudApplicationURL(suggestedURL);
				break;
			}
			catch (CoreException ce) {
				// Ignore. Move on to the next one
			}
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
	 * URL, or URL is invalid, including invalid host or domain.
	 */
	public CloudApplicationURL getCloudApplicationURL(String url) throws CoreException {

		IStatus isValidStatus = simpleValidation(url);
		if (!isValidStatus.isOK()) {
			throw new CoreException(isValidStatus);
		}

		if (domainsPerActiveSpace == null || domainsPerActiveSpace.isEmpty()) {
			throw new CoreException(
					CloudFoundryPlugin
							.getErrorStatus("No domains found for the current active space. Unable to map the URL to the application."));
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
					+ " -- host not specified for domain " + domainName));

		}
		return new CloudApplicationURL(host, domainName);
	}

	/**
	 * 
	 * @param cloudServer
	 * @return Non-null Cloud application URL lookup. Will attempt to fetch a
	 * cached version in the server if available, or create a new one if not.
	 */
	public static CloudApplicationUrlLookup getCurrentLookup(CloudFoundryServer cloudServer) {
		return cloudServer.getBehaviour().getApplicationUrlLookup() != null ? cloudServer.getBehaviour()
				.getApplicationUrlLookup() : new CloudApplicationUrlLookup(cloudServer);
	}

}
