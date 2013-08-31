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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Validates whether a url name value, and optionally a start command if an app
 * is a standalone app, are valid. If a standalone app, URL is optional, which
 * is also checked by this validator
 * <p/>
 * Valid url names should not include the protocol (e.g. http://www.google.com)
 * or queries in the name valid names are:
 * <p/>
 * www.google.com
 * <p/>
 * www$.google.com
 * <p/>
 * www.google.com4
 * <p/>
 * names with trailing or ending spaces, or spaces in between the name segments
 * are invalid.
 * 
 */
public class ApplicationUrlValidator {

	public static final String EMPTY_URL_ERROR = "Enter a deployment URL.";

	public static final String INVALID_CHARACTERS_ERROR = "The entered name contains invalid characters.";

	public static final String INVALID_START_COMMAND = "A start command is required when deploying a standalone application.";

	public ApplicationUrlValidator() {
	}

	public IStatus isValid(String url) {
		// Check URL validity
		String errorMessage = null;

		if (ValueValidationUtil.isEmpty(url)) {
			errorMessage = EMPTY_URL_ERROR;
		}
		else if (new URLNameValidation(url).hasInvalidCharacters()) {
			errorMessage = INVALID_CHARACTERS_ERROR;
		}

		IStatus status = errorMessage != null ? CloudFoundryPlugin.getErrorStatus(errorMessage) : Status.OK_STATUS;

		return status;
	}
}
