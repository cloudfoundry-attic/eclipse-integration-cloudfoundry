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
package org.cloudfoundry.ide.eclipse.internal.server.core.spaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.springframework.web.client.RestClientException;

/**
 * Given a cloud server, if it supports cloud spaces, this handler will attempt
 * to resolve an actual cloud space if one is not already set in the cloud
 * server without going through cloud server behaviour. The reason it does not
 * go through the cloud server behaviour is that the cloud server may not yet
 * been fully validated, and therefore may not have a valid client. Such a case
 * will arise when part of a cloud space information like the org and space
 * names are set as properties in the server, but the full CloudSpace meta data
 * is missing as a client that requires a CloudSpace has not yet been created,
 * and therefore an actual look-up is required to obtain the CloudSpace based on
 * the org and space names saved as server properties.
 */
public class CloudSpaceServerLookup {

	private final CloudFoundryServer cloudServer;

	private CloudCredentials credentials;

	public CloudSpaceServerLookup(CloudFoundryServer cloudServer, CloudCredentials credentials) {
		this.cloudServer = cloudServer;
		this.credentials = credentials;
	}

	protected CloudCredentials getCredentials() {
		if (credentials == null) {
			String userName = cloudServer.getUsername();
			String password = cloudServer.getPassword();
			credentials = new CloudCredentials(userName, password);
		}
		return credentials;
	}

	/**
	 * 
	 * @param monitor
	 * @return a cloud space descriptor, if a lookup was successful and matched
	 * the cloud space properties set in the server. Otherwise null is returned.
	 * @throws CoreException
	 */
	public CloudFoundrySpace getCloudSpace(IProgressMonitor monitor) throws CoreException {
		CloudFoundrySpace cloudFoundrySpace = null;
		String url = cloudServer.getUrl();
		if (cloudServer.supportsCloudSpaces()) {
			cloudFoundrySpace = cloudServer.getCloudFoundrySpace();

			if (cloudFoundrySpace != null && cloudFoundrySpace.getSpace() == null) {
				// Do a look-up to determine the actual cloud space

				CloudSpacesDescriptor actualSpaces = getCloudSpaceDescriptor(monitor);

				if (actualSpaces != null && actualSpaces.supportsSpaces()) {
					CloudSpace cloudSpace = actualSpaces.getSpace(cloudFoundrySpace.getOrgName(),
							cloudFoundrySpace.getSpaceName());
					// Return null if no cloudspace was found.
					if (cloudSpace == null) {
						cloudFoundrySpace = null;
					}
					else {
						cloudFoundrySpace = new CloudFoundrySpace(cloudSpace);
					}
				}
			}
			if (cloudFoundrySpace == null) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(NLS.bind(
						"Expected a cloud space for {0} but none were found.", new String[] { url })));
			}
		}

		return cloudFoundrySpace;
	}

	public CloudSpacesDescriptor getCloudSpaceDescriptor(IProgressMonitor monitor) throws CoreException {
		String url = cloudServer.getUrl();
		return getCloudSpaceDescriptor(getCredentials(), url, monitor);
	}

	public static CloudSpacesDescriptor getCloudSpaceDescriptor(CloudCredentials credentials, String url,
			IProgressMonitor monitor) throws CoreException {
		CloudFoundryOperations operations = CloudFoundryServerBehaviour.createClient(url, credentials.getEmail(),
				credentials.getPassword());
		CoreException httpException = null;
		try {
			operations.login();
			return getCloudSpaceDescriptor(operations, monitor);
		}
		catch (CloudFoundryException cfe) {
			httpException = CloudUtil.toCoreException(cfe);
		}
		catch (RestClientException e) {
			httpException = CloudUtil.toCoreException(e);
		}
		catch (CoreException ce) {
			httpException = ce;
		}
		// Convert the core exception into user friendly error messages.
		if (httpException != null) {
			String validationMessage = CloudUtil.getV2ValidationErrorMessage(httpException);
			if (validationMessage != null) {
				httpException = new CoreException(CloudFoundryPlugin.getErrorStatus(validationMessage));
			}
			throw httpException;
		}
		return null;
	}

	private static CloudSpacesDescriptor getCloudSpaceDescriptor(CloudFoundryOperations operations,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask("Determining if the cloud server supports organizations and spaces",
				IProgressMonitor.UNKNOWN);

		boolean supportsSpaces = false;
		List<CloudSpace> actualSpaces = new ArrayList<CloudSpace>();

		try {

			supportsSpaces = operations.supportsSpaces();
			if (supportsSpaces) {
				List<CloudSpace> foundSpaces = operations.getSpaces();
				if (foundSpaces != null) {
					actualSpaces.addAll(foundSpaces);
				}
			}

			CloudSpacesDescriptor descriptor = new CloudSpacesDescriptor(actualSpaces, supportsSpaces);
			return descriptor;

		}
		catch (RuntimeException e) {
			if (e.getCause() instanceof IOException) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Parse error from server response", e.getCause()));
				throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
						"Unable to communicate with server"));
			}
			else {
				throw e;
			}
		}
		finally {
			progress.done();
		}
	}

}
