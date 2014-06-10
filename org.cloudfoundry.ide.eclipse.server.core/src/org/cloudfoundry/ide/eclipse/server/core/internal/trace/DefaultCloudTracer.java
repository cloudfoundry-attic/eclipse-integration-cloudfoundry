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
package org.cloudfoundry.ide.eclipse.server.core.internal.trace;

import java.io.StringWriter;

import org.cloudfoundry.client.lib.RestLogEntry;

/**
 * General-purpose tracer that parses a {@link RestLogEntry} into various String
 * traces, and assigns a {@link ITraceType} to each section of the log entry.
 *
 */
public class DefaultCloudTracer extends CloudTracer {

	static final String HTTP_TRACE_STATUS = "HTTP STATUS";

	static final String HTTP_TRACE_REQUEST = "REQUEST";

	static final String ERROR_STATUS = "ERROR";

	static final String TRACE_SEPARATOR = " :: ";

	static final String SPACE = " ";

	protected void doTrace(RestLogEntry restLogEntry) {

		StringWriter writer = new StringWriter();
		boolean isError = restLogEntry.getStatus() != null && ERROR_STATUS.equals(restLogEntry.getStatus());

		writer.append(restLogEntry.getStatus());

		writer.append(SPACE);
		writer.append(TRACE_SEPARATOR);
		writer.append(SPACE);

		writer.append(HTTP_TRACE_STATUS);
		writer.append(':');
		writer.append(SPACE);
		writer.append(restLogEntry.getHttpStatus().name());

		fireTraceEvent(writer.toString(), isError ? TraceType.HTTP_ERROR : TraceType.HTTP_OK);

		writer = new StringWriter();
		writer.append(SPACE);
		writer.append(TRACE_SEPARATOR);
		writer.append(SPACE);
		writer.append(HTTP_TRACE_REQUEST);
		writer.append(':');
		writer.append(SPACE);
		writer.append(restLogEntry.getMethod().toString());

		writer.append(' ');
		writer.append(restLogEntry.getUri().toString());
		writer.append(TRACE_SEPARATOR);
		writer.append(restLogEntry.getMessage());
		writer.append('\n');

		fireTraceEvent(writer.toString(), TraceType.GENERAL);
	}

}
