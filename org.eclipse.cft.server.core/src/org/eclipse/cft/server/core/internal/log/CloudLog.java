/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Apache License, Version 2.0 (the "License�);
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors: Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.log;

/**
 * A log message that contains the message and message type. This may be used to
 * model both local log messages (for example writing to local standard out) or
 * an actual loggregator application log for a published app. The
 * {@link LogContentType} indicates whether it is a local log or a log from a
 * published application that is being streamed to a console.
 *
 */
public class CloudLog {

	private final LogContentType logType;

	private final String message;

	public CloudLog(String message, LogContentType logType) {
		this.message = message;
		this.logType = logType;
	}

	public LogContentType getLogType() {
		return logType;
	}

	public String getMessage() {
		return message;
	}

	public String toString() {
		return logType + " - " + message; //$NON-NLS-1$
	}

}
