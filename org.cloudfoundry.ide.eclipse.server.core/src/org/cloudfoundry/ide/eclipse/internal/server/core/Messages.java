/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

public class Messages {

	/*
	 * Errors
	 */
	public static final String ERROR_PERFORMING_CLOUD_FOUNDRY_OPERATION = "Error performing Cloud Foundry operation: {0}";

	public static final String ERROR_WRONG_EMAIL_OR_PASSWORD = "Wrong email or password";

	public static final String ERROR_UNABLE_TO_ESTABLISH_CONNECTION = "Unable to establish connection";

	public static final String ERROR_FAILED_REST_CLIENT = "Client error: {0}";

	public static final String ERROR_UNKNOWN = "Unknown Cloud Foundry error";

	public static final String ERROR_INVALID_MEMORY = "Invalid memory. Please enter a valid integer value over 0.";

	public static final String ERROR_FAILED_MEMORY_UPDATE = "Unable to update application memory";

	public static final String ERROR_FAILED_READ_SELF_SIGNED_PREFS = "Failed to read self-signed certificate preferences for servers. Unable to store self-signed certificate preferences for {0}";

	public static final String ERROR_FAILED_STORE_SELF_SIGNED_PREFS = "Failed to store self-signed certificate preference for {0}";

	/*
	 * Warnings
	 */
	public static final String WARNING_SELF_SIGNED_PROMPT_USER = "Failed to connect to {0}, probably because the site is using a self-signed certificate. Do you want to trust this site anyway?";

	/*
	 * Titles
	 */

	public static final String TITLE_SELF_SIGNED_PROMPT_USER = "Failed to connect";
}
