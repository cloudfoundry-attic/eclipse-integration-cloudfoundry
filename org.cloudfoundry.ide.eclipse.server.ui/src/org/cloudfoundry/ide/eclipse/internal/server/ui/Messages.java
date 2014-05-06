/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

public class Messages {

	/*
	 * Errors
	 */
	public static final String ERROR_NO_VALIDATOR_PRESENT = "Internal Error: No validator present to validate credentials. Please check with technical support.";

	public static final String ERROR_NO_CALLBACK_UNABLE_TO_REFRESH_CONSOLE = "No Cloud Foundry console callback available. Unable to refresh console contents.";

	public static final String ERROR_NO_CLOUD_SPACE_SELECTED = "Please select a valid cloud space.";

	public static final String ERROR_SERVER_INSTANCE_CLOUD_SPACE_EXISTS = "A Cloud Foundry server instance ({0}) for space: {1} - already exists. Please select another cloud space if available.";

	public static final String ERROR_ALL_SPACES_ASSOCIATED_SERVER_INSTANCES = "A server instance for each space already exists. Please delete the existing server instance to the target space before creating a new one.";

	public static final String ERROR_CHECK_CONNECTION_NO_SPACES = "No cloud spaces found. Please check your credentials or connection.";

	/*
	 * UI Labels
	 */

	public static final String LABEL_MEMORY_LIMIT = "Memory Limit (MB):";

	public static final String LABEL_ENABLE_TRACING = "HTTP Tracing";

	public static final String TOOLTIP_ENABLE_TRACING = "Enables HTTP Tracing in a Cloud Foundry tracing console";

	/*
	 * Dialogue messages
	 */
	public static final String SERVER_WIZARD_VALIDATOR_CLICK_TO_VALIDATE = "Press 'Validate Account' or 'Next' to validate credentials.";

	public static final String SHOWING_CONSOLE = "Fetching console contents. Please wait...\n";

}
