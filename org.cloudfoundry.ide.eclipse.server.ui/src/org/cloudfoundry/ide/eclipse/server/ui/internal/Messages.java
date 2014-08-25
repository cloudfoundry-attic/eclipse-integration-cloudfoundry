/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

public class Messages {

	/*
	 * Errors
	 */
	public static final String ERROR_NO_VALIDATOR_PRESENT = "Internal Error: No validator present to validate credentials. Please check with technical support.";

	public static final String ERROR_NO_CALLBACK_UNABLE_TO_REFRESH_CONSOLE = "No Cloud Foundry console callback available. Unable to refresh console contents.";

	public static final String ERROR_VALID_SERVER_NAME = "Please enter a valid server name.";

	public static final String ERROR_SERVER_NAME_ALREADY_EXISTS = "A Cloud Foundry server instance with the specified name already exists. Please select another name.";

	public static final String ERROR_UNKNOWN_SERVER_CREATION_ERROR = "Unable to create a Cloud Foundry server instance - unknown error";

	public static final String ERROR_NO_USERNAME_SPACES = "No username found in existing server. Unable to update list of orgs and spaces for the server";

	public static final String ERROR_NO_PASSWORD_SPACES = "No password found in existing server. Unable to update list of orgs and spaces for the server";

	public static final String ERROR_NO_URL_SPACES = "No password found in existing server. Unable to update list of orgs and spaces for the server";

	public static final String ERROR_APPLICATION_LOG = "Error while processing application log for {0} - {1}";

	public static final String ERROR_FAILED_INITIALISE_APPLICATION_LOG_STREAM = "Failed to initialise application log stream due to missing cloud application module or cloud server. No application logs will be streamed.";

	public static final String ERROR_ROUTE_IN_USE = "Cloud route - {0} - is in use. Please only delete routes that are not in use.";

	public static final String UNKNOWN_APPLICATION = "Unknown application";

	/*
	 * UI Labels
	 */

	public static final String LABEL_MEMORY_LIMIT = "Memory Limit (MB):";

	public static final String LABEL_ENABLE_TRACING = "HTTP Tracing";

	public static final String TOOLTIP_ENABLE_TRACING = "Enables HTTP Tracing in a Cloud Foundry tracing console";

	public static final String ENTER_AN_EMAIL = "Enter an email address";

	public static final String ENTER_A_PASSWORD = "Enter a password";

	public static final String SELECT_SERVER_URL = "Select a {0} URL";

	public static final String ROUTE_NAME = "Name";

	public static final String ROUTES_SHOW_IN_USE = "Show In Use";

	public static final String ROUTES_SHOW_REMOVED = "Show Removed";

	public static final String REMOVE = "Remove";

	public static final String SELECT_ALL = "Select All";

	public static final String DESELECT_ALL = "Deselect All";

	public static final String DOMAIN_COMBO = "Domain:";

	/*
	 * Dialogue messages
	 */
	public static final String SERVER_WIZARD_VALIDATOR_CLICK_TO_VALIDATE = "Press 'Validate Account', 'Next', 'Finish' to validate credentials.";

	public static final String SHOWING_CONSOLE = "Fetching console contents. Please wait...\n";

	public static final String CLONE_SERVER_WIZARD_OK_MESSAGE = "Please select a cloud space and enter a server name to create a new server instance to that space.";

	public static final String ROUTE_PAGE_DESCRIPTION = "Select Cloud routes to remove";

	public static final String REFRESHING_DOMAIN_ROUTES = "Refreshing list of domains and routes";

}
