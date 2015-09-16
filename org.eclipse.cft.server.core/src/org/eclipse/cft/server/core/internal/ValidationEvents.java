/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal;

public class ValidationEvents {

	/**
	 * Indicates that account values (e.g. username, password, server URL, cloud
	 * space), has been filled locally. This should only trigger local
	 * credential validation, not a remote server authorization or cloud space
	 * validation.
	 */
	public static final int CREDENTIALS_FILLED = 1000;

	/**
	 * Indicates a self-signed server has been detected
	 */
	public static final int SELF_SIGNED = 1001;

	/**
	 * Indicates an event where remote server authorisation of credentials is
	 * required. This is specialisation of {@link #VALIDATION} in the sense an
	 * explicit server authorisation is required, and therefore has higher
	 * priority than a {@link #VALIDATION} event
	 */
	public static final int SERVER_AUTHORISATION = 1002;

	/**
	 * Indicates that validation has been requested or completed. The reason
	 * that the same event is used to both indicate a validation request as well
	 * as a completion is that
	 */
	public static final int VALIDATION = 1003;

	public static final int EVENT_NONE = -1;
}
