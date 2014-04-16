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
package org.cloudfoundry.ide.eclipse.internal.server.core;

/**
 * Constants used in validating server credentials.
 */
public class ServerCredentialsValidationStatics {
	public static final int EVENT_CREDENTIALS_FILLED = 1000;

	public static final int EVENT_SPACE_VALID = 1002;

	public static final int EVENT_SPACE_CHANGED = 1003;
	
	public static final int EVENT_SELF_SIGNED_ERROR = 1004;

	public static final int EVENT_NONE = -1;

	public static final String DEFAULT_DESCRIPTION = "Enter credentials to log on to a selected Cloud Foundry server.";

	public static final String VALID_ACCOUNT_MESSAGE = "Account information is valid.";

}
