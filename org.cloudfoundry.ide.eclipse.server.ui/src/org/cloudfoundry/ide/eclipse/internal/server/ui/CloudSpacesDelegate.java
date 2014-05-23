/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
 *     IBM - Fix duplicate space check
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudOrgsAndSpaces;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.osgi.util.NLS;

/**
 * Resolves a list of orgs and spaces given a set of credentials and cloud
 * server URL. This is returned in the form of a {@link CloudSpacesDescriptor}
 * <p/>
 * Descriptors are cached per credentials and cloud server URL to prevent
 * frequent org/spaces request to the server.
 * <p/>
 * To obtain an updated list of descriptors, always create a new
 * CloudSpaceChangeHandler instance.
 * <p/>
 * Also Performs checks and validations on cloud spaces, and has API to set a
 * selected space that may be invoked by a space selection component, like a UI
 * part that displays available cloud spaces.
 * 
 * 
 * 
 * 
 */
public abstract class CloudSpacesDelegate {

	private final CloudFoundryServer cloudServer;

	private CloudSpacesDescriptor spacesDescriptor;

	protected final String serverServiceName;

	/**
	 * 
	 * Local cache to prevent frequent cloud space descriptor lookups
	 */
	private Map<String, CloudSpacesDescriptor> cachedDescriptors = new HashMap<String, CloudSpacesDescriptor>();

	protected CloudSpacesDelegate(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		String serverTypeId = cloudServer.getServer().getServerType().getId();
		serverServiceName = CloudFoundryBrandingExtensionPoint.getServiceName(serverTypeId);
	}

	public String getServerServiceName() {
		return serverServiceName;
	}

	/**
	 * @return the first space available that has no corresponding server
	 * instance. If null, no space found that is not already associated with a
	 * server instance.
	 */
	public CloudSpace getSpaceWithNoServerInstance() {
		CloudSpacesDescriptor descriptor = getCurrentSpacesDescriptor();
		CloudOrgsAndSpaces orgsSpaces = descriptor != null ? descriptor.getOrgsAndSpaces() : null;

		if (orgsSpaces != null) {

			List<CloudFoundryServer> cloudServers = CloudServerUtil.getCloudServers();
			if (cloudServers == null || cloudServers.isEmpty()) {
				return orgsSpaces.getDefaultCloudSpace();
			}
			else {
				List<CloudSpace> spaces = orgsSpaces.getAllSpaces();
				if (spaces != null) {
					// Find a space that does not have a corresponding server
					// instance.
					for (CloudSpace space : spaces) {
						CloudFoundryServer existingServer = null;
						for (CloudFoundryServer cloudServer : cloudServers) {
							if (matchesExisting(space, cloudServer.getCloudFoundrySpace())) {
								existingServer = cloudServer;
								break;
							}
						}
						if (existingServer == null) {
							return space;
						}
					}
				}
			}
		}
		return null;
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
		return getCurrentSpacesDescriptor() != null && getCurrentSpacesDescriptor().getID() != null
				&& getCurrentSpacesDescriptor().getID().equals(cachedDescriptorID);
	}

	/**
	 * Given space selection, determine if it is valid. For example, a user
	 * wishes to create a server instance to the selected cloudSpace, if the
	 * cloud space is valid, return
	 * {@link org.eclipse.core.runtime.Status#OK_STATUS}.
	 * @param cloudServerURL target server URL containing the selected cloud
	 * space. Used to check if other existing server instances with that server
	 * URL already target the selected org/space. If null, a check will be
	 * performed against the delegate's associated cloud server.
	 * @param selectionObj a potential space selection.
	 * @return if valid, return
	 * {@link org.eclipse.core.runtime.Status#OK_STATUS}. Otherwise return
	 * appropriate error status. Must not be null.
	 */
	public IStatus validateSpaceSelection(String cloudServerURL, CloudSpace selectedCloudSpace) {
		String errorMessage = null;

		if (cloudServerURL == null) {
			cloudServerURL = getCloudServer().getUrl();
		}

		if (selectedCloudSpace == null) {
			errorMessage = org.cloudfoundry.ide.eclipse.internal.server.core.Messages.ERROR_INVALID_SPACE;
		}
		else if (cloudServerURL != null) {
			List<CloudFoundryServer> cloudServers = CloudServerUtil.getCloudServers();
			if (cloudServers != null) {
				for (CloudFoundryServer cloudServer : cloudServers) {
					// Can ignore the cloud space check if the URL is different.
					if (cloudServerURL.equals(cloudServer.getUrl())
							&& matchesExisting(selectedCloudSpace, cloudServer.getCloudFoundrySpace())) {
						errorMessage = NLS.bind(Messages.ERROR_SERVER_INSTANCE_CLOUD_SPACE_EXISTS, cloudServer
								.getServer().getName(), selectedCloudSpace.getName());
						break;
					}
				}
			}
		}

		return (errorMessage != null) ? CloudFoundryPlugin.getErrorStatus(errorMessage) : Status.OK_STATUS;
	}

	public CloudSpacesDescriptor getCurrentSpacesDescriptor() {
		return spacesDescriptor;
	}

	public void clearDescriptor() {
		spacesDescriptor = null;
	}

	/**
	 * Validates the cloud org/space descriptor as well as the cloud space
	 * selection in the current local server instance. Note that cloud space
	 * descriptors are cached per each session of this Cloud Space change
	 * handler, in order to prevent frequent requests for orgs and spaces for
	 * credentials that have already been processed. To obtain a clean updated
	 * list of descriptors, create a new cloud space change handler.
	 * 
	 * @param urlText either correct URL or display version of the URL. If
	 * display URL, attempt will be made to resolve the actual URL>
	 * @param userName username to use to find list of spaces
	 * @param password password to user to find list of spaces
	 * @paramg selfSigned true if connecting to self-signed certificate.
	 * @return descriptor with list of cloud spaces for the given URL and
	 * credentials, or null if failed to resolve
	 * @param context a runnable UI context, like a wizard.
	 * @param updateDescriptor if true, will send a request to the server with
	 * the given set of credentials to obtain a new list of orgs/spaces and
	 * then perform check on the current server if a default space is set. If
	 * false, will only perform a local space selection check on the current
	 * server
	 * @throws CoreException if credentials and URL do not match the current
	 * credentials and URL in the server, are invalid, or failed to retrieve
	 * list of spaces
	 */
	public CloudSpacesDescriptor validate(String urlText, String userName, String password, boolean selfSigned,
			IRunnableContext context, boolean updateDescriptor) throws CoreException {
		CloudSpacesDescriptor descriptor = null;
		if (updateDescriptor) {
			try {
				descriptor = internalUpdateDescriptor(urlText, userName, password, selfSigned, context);
			}
			catch (CoreException e) {
				// Handle orgs and spaces errors separately from other
				// errors (e.g. credential validation errors) to convert
				// them to user-friendly messages.
				String message = e.getMessage() != null ? NLS.bind(
						Messages.ERROR_FAILED_RESOLVE_ORGS_SPACES_DUE_TO_ERROR, e.getMessage())
						: Messages.ERROR_CHECK_CONNECTION_NO_SPACES;
				throw CloudErrorUtil.toCoreException(message, e);
			}
		}

		IStatus status = validateCurrent(getCurrentCloudSpace());
		if (status != null && !status.isOK()) {
			throw new CoreException(status);
		}
		return descriptor;
	}

	protected CloudSpacesDescriptor internalUpdateDescriptor(String urlText, String userName, String password,
			boolean selfSigned, IRunnableContext context) throws CoreException {
		String actualURL = CloudUiUtil.getUrlFromDisplayText(urlText);

		validateCredentials(actualURL, userName, password);

		String cachedDescriptorID = CloudSpacesDescriptor.getDescriptorID(userName, password, actualURL);
		spacesDescriptor = cachedDescriptors.get(cachedDescriptorID);
		if (spacesDescriptor == null) {
			CloudOrgsAndSpaces orgsAndSpaces = CloudUiUtil.getCloudSpaces(userName, password, actualURL, true,
					selfSigned, context);

			if (orgsAndSpaces != null) {
				spacesDescriptor = new CloudSpacesDescriptor(orgsAndSpaces, userName, password, actualURL);
				cachedDescriptors.put(cachedDescriptorID, spacesDescriptor);
			}
		}

		return spacesDescriptor;

	}

	protected IStatus validateCurrent(CloudSpace currentSpace) {
		int severity = IStatus.OK;
		String validationMessage = Messages.VALID_ACCOUNT;
		CloudSpacesDescriptor descriptor = getCurrentSpacesDescriptor();
		if (descriptor == null || descriptor.getOrgsAndSpaces() == null) {
			validationMessage = Messages.ERROR_CHECK_CONNECTION_NO_SPACES;
			severity = IStatus.ERROR;
		}
		else if (getSpaceWithNoServerInstance() == null) {
			validationMessage = Messages.ERROR_ALL_SPACES_ASSOCIATED_SERVER_INSTANCES;
			severity = IStatus.ERROR;
		}
		else {
			return validateSpaceSelection(currentSpace);
		}
		return CloudFoundryPlugin.getStatus(validationMessage, severity);
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

	protected CloudFoundryServer getCloudServer() {
		return cloudServer;
	}

	public static boolean matchesExisting(CloudSpace selectedCloudSpace, CloudFoundrySpace existingSpace) {
		return (existingSpace == null && selectedCloudSpace == null)
				|| (existingSpace != null && selectedCloudSpace != null
						&& existingSpace.getOrgName().equals(selectedCloudSpace.getOrganization().getName()) && existingSpace
						.getSpaceName().equals(selectedCloudSpace.getName()));
	}

	/**
	 * Given space selection, determine if it is valid. For example, a user
	 * wishes to create a server instance to the selected cloudSpace, if the
	 * cloud space is valid, return
	 * {@link org.eclipse.core.runtime.Status#OK_STATUS}.
	 * @param selectionObj a potential space selection.
	 * @return if valid, return
	 * {@link org.eclipse.core.runtime.Status#OK_STATUS}. Otherwise return
	 * appropriate error status. Must not be null.
	 */
	public IStatus validateSpaceSelection(CloudSpace selectedCloudSpace) {
		return validateSpaceSelection(null, selectedCloudSpace);
	}

	public boolean hasSpace() {
		return getCurrentCloudSpace() != null;
	}

	/**
	 * Sets a cloud space, selected externally. For example, a UI component may
	 * invoke this method when a user selects a cloud space in the UI.
	 * Subclasses can decide what to do when the UI component invokes this
	 * method, like for example, setting the cloud space in a
	 * {@link CloudFoundryServer}
	 * @param selectedCloudSpace a selected space to be set.
	 */
	public abstract void setSelectedSpace(CloudSpace selectedCloudSpace);

	/**
	 * 
	 * @return currently set cloud space, or null.
	 */
	protected abstract CloudSpace getCurrentCloudSpace();

}
