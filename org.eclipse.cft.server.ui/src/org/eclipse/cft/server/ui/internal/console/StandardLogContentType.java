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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.log.LogContentType;

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
	 * Generic application log type that is used for both loggregator and file
	 * log stream. NOTE: May be deprecated once file log streaming is removed.
	 */
	public static final LogContentType APPLICATION_LOG = new LogContentType("applicationlog"); //$NON-NLS-1$

	/**
	 * @deprecated only used by file log streaming, which is no longer supported.
	 */
	public static final LogContentType SHOW_EXISTING_LOGS = new LogContentType("existingLogs"); //$NON-NLS-1$

}
