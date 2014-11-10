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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;

public class StandardLogContentType {

	/**
	 * Local std out. May be different that an application's log std out from
	 * the Cloud Foundry server. This allows local std out (for example, local
	 * progress messages) to be distinct from remote std out logs.
	 */

	public static final LogContentType STD_OUT = new LogContentType("stdout"); //$NON-NLS-1$

	/**
	 * Local std error. May be different that an application's log std error
	 * from the Cloud Foundry server. This allows local std error (for example,
	 * exceptions thrown locally) to be distinct from remote std error logs.
	 */
	public static final LogContentType STD_ERROR = new LogContentType("stderror"); //$NON-NLS-1$

	/**
	 * Application log errors that occur while the application is deployed in
	 * the Cloud Foundry server.
	 */
	public static final LogContentType APPLICATION_LOG_STS_ERROR = new LogContentType("applicationlogstderror"); //$NON-NLS-1$

	/**
	 * Application log std out messages that occur while the application is
	 * deployed in the Cloud Foundr server.
	 */
	public static final LogContentType APPLICATION_LOG_STD_OUT = new LogContentType("applicationlogstdout"); //$NON-NLS-1$

	/**
	 * Other application log content obtained while the app is deployed to a
	 * Cloud Foundry server that is neither std out or std error.
	 */
	public static final LogContentType APPLICATION_LOG_UNKNOWN = new LogContentType("applicationlogunknown"); //$NON-NLS-1$

	/**
	 * Generic application log type that is used for both loggregator and file
	 * log stream. NOTE: May be deprecated once file log streaming is removed.
	 */
	public static final LogContentType APPLICATION_LOG = new LogContentType("applicationlog"); //$NON-NLS-1$

	/**
	 * @deprecated only used by file log streaming, which is deprecated
	 */
	public static final LogContentType SHOW_EXISTING_LOGS = new LogContentType("existingLogs"); //$NON-NLS-1$

}
