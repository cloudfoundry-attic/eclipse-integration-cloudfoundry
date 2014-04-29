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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

public class Messages {

	/*
	 * Errors
	 */

	public static final String ERROR_PERFORMING_CLOUD_FOUNDRY_OPERATION = "Error performing Cloud Foundry operation: {0}";

	public static final String ERROR_WRONG_EMAIL_OR_PASSWORD_UNAUTHORISED = "Wrong email or password - 401 (Unauthorised)";

	public static final String ERROR_WRONG_EMAIL_OR_PASSWORD_FORBIDDEN = "Wrong email or password - 403 (Forbidden)";

	public static final String ERROR_UNABLE_TO_ESTABLISH_CONNECTION_UNKNOWN_HOST = "Unable to establish connection - Unknown host";

	public static final String ERROR_FAILED_REST_CLIENT = "Client error - {0}";

	public static final String ERROR_FAILED_RESOLVE_ORGS_SPACES = "Failed to resolve organizations and spaces for the given credentials";

	public static final String ERROR_FAILED_RESOLVE_ORGS_SPACES_DUE_TO_ERROR = "Failed to resolve organizations and spaces - {0}";

	public static final String ERROR_NO_VALID_CLOUD_SPACE_SELECTED = "No cloud space selected. Please select a valid cloud space.";
	
	public static final String ERROR_INVALID_ORG = "Invalid organization";
	
	public static final String ERROR_INVALID_SPACE = "Invalid space";

	public static final String ERROR_UNKNOWN = "Unknown Cloud Foundry error";

	public static final String ERROR_INVALID_MEMORY = "Invalid memory. Please enter a valid integer value over 0.";

	public static final String ERROR_FAILED_MEMORY_UPDATE = "Unable to update application memory";

	public static final String ERROR_FAILED_READ_SELF_SIGNED_PREFS = "Failed to read self-signed certificate preferences for servers. Unable to store self-signed certificate preferences for {0}";

	public static final String ERROR_FAILED_STORE_SELF_SIGNED_PREFS = "Failed to store self-signed certificate preference for {0}";

	public static final String ERROR_UNABLE_CONNECT_SERVER_CREDENTIALS = "Unable to connect to the server to validate credentials";

	public static final String ERROR_FAILED_CLIENT_CREATION_NO_SPACE = "Unable to resolve locally stored organisation and space for the server instance {0}. The server instance may have to be cloned or created again.";

	public static final String ERROR_FAILED_MODULE_REFRESH = "Failed to refresh list of applications. Application list may not be accurate. Check connection and try a manual refresh - Reason: {0}";

	public static final String ERROR_FIRE_REFRESH = "Internal Error: Failed to resolve Cloud Foundry server from WST IServer. Manual server disconnect and reconnect may be required - Reason: {0}";

	public static final String ERROR_INITIALISE_REFRESH_NO_SERVER = "Failed to initialise Cloud Foundry refresh job. Unable to resolve a Cloud Foundry server - {0}";

	public static final String ERROR_APP_DEPLOYMENT_VALIDATION_ERROR = "Invalid application deployment information for: {0} - Unable to deploy or start application - {1}";

	public static final String ERROR_NO_WST_MODULE = "Internal Error: No WST IModule specified - Unable to deploy or start application";

	public static final String ERROR_NO_MAPPED_CLOUD_MODULE = "Internal Error: No mapped Cloud Foundry application module found for: {0} - Unable to deploy or start application";

	public static final String ERROR_FAILED_TO_PUSH_APP = "Failed to push application - {0}";

	public static final String ERROR_NO_CLOUD_APPLICATION_FOUND = "No cloud module specified when attempting to update application instance stats.";

	public static final String ERROR_NO_MODULES_TO_PUBLISH = "Publish request failed. No modules to publish.";

	public static final String ERROR_NO_CLIENT = "No Cloud Foundry client available to process the following request - {0} ";

	/*
	 * Warnings
	 */
	public static final String WARNING_SELF_SIGNED_PROMPT_USER = "Failed to connect to {0}, probably because the site is using a self-signed certificate. Do you want to trust this site anyway?";

	/*
	 * Titles
	 */

	public static final String TITLE_SELF_SIGNED_PROMPT_USER = "Failed to connect";

	/*
	 * Console messages
	 */
	public static final String CONSOLE_ERROR_MESSAGE = "Error: {0}";

	public static final String CONSOLE_RESTARTING_APP = "Restarting application";

	public static final String CONSOLE_DEPLOYING_APP = "Starting application operation";

	public static final String CONSOLE_GENERATING_ARCHIVE = "Generating application archive";

	public static final String CONSOLE_APP_STOPPED = "Application stopped";

	public static final String CONSOLE_PRE_STAGING_MESSAGE = "Staging application";

	public static final String CONSOLE_APP_PUSH_MESSAGE = "Pushing application to Cloud Foundry server";

	public static final String CONSOLE_APP_CREATION = "Creating application in Cloud Foundry server";

	public static final String CONSOLE_APP_FOUND = "Existing application found in Cloud Foundry server";

	public static final String CONSOLE_APP_MAPPING_STARTED = "Updating application mapping";

	public static final String CONSOLE_APP_MAPPING_COMPLETED = "Application mapping updated";

	public static final String CONSOLE_APP_PUSHED_MESSAGE = "Application successfully pushed to Cloud Foundry server";

	public static final String CONSOLE_PREPARING_APP = "Checking application - {0}";

	public static final String CONSOLE_STILL_WAITING_FOR_APPLICAITON_TO_START = "Still waiting for applicaiton to start...";

	public static final String CONSOLE_WAITING_FOR_APPLICATION_TO_START = "Waiting for application to start...";

	public static final String CONSOLE_STOPPING_APPLICATION = "Stopping application - {0}";

	/*
	 * Jobs
	 */

	public static final String REFRESHING_MODULES = "Initialising and refreshing modules for - {0}";

	public static final String PUBLISHING_MODULE = "Publishing module - {0}";

	public static final String DELETING_MODULE = "Deleting module - {0}";

	public static final String VALIDATING_CREDENTIALS = "Validating credentials";
	
	
	/*
	 * Labels
	 * 
	 */
	
	public static final String VALID_ACCOUNT = "Account information is valid.";

}
