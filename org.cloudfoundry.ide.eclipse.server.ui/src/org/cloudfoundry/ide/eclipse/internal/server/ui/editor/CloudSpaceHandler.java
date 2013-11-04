/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudOrgsAndSpaces;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.osgi.util.NLS;

/**
 * Resolves a list of orgs and spaces give a set of credentials and cloud server
 * URL. This is returned in the form of a cloud spaces descriptor.
 * 
 * Descriptors are cached per credentials and cloud server URL to prevent
 * frequent org/spaces request to the server.
 * 
 * To obtain an updated list of descriptors, always create a new
 * CloudSpaceChangeHandler instance.
 * 
 * 
 * This handler allows a new cloud space to be updated in a cloud server. A
 * check is performed if the credentials used to find the list of spaces matches
 * the current credentials in the server.
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
 * </p>
 * Note that the server configuration is not actually saved, even if a new space
 * is set in the server. it is up to the handler subclass to decide when to save
 * the server configuration.
 * 
 * 
 * 
 */
public class CloudSpaceHandler {

	private final CloudFoundryServer cloudServer;

	private CloudSpacesDescriptor spacesDescriptor;

	protected final String serverServiceName;

	/**
	 * 
	 * Local cache to prevent frequent cloud space descriptor lookups
	 */
	private Map<String, CloudSpacesDescriptor> cachedDescriptors = new HashMap<String, CloudSpacesDescriptor>();

	public CloudSpaceHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		String serverTypeId = cloudServer.getServer().getServerType().getId();
		serverServiceName = CloudFoundryBrandingExtensionPoint.getServiceName(serverTypeId);
	}

	/**
	 * Returns a cloud space descriptor which contains all the orgs and spaces
	 * for the given set of credentials and cloud URL. Note that cloud space
	 * descriptors are cached per each session of this Cloud Space change
	 * handler, in order to prevent frequent requests for orgs and spaces for
	 * credentials that have already been processed. To obtain a clean updated
	 * list of descriptors, create a new cloud space change handler.
	 * 
	 * @param urlText either correct URL or display version of the URL. If
	 * display URL, attempt will be made to resolve the actual URL>
	 * @param userName username to use to find list of spaces
	 * @param password password to user to find list of spaces
	 * @param context a runnable UI context, like a wizard.
	 * @return descriptor with list of cloud spaces for the given url and
	 * credentials, or null if failed to resolve
	 * @throws CoreException if credentials and URL do not match the current
	 * credentials and URL in the server, are invalid, or failed to retrieve
	 * list of spaces
	 */
	public CloudSpacesDescriptor getUpdatedDescriptor(String urlText, String userName, String password,
			IRunnableContext context) throws CoreException {
		String actualURL = CloudUiUtil.getUrlFromDisplayText(urlText);

		validateCredentials(actualURL, userName, password);

		String cachedDescriptorID = CloudSpacesDescriptor.getDescriptorID(userName, password, actualURL);
		spacesDescriptor = cachedDescriptors.get(cachedDescriptorID);
		if (spacesDescriptor == null) {
			CloudOrgsAndSpaces orgsAndSpaces = CloudUiUtil.getCloudSpaces(userName, password, actualURL, true, context);

			if (orgsAndSpaces != null) {
				spacesDescriptor = new CloudSpacesDescriptor(orgsAndSpaces, userName, password, actualURL);
				cachedDescriptors.put(cachedDescriptorID, spacesDescriptor);
			}
		}
		internalDescriptorChanged();

		return spacesDescriptor;
	}

	/**
	 * True if there is already a descriptor set for the given set of
	 * credentials that contains a list of orgs and spaces. False otherwise
	 * @param urlText
	 * @param userName
	 * @param password
	 * @return
	 */
	public boolean matchesCurrentDescriptor(String urlText, String userName, String password) {
		String actualURL = CloudUiUtil.getUrlFromDisplayText(urlText);

		String cachedDescriptorID = CloudSpacesDescriptor.getDescriptorID(userName, password, actualURL);

		// If there are no changes in credentials and URL, and a descriptor is
		// already present, do not do a lookup or notify listeners of changes.
		return spacesDescriptor != null && spacesDescriptor.getID() != null
				&& spacesDescriptor.getID().equals(cachedDescriptorID);
	}

	public void clearSetDescriptor() {
		spacesDescriptor = null;
	}

	protected void validateCredentials(String url, String userName, String password) throws CoreException {
		String actualURL = cloudServer.getUrl();
		String actualUserName = cloudServer.getUsername();
		String actualPassword = cloudServer.getPassword();

		boolean isValid = true;
		String[][] valuesToCheck = { { url, actualURL }, { userName, actualUserName }, { password, actualPassword } };
		for (String[] value : valuesToCheck) {

			if (!areValid(value[0], value[1])) {
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
		return actual != null ? actual.equals(expected) : false;

	}

	public CloudSpacesDescriptor getCurrentSpacesDescriptor() {
		return spacesDescriptor;
	}

	/**
	 * Force notify that the spaces descriptor has changed. Since the descriptor
	 * is assumed to be changed, a new default space obtained from that
	 * descriptor will be set in the server
	 */
	protected void internalDescriptorChanged() {

		// Set a default space, if one is available
		if (spacesDescriptor != null) {
			CloudSpace defaultCloudSpace = spacesDescriptor.getOrgsAndSpaces().getDefaultCloudSpace();
			setSelectedSpace(defaultCloudSpace);
		}
		else {
			setSelectedSpace(null);
		}

	}

	public void setSelectedSpace(CloudSpace selectedCloudSpace) {
		// Only set space if a change has occurred. This is to avoid firing
		// space change events
		// when no changes have been made, as well as avoid dirtying the server.
		if (hasSpaceChanged(selectedCloudSpace)) {
			cloudServer.setSpace(selectedCloudSpace);
		}
	}

	protected boolean hasSpaceChanged(CloudSpace selectedCloudSpace) {
		CloudFoundrySpace existingSpace = cloudServer.getCloudFoundrySpace();
		return !matchesExisting(selectedCloudSpace, existingSpace);
	}

	public static boolean matchesExisting(CloudSpace selectedCloudSpace, CloudFoundrySpace existingSpace) {
		return (existingSpace == null && selectedCloudSpace == null)
				|| (existingSpace != null && selectedCloudSpace != null
						&& existingSpace.getOrgName().equals(selectedCloudSpace.getOrganization().getName()) && existingSpace
						.getSpaceName().equals(selectedCloudSpace.getName()));
	}

	public boolean hasSetSpace() {
		return cloudServer.hasCloudSpace();
	}

	public CloudSpace getCurrentCloudSpace() {
		return cloudServer.getCloudFoundrySpace() != null ? cloudServer.getCloudFoundrySpace().getSpace() : null;
	}

	protected CloudFoundryServer getCloudServer() {
		return cloudServer;
	}

}
