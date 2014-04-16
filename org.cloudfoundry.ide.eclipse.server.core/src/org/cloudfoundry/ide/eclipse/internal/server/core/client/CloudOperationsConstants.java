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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

public class CloudOperationsConstants {

	/*
	 * Intervals are how long a thread should sleep before moving to the next
	 * iteration, or how long a refresh operation should wait before refreshing
	 * the deployed apps.
	 */
	public static final long DEFAULT_INTERVAL = 60 * 1000;

	public static final long SHORT_INTERVAL = 5 * 1000;

	public static final long MEDIUM_INTERVAL = 10 * 1000;

	public static final long ONE_SECOND_INTERVAL = 1000;

	public static final long LOGIN_INTERVAL = 2000;

	public static final long DEPLOYMENT_TIMEOUT = 10 * 60 * 1000;

	public static final long UPLOAD_TIMEOUT = 60 * 1000;

	public static final long DEFAULT_CF_CLIENT_REQUEST_TIMEOUT = 15 * 1000;
}
