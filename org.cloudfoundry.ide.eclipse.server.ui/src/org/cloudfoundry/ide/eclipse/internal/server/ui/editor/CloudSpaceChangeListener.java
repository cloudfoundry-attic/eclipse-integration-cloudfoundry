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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.osgi.util.NLS;

/**
 * 
 * This allows a new cloud space to be set in a cloud server using a different
 * set of credentials and URL. A check is performed if the credentials used to
 * find the list of spaces matches the current credentials in the server.
 * 
 * Handles the following:
 * 
 * <p>
 * 1. Updates a spaces descriptor that list the available spaces for a given URL
 * and credentials for a given server, clears existing spaces in the cloud
 * server, and, if available, sets a new default space in the server from the
 * list of updates spaces
 * </p>
 * <p>
 * 2. Notifies when an updated descriptor is available, such that components
 * interested in the change can display the list of new spaces to a user, and
 * allow a user to select a new space
 * </p>
 * <p>
 * 3. Once a cloud space is selected , provides API to set the cloud space in
 * the server
 * 
 * 
 * 
 */
public class CloudSpaceChangeListener {

	private final CloudFoundryServer cloudServer;

	private CloudSpacesDescriptor spacesDescriptor;

	public CloudSpaceChangeListener(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	/**
	 * If specified credentials or URL do not match the current credentials and
	 * URL in server, exception is thrown.
	 * 
	 * @param urlText either correct URL or display version of the URL. If
	 * display URL, attempt will be made to resolve the actual URL>
	 * @param userName username to use to find list of spaces
	 * @param password password to user to find list of spaces
	 * @param context a runnable UI context, like a wizard.
	 * @return descriptor with list of cloud spaces for the given url and
	 * credentials, or null if failed to resolve.
	 * @throws CoreException if credentials and URL do not match the current
	 * credentials and URL in the server, or failed to retrieve list of spaces
	 */
	public CloudSpacesDescriptor updateDescriptor(String urlText, String userName, String password,
			IRunnableContext context) throws CoreException {
		String actualURL = CloudUiUtil.getUrlFromDisplayText(urlText);
		validateCredentials(actualURL, userName, password);
		spacesDescriptor = CloudUiUtil.getCloudSpaces(userName, password, urlText, true, context);
		internalHandleCloudSpaceSelection(spacesDescriptor);

		return spacesDescriptor;
	}

	public void clearDescriptor() {
		internalHandleCloudSpaceSelection(null);
	}

	protected void validateCredentials(String url, String userName, String password) throws CoreException {
		String actualURL = cloudServer.getUrl();
		String actualUserName = cloudServer.getUsername();
		String actualPassword = cloudServer.getPassword();

		boolean isValid = true;
		String[][] valuesToCheck = { { url, actualURL }, { userName, actualUserName }, { password, actualPassword } };
		for (String[] value : valuesToCheck) {

			if (value.length != 2 || !areValid(value[0], value[1])) {
				isValid = false;
				break;
			}
		}

		if (!isValid) {
			throw new CoreException(
					CloudFoundryPlugin.getErrorStatus(NLS
							.bind("Failed to obtain a list of cloud spaces for {0} and username {1}. Credentials or URL do not match the current credentials and URL in the server.",
									new String[] { actualURL, userName })));
		}
	}

	/**
	 * Both values must be non-null and equal to be valid.
	 * @param expected
	 * @param actual
	 * @return true if valid, false otherwise.
	 */
	protected boolean areValid(String expected, String actual) {
		if (actual != null) {
			return actual.equals(expected);
		}
		else {
			return false;
		}
	}

	public CloudSpacesDescriptor getCurrentSpacesDescriptor() {
		return spacesDescriptor;
	}

	protected void internalHandleCloudSpaceSelection(CloudSpacesDescriptor spacesDescriptor) {
		// Clear existing space
		cloudServer.setSpace(null);

		// Set a default space, if one is available
		if (spacesDescriptor != null) {
			CloudSpace defaultCloudSpace = spacesDescriptor.getDefaultCloudSpace();
			cloudServer.setSpace(defaultCloudSpace);
		}

		// Notify that a new descriptor is available so that the list of spaces
		// can be presented to a user for selection
		handleCloudSpaceSelection(spacesDescriptor);
	}

	protected void handleCloudSpaceSelection(CloudSpacesDescriptor spacesDescriptor) {
		// do nothing. Subclasses can override
	}

	public void setSelectedSpace(CloudSpace selectedCloudSpace) {
		cloudServer.setSpace(selectedCloudSpace);
	}

	public boolean hasSetSpace() {
		return cloudServer.supportsCloudSpaces();
	}

	public CloudSpace getCurrentSpace() {
		return cloudServer.getCloudFoundrySpace() != null ? cloudServer.getCloudFoundrySpace().getSpace() : null;
	}

}
