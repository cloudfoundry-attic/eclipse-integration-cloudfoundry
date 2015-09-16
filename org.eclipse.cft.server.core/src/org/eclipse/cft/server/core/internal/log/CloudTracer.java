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
package org.eclipse.cft.server.core.internal.log;

import org.cloudfoundry.client.lib.RestLogEntry;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;

/**
 * Cloud tracer that fires trace events when a trace request is received from
 * the Cloud Foundry trace framework.
 *
 */
public abstract class CloudTracer implements ICloudTracer {

	public void traceNewLogEntry(RestLogEntry restLogEntry) {

		if (restLogEntry == null || !HttpTracer.getCurrent().isEnabled()) {
			return;
		}
		doTrace(restLogEntry);
	}

	/**
	 * 
	 * @param restLogEntry non-null log entry, invoked only when tracing is
	 * enabled.
	 */
	abstract void doTrace(RestLogEntry restLogEntry);

	/**
	 * Utility method for tracing a message based on a {@link LogContentType}.
	 * When invoked, it will notify listeners of a trace event.
	 * @param message if null, nothing is traced.
	 * @param type of trace. This allows listeners to perform a specific type of
	 * operation on the trace message.
	 */
	protected void fireTraceEvent(CloudLog log) {
		if (log == null) {
			return;
		}

		try {
			CloudFoundryPlugin.getCallback().trace(log, false);
		}
		catch (Throwable t) {
			// Failure in tracing. Catch as to not prevent any further framework
			// operations.
			CloudFoundryPlugin.logError(t);
		}
	}

	/**
	 * 
	 * @return current cloud tracer used to stream trace content. Should never
	 * be null.
	 */
	public static ICloudTracer getCurrentCloudTracer() {
		// Add option for Framework here to load third-party tracers. For now,
		// just return a default tracer
		return new DefaultCloudTracer();
	}

}
