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
import org.cloudfoundry.ide.eclipse.internal.server.core.ValidationEvents;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 * Performs both local and remote server validation of server account details
 * for a given Cloud Foundry server instance ( {@link CloudFoundryServer} ),
 * like credentials and cloud spaces, as well as handling self-signed errors
 * when remote server authorisations are attempted.
 * 
 */
public abstract class ServerWizardValidator implements ServerValidator {

	private final CloudFoundryServer cfServer;

	private final CloudSpacesDelegate cloudServerSpaceDelegate;

	/**
	 * The Server validator acts as an event source (it generates events)
	 */
	private final IEventSource<ServerWizardValidator> validatorEventSource = new IEventSource<ServerWizardValidator>() {

		public ServerWizardValidator getSource() {
			return ServerWizardValidator.this;
		}
	};

	// Session cache as long as the validator is used, as to not keep
	// prompting the user multiple times
	private boolean acceptSelfSigned = false;

	public ServerWizardValidator(CloudFoundryServer cloudServer, CloudSpacesDelegate cloudServerSpaceDelegate) {
		this.cfServer = cloudServer;
		this.cloudServerSpaceDelegate = cloudServerSpaceDelegate;
	}

	public CloudSpacesDelegate getSpaceDelegate() {
		return cloudServerSpaceDelegate;
	}

	public CloudFoundryServer getCloudFoundryServer() {
		return cfServer;
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
		return validate(false, true, null);
	}

	/**
	 * Note that this will be run in the UI thread.
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServerValidator
	 * #validate(boolean, org.eclipse.jface.operation.IRunnableContext)
	 */
	public synchronized ValidationStatus validate(boolean validateAgainstServer, boolean validateSpace,
			IRunnableContext runnableContext) {
		ValidationStatus status = serverValidation(validateAgainstServer, validateSpace, runnableContext);

		// If validating against server, also check self-signed errors.
		if (validateAgainstServer) {
			status = checkSelfSigned(status, runnableContext);
			if (status != null && status.getStatus().isOK()) {
				serverValidation(validateAgainstServer, validateSpace, runnableContext);
			}
		}

		return status;
	}

	/**
	 * 
	 * @param status containing possible self-signed information. If null, no
	 * further checks or validation will occur
	 * @param context
	 * @return {@link IStatus#OK} if self-signed was accepted. IStatus.ERROR if
	 * Null if initial status is also null.
	 */
	protected ValidationStatus checkSelfSigned(final ValidationStatus status, final IRunnableContext context) {
		// If not status is passes that may contain self-signed information, no
		// further validation is possible
		if (status == null) {
			return null;
		}
		final ValidationStatus[] validationStatus = new ValidationStatus[] { status };

		IStatus iStatus = validationStatus[0].getStatus();

		if (validationStatus[0].getEventType() == ValidationEvents.SELF_SIGNED
				&& iStatus.getException() instanceof SSLPeerUnverifiedException) {

			Display.getDefault().syncExec(new Runnable() {

				public void run() {

					// Now check if for this server URL there is a stored
					// value
					// indicating whether to user self-signed certs or not.
					boolean storedSelfSign = cfServer.getSelfSignedCertificate();

					if (!storedSelfSign) {
						String message = NLS.bind(Messages.WARNING_SELF_SIGNED_PROMPT_USER, cfServer.getUrl());

						if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
								Messages.TITLE_SELF_SIGNED_PROMPT_USER, message)) {
							acceptSelfSigned = true;
						}
					}
					else {
						acceptSelfSigned = storedSelfSign;
					}

					// Re-validate if the user has selected to continue with
					// self-signed certificate
					if (acceptSelfSigned) {
						validationStatus[0] = new ValidationStatus(Status.OK_STATUS, ValidationEvents.VALIDATION);
					}
					else {
						// If user selected not to accept self-signed server, no
						// further validation
						// can be possible. Indicate this as an error
						validationStatus[0] = new ValidationStatus(CloudFoundryPlugin
								.getErrorStatus(Messages.ERROR_UNABLE_CONNECT_SERVER_CREDENTIALS),
								ValidationEvents.SELF_SIGNED);
					}
				}
			});
		}

		// Always update the value in the server. For example, if a
		// previous run has determined that this URL needs
		// self-signed
		// certificate
		// but no SSL error was thrown, it means that Server
		// condition
		// has changed (i.e it no longer needs the certificate).
		cfServer.setSelfSignedCertificate(acceptSelfSigned);
		return validationStatus[0];
	}

	/**
	 * Validates the server credentials and URL using a standalone disposable
	 * Java client ( {@link CloudFoundryOperations} ).
	 * @param validateAgainstServer true if credentials should be validated
	 * against a server. False if validation should be local (e.g. checking
	 * credential and URL syntax)
	 * @param runnableContext
	 * @return non-null validation status.
	 */
	protected ValidationStatus serverValidation(boolean validateAgainstServer, boolean validateSpace,
			IRunnableContext runnableContext) {

		// Check for valid username, password, and server URL syntax first
		// before attempting a remote validation
		ValidationStatus validationStatus = validateLocally();

		String userName = cfServer.getUsername();
		String password = cfServer.getPassword();
		String url = cfServer.getUrl();

		IStatus eventStatus = null;

		if (validationStatus.getStatus().isOK()) {

			// If credentials changed, clear the space descriptor. If a server
			// validation is required later on
			// the new credentials will be validated and a new descriptor will
			// be obtained.
			if (!cloudServerSpaceDelegate.matchesCurrentDescriptor(url, userName, password)) {
				cloudServerSpaceDelegate.clearDescriptor();
			}

			try {

				// Credential validation errors result in CoreExceptions
				// handled below.
				// Likewise, OperationCanceledExceptions are handled below.
				if (validateAgainstServer) {
					// Indicate that the URL may be a display URL (in which case
					// the validation will convert
					// it to an actual URL)
					boolean displayURL = true;
					CloudUiUtil.validateCredentials(userName, password, url, displayURL, acceptSelfSigned,
							runnableContext);
				}

				if (validateSpace) {
					cloudServerSpaceDelegate.validate(url, userName, password, acceptSelfSigned, runnableContext,
							validateAgainstServer);
				}
			}
			catch (CoreException e) {
				// Even if an error occurred, classify the event as a validaiton
				// event, UNLESS it is a
				// self-signed error
				int eventType = ValidationEvents.VALIDATION;
				if (e.getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
					eventType = ValidationEvents.SELF_SIGNED;
				}
				eventStatus = e.getStatus();
				validationStatus = getValidationStatus(eventStatus, eventType);
			}
			catch (OperationCanceledException oce) {
				eventStatus = null;
				validationStatus = getValidationStatus(Status.OK_STATUS, ValidationEvents.EVENT_NONE);
			}

		}
		return validationStatus;

	}

	protected ValidationStatus getValidationStatus(int statusType, String validationMessage, int eventType) {
		IStatus status = CloudFoundryPlugin.getStatus(validationMessage, statusType);
		return getValidationStatus(status, eventType);
	}

	protected ValidationStatus getValidationStatus(IStatus status, int eventType) {
		return new ServerWizardValidationStatus(status, eventType);
	}

	/**
	 * 
	 * @return status of local validation of account information, without
	 * requiring connection to a remote Cloud Foundry server. Should not be used
	 * for long running validation. Must not return null status.
	 */
	protected abstract ValidationStatus validateLocally();

	/**
	 * Local validation of account values. Checks if any values (like username,
	 * password, or server URL) are missing or have incorrect syntax.
	 * <p/>
	 * Does not attempt to authorise the account against the server.
	 * @return non-null local validation status.
	 */

	private class ServerWizardValidationStatus extends ValidationStatus implements IAdaptable {

		public ServerWizardValidationStatus(IStatus status, int eventType) {
			super(status, eventType);
		}

		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class clazz) {
			if (clazz.equals(PartChangeEvent.class)) {
				return new PartChangeEvent(null, getStatus(), validatorEventSource, getEventType());
			}
			return null;
		}
	}
}
