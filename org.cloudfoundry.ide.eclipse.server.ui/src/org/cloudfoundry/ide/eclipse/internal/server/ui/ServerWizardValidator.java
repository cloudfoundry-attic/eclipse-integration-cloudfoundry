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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsValidationStatics;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 * Validates a username, password and orgs and spaces selection, both locally
 * and with a Cloud Foundry server. The local option allows for quick
 * validation, since the server validation may be a long running job.
 * 
 */
public class ServerWizardValidator implements ServerValidator {

	/*
	 * 
	 * NOTE: Be sure to keep track of the previous status and that previous
	 * status is always kept up to date
	 */
	private ValidationStatus previousStatus;

	private final CloudFoundryServer cfServer;

	private final CloudServerSpaceDelegate cloudServerSpaceDelegate;

	public static final ValidationStatus CREDENTIALS_SET_VALIDATION_STATUS = new ValidationStatus(
			CloudFoundryPlugin.getStatus(
					org.cloudfoundry.ide.eclipse.internal.server.ui.Messages.SERVER_WIZARD_VALIDATOR_CLICK_TO_VALIDATE,
					IStatus.INFO), ServerCredentialsValidationStatics.EVENT_CREDENTIALS_FILLED);

	// Session cache as long as the validator is used, as to not keep
	// prompting the user multiple times
	private boolean selfSigned = false;

	public ServerWizardValidator(CloudFoundryServer cloudServer, CloudServerSpaceDelegate cloudServerSpaceDelegate) {
		this.cfServer = cloudServer;
		this.cloudServerSpaceDelegate = cloudServerSpaceDelegate;
	}

	public CloudServerSpaceDelegate getSpaceDelegate() {
		return cloudServerSpaceDelegate;
	}

	/**
	 * True if username and password were filled without errors in the last
	 * validation.
	 * 
	 */
	public synchronized boolean areCredentialsFilled() {

		return previousStatus != null
				&& previousStatus.getStatus().getSeverity() != IStatus.ERROR
				&& (previousStatus.getValidationType() == ServerCredentialsValidationStatics.EVENT_SPACE_VALID || previousStatus
						.getValidationType() == ServerCredentialsValidationStatics.EVENT_CREDENTIALS_FILLED);

	}

	/**
	 * Validation from the last validation run.
	 */
	public synchronized ValidationStatus getPreviousValidationStatus() {
		return previousStatus;
	}

	/**
	 * Can be run outside of UI thread. Does a local validation without
	 * connecting to the server. It is mean to do a local check of credentials
	 * (e.g. acceptable credential values), but not validate against the server.
	 * 
	 * @param validateAgainstServer
	 * @param handle
	 * @return
	 */
	public ValidationStatus localValidation() {
		return validate(false, null);
	}

	/**
	 * Note that this will be run in the UI thread.
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServerValidator
	 * #validate(boolean, org.eclipse.jface.operation.IRunnableContext)
	 */
	public synchronized ValidationStatus validate(boolean validateAgainstServer, IRunnableContext runnableContext) {
		ValidationStatus status = null;
		if (validateAgainstServer) {
			status = doValidateInUI(runnableContext);
		}
		else {
			status = internalValidate(validateAgainstServer, selfSigned, runnableContext);
		}
		previousStatus = status;
		return status;
	}

	/**
	 * Validates the credentials against the server (will connect to the server)
	 * in the UI thread, as dialogues may be opened to the user in this case.
	 * @return validation of server credentials
	 */
	public synchronized ValidationStatus validateInUI(IRunnableContext context) {
		ValidationStatus status = doValidateInUI(context);
		previousStatus = status;
		return status;
	}

	protected ValidationStatus doValidateInUI(final IRunnableContext context) {
		final ValidationStatus[] validationStatus = new ValidationStatus[1];

		// Must run in UI thread since errors result in a dialogue opening.
		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				validationStatus[0] = internalValidate(true, selfSigned, context);
				IStatus status = validationStatus[0].getStatus();

				if (validationStatus[0].getValidationType() == ServerCredentialsValidationStatics.EVENT_SELF_SIGNED_ERROR
						&& status.getException() instanceof SSLPeerUnverifiedException) {
					// Now check if for this server URL there is a stored value
					// indicating whether to user self-signed certs or not.
					boolean storedSelfSign = cfServer.getSelfSignedCertificate();

					if (!storedSelfSign) {
						String message = NLS.bind(Messages.WARNING_SELF_SIGNED_PROMPT_USER, cfServer.getUrl());

						if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
								Messages.TITLE_SELF_SIGNED_PROMPT_USER, message)) {
							selfSigned = true;
						}
					}
					else {
						selfSigned = storedSelfSign;
					}

					// Re-validate if the user has selected to continue with
					// self-signed certificate
					if (selfSigned) {
						validationStatus[0] = internalValidate(true, selfSigned, context);
					}
					else {
						validationStatus[0] = new ValidationStatus(CloudFoundryPlugin
								.getErrorStatus(Messages.ERROR_UNABLE_CONNECT_SERVER_CREDENTIALS),
								ServerCredentialsValidationStatics.EVENT_SELF_SIGNED_ERROR);
					}

				}

				// Always update the value in the server. For example, if a
				// previous run has determined that this URL needs self-signed
				// certificate
				// but no SSL error was thrown, it means that Server condition
				// has changed (i.e it no longer needs the certificate).
				cfServer.setSelfSignedCertificate(selfSigned);

			}
		});

		return validationStatus[0];
	}

	/**
	 * Validates the server credentials and URL using a standalone disposable
	 * Java client ( {@link CloudFoundryOperations} ).
	 * @param validateAgainstServer
	 * @param runnableContext
	 * @return non-null validation status.
	 */
	protected ValidationStatus internalValidate(boolean validateAgainstServer, boolean selfSigned,
			IRunnableContext runnableContext) {

		// Check for valid username, password, and server URL syntax first
		// before attempting a remote validation
		IStatus localValidation = validateLocally(cfServer);

		String userName = cfServer.getUsername();
		String password = cfServer.getPassword();
		String url = cfServer.getUrl();

		String message = localValidation.getMessage();

		// CF-specific validation type that is not represented in a standard
		// IStatus.
		// This enables/disables additional wizard behaviour
		int cfValidationType = ServerCredentialsValidationStatics.EVENT_NONE;

		IStatus eventStatus = null;

		// If local validation is not OK, treat as INFO rather than ERROR. INFO
		// will still keep wizard buttons disabled. A general
		// INFO event status is created later with the local validation error
		// message obtained above.
		if (localValidation.isOK()) {

			// First check if a space is already selected, and credentials
			// haven't changed, meaning new credentials validation and space
			// look up is not necessary
			if (!cloudServerSpaceDelegate.matchesCurrentDescriptor(url, userName, password)) {
				cloudServerSpaceDelegate.clearDescriptor();
				message = CREDENTIALS_SET_VALIDATION_STATUS.getStatus().getMessage();
				cfValidationType = CREDENTIALS_SET_VALIDATION_STATUS.getValidationType();
			}
			else if (cloudServerSpaceDelegate.hasSetSpace()) {
				cfValidationType = ServerCredentialsValidationStatics.EVENT_SPACE_VALID;
				// No need to validate as credentials haven't changed and space
				// is the same
				validateAgainstServer = false;
			}

			if (validateAgainstServer) {
				try {

					// Indicate that the URL may be a display URL (in which case
					// the validation will convert
					// it to an actual URL)
					boolean displayURL = true;

					// Credential validation errors result in CoreExceptions
					// handled below.
					// Likewise, OperationCanceledExceptions are handled below.
					CloudUiUtil.validateCredentials(userName, password, url, displayURL, selfSigned, runnableContext);

					// At this stage, credentials have been validated. Now check
					// orgs and spaces.
					String orgSpacesErrorMessage = null;

					try {
						CloudSpacesDescriptor descriptor = cloudServerSpaceDelegate.getUpdatedDescriptor(url, userName,
								password, selfSigned, runnableContext);
						if (descriptor == null) {
							orgSpacesErrorMessage = Messages.ERROR_FAILED_RESOLVE_ORGS_SPACES;
						}
						else if (cloudServerSpaceDelegate.hasSetSpace()) {
							cfValidationType = ServerCredentialsValidationStatics.EVENT_SPACE_VALID;
						}
						else {
							// Don't treat this as an error. By default
							// non-error that are also not OK are treated as
							// INFO status.
							message = Messages.ERROR_NO_VALID_CLOUD_SPACE_SELECTED;
							cfValidationType = ServerCredentialsValidationStatics.EVENT_CREDENTIALS_FILLED;
						}
					}
					catch (CoreException e) {
						// Handle orgs and spaces errors separately from other
						// errors (e.g. credential validation errors) to convert
						// them to user-friendly messages.
						orgSpacesErrorMessage = e.getMessage() != null ? NLS.bind(
								Messages.ERROR_FAILED_RESOLVE_ORGS_SPACES_DUE_TO_ERROR, e.getMessage())
								: Messages.ERROR_FAILED_RESOLVE_ORGS_SPACES;
					}

					if (orgSpacesErrorMessage != null) {
						int statusType = IStatus.ERROR;
						eventStatus = CloudFoundryPlugin.getStatus(orgSpacesErrorMessage, statusType);
					}
				}
				catch (CoreException e) {
					if (e.getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
						cfValidationType = ServerCredentialsValidationStatics.EVENT_SELF_SIGNED_ERROR;
					}
					eventStatus = e.getStatus();
				}
				catch (OperationCanceledException oce) {
					eventStatus = null;
				}
			}
		}

		if (eventStatus == null) {

			// By default status are marked as INFO, unless they are explicit
			// errors or OK validations. This allows the wizard to retain its
			// current state (and show the default wizard page
			// message) in case neither OK or ERROR status are received (for
			// example, a credentials validation operation has been cancelled).
			int statusType = IStatus.INFO;

			if (cfValidationType == ServerCredentialsValidationStatics.EVENT_SPACE_VALID) {
				message = ServerCredentialsValidationStatics.VALID_ACCOUNT_MESSAGE;
				statusType = IStatus.OK;
			}

			eventStatus = CloudFoundryPlugin.getStatus(message, statusType);
		}

		return new ValidationStatus(eventStatus, cfValidationType);

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
			message = NLS.bind("Select a {0} URL.", cloudServerSpaceDelegate.serverServiceName);
		}
		else {
			valuesFilled = true;
			message = NLS.bind(ServerCredentialsValidationStatics.DEFAULT_DESCRIPTION,
					cloudServerSpaceDelegate.serverServiceName);
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

		/**
		 * @return one of the following:
		 * {@link ServerCredentialsValidationStatics#EVENT_CREDENTIALS_FILLED},
		 * {@link ServerCredentialsValidationStatics#EVENT_NONE},
		 * {@link ServerCredentialsValidationStatics#EVENT_SELF_SIGNED_ERROR},
		 * {@link ServerCredentialsValidationStatics#EVENT_SPACE_CHANGED},
		 * {@link ServerCredentialsValidationStatics#EVENT_SPACE_VALID}
		 */
		public int getValidationType() {
			return validationType;
		}
	}

}
