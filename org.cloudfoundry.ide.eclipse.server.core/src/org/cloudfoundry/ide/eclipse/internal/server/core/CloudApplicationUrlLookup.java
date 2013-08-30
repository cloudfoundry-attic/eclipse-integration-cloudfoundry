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

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpaceServerLookup;
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

	private List<CloudApplication> allApplications;

	private ApplicationUrlValidator validator;

	public CloudApplicationUrlLookup(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		validator = new ApplicationUrlValidator();
	}

	public void refreshDomains(IProgressMonitor monitor) throws CoreException {
		domainsPerActiveSpace = cloudServer.getBehaviour().getDomainsForSpace(monitor);
		CloudSpaceServerLookup lookup = new CloudSpaceServerLookup(cloudServer);
		allApplications = lookup.getAllOrgApps(monitor);
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

		IStatus isValidStatus = validator.isValid(url);
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
	 * Returns a cloud application URL if:
	 * 
	 * <p/>
	 * 1. URL is valid, meaning that its domain portion (the last segments of
	 * the URL), correspond to a known domain in the active session cloud space
	 * <p/>
	 * 2. URL is not currently taken by another application.
	 * <p/>
	 * Throws core exception otherwise.
	 * @param url to check if it is valid and available
	 * @return
	 * @throws CoreException if list of domains cannot be resolved, or host or
	 * domain in the url are invalid.
	 */
	public CloudApplicationURL getAvailableAppUrl(String url) throws CoreException {
		CloudApplicationURL appURL = getCloudApplicationURL(url);
		if (isAvailable(appURL)) {
			return appURL;
		}
		throw new CoreException(CloudFoundryPlugin.getErrorStatus("The URL is already taken by another application."));
	}

	/**
	 * True if the given Cloud app URL is not currently taken by another
	 * application. False otherwise.
	 * @param url to check.
	 */
	public boolean isAvailable(CloudApplicationURL url) {
		if (allApplications == null || allApplications.isEmpty()) {
			// If unable to check applications, for now assume valid, and let
			// the app deployment or URL mapping process determine if it is
			// valid
			return true;
		}
		String urlToCheck = url.getUrl().trim();

		for (CloudApplication application : allApplications) {
			List<String> uris = application.getUris();

			if (uris != null) {
				for (String uri : uris) {
					if (uri != null) {
						uri = uri.trim();
						if (uri.equals(urlToCheck)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

}
