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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsValidationStatics;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.osgi.util.NLS;

/**
 * Validates a username, password and orgs and spaces selection, both locally
 * and with a Cloud Foundry server. The local option allows for quick
 * validation, since the server validation may be a long running job.
 */
public class ServerWizardValidator implements ServerValidator {

	private final CloudFoundryServer cfServer;

	private final CloudSpaceHandler handler;

	private ValidationStatus previousStatus;

	public ServerWizardValidator(CloudFoundryServer cloudServer, CloudSpaceHandler spaceHandler) {
		this.cfServer = cloudServer;
		this.handler = spaceHandler;
	}

	public CloudSpaceHandler getSpaceHandler() {
		return handler;
	}

	/**
	 * True if username and password were set in the previous validation.
	 * However, it does not imply that the credentials were also validated
	 * against the server, merely that they were present when a validation
	 * request was made before.
	 * 
	 */
	public synchronized boolean areCredentialsFilled() {

		return previousStatus != null
				&& (previousStatus.getValidationType() == ServerCredentialsValidationStatics.EVENT_SPACE_VALID || previousStatus
						.getValidationType() == ServerCredentialsValidationStatics.EVENT_INVALID_SPACE_FILLED_CREDENTIALS);

	}

	/**
	 * True if the previous status was valid. False if a new validation is
	 * required or previous status was not valid.
	 */
	public synchronized ValidationStatus getPreviousValidationStatus() {
		return previousStatus;

	}

	/**
	 * Can be run outside of UI thread.
	 * 
	 * @param validateAgainstServer
	 * @param handle
	 * @return
	 */
	public ValidationStatus validate(boolean validateAgainstServer) {
		return validate(validateAgainstServer, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServerValidator
	 * #validate(boolean, org.eclipse.jface.operation.IRunnableContext)
	 */
	public synchronized ValidationStatus validate(boolean validateAgainstServer, IRunnableContext runnableContext) {
		IStatus localValidation = validateLocally(cfServer);

		String userName = cfServer.getUsername();
		String password = cfServer.getPassword();
		String url = cfServer.getUrl();

		String message = localValidation.getMessage();
		String errorMsg = null;

		int validationType = ServerCredentialsValidationStatics.EVENT_NONE;

		if (localValidation.isOK()) {

			// First check if a space is already selected, and credentials
			// haven't changed, meaning new credentials validation and space
			// look up is not necessary
			if (!handler.matchesCurrentDescriptor(url, userName, password)) {
				handler.clearSetDescriptor();
				message = "Press 'Validate Account', 'Next' or 'Finish' to validate credentials.";
				validationType = ServerCredentialsValidationStatics.EVENT_INVALID_SPACE_FILLED_CREDENTIALS;
			}
			else if (handler.hasSetSpace()) {
				validationType = ServerCredentialsValidationStatics.EVENT_SPACE_VALID;
				// No need to validate as credentials haven't changed and space
				// is the same
				validateAgainstServer = false;
			}

			if (validateAgainstServer) {
				errorMsg = CloudUiUtil.validateCredentials(cfServer, userName, password, url, true, runnableContext);

				// No credential errors, so now do a orgs and spaces lookup for
				// the newly validated credentials.
				if (errorMsg == null) {

					try {
						CloudSpacesDescriptor descriptor = handler.getUpdatedDescriptor(url, userName, password,
								runnableContext);
						if (descriptor == null) {
							errorMsg = "Failed to resolve organizations and spaces for the given credentials. Please contact Cloud Foundry support.";
						}
						else if (handler.hasSetSpace()) {
							validationType = ServerCredentialsValidationStatics.EVENT_SPACE_VALID;
						}
						else {
							message = "No Cloud space selected. Please select a valid Cloud space";
							validationType = ServerCredentialsValidationStatics.EVENT_INVALID_SPACE_FILLED_CREDENTIALS;
						}
					}
					catch (CoreException e) {
						errorMsg = "Failed to resolve organization and spaces "
								+ (e.getMessage() != null ? " due to " + e.getMessage()
										: ". Unknown error occurred while requesting list of spaces from the server")
								+ ". Please contact Cloud Foundry support.";
					}
				}
			}

		}

		int statusType = IStatus.OK;
		if (errorMsg != null) {
			message = errorMsg;
			statusType = IStatus.ERROR;
		}
		else if (validationType == ServerCredentialsValidationStatics.EVENT_SPACE_VALID) {
			message = ServerCredentialsValidationStatics.VALID_ACCOUNT_MESSAGE;
		}
		else {
			statusType = IStatus.INFO;
		}

		IStatus eventStatus = CloudFoundryPlugin.getStatus(message, statusType);

		previousStatus = new ValidationStatus(eventStatus, validationType);

		return previousStatus;

	}

	protected IStatus validateLocally(CloudFoundryServer cfServer) {

		String userName = cfServer.getUsername();
		String password = cfServer.getPassword();
		String url = cfServer.getUrl();
		String message = null;

		boolean valuesFilled = false;

		if (userName == null || userName.trim().length() == 0) {
			message = "Enter an email address.";
		}
		else if (password == null || password.trim().length() == 0) {
			message = "Enter a password.";
		}
		else if (url == null || url.trim().length() == 0) {
			message = NLS.bind("Select a {0} URL.", handler.serverServiceName);
		}
		else {
			valuesFilled = true;
			message = NLS.bind(ServerCredentialsValidationStatics.DEFAULT_DESCRIPTION, handler.serverServiceName);
		}

		int statusType = valuesFilled ? IStatus.OK : IStatus.ERROR;

		return CloudFoundryPlugin.getStatus(message, statusType);
	}

	public static class ValidationStatus {

		private final IStatus status;

		private final int validationType;

		public ValidationStatus(IStatus status, int validationType) {
			this.status = status;
			this.validationType = validationType;
		}

		public IStatus getStatus() {
			return status;
		}

		public int getValidationType() {
			return validationType;
		}
	}

}
