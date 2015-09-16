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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.RestLogEntry;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Enables HTTP tracing for Cloud Foundry requests via a
 * {@link CloudFoundryOperations}.
 *
 */
public class HttpTracer {

	private static HttpTracer currentTracer;

	public static final String PREFERENCE_TRACE = CloudFoundryPlugin.PLUGIN_ID + ".http.tracing"; //$NON-NLS-1$

	private RestLogCallback activeListener = null;

	private static boolean isEnabled;

	public static synchronized HttpTracer getCurrent() {
		if (currentTracer == null) {
			currentTracer = new HttpTracer();
			isEnabled = currentTracer.loadTracePreference();
		}
		return currentTracer;
	}

	/**
	 * Trace requests in the given client. Generally, this will either register
	 * a trace listener to the client, if tracing is enabled, or unregister a
	 * trace listener in the client if tracing is disabled.
	 * <p/>
	 * The trace operation manages the listeners being added, in particular it
	 * prevents many listener instances from being added to the same client,
	 * therefore trace can be invoke many times (for example, prior to
	 * performing a client call) without having to worry about a new listener
	 * being added on every call.
	 * @param client whose HTTP requests need to be traced. Must not be null
	 */
	public synchronized void trace(CloudFoundryOperations client) {
		if (client == null) {
			return;
		}
		// To prevent many listeners from being registered in the same client,
		// always unregister the listener, even when enabling tracing. This is a
		// work-around
		// as the client does not have API to check if a listener is already
		// registered.
		if (activeListener != null) {
			client.unRegisterRestLogListener(activeListener);
		}
		if (isEnabled()) {
			if (activeListener == null) {
				activeListener = new PrintingApplicationLogListener();
			}
			client.registerRestLogListener(activeListener);
		}
	}

	/**
	 * 
	 * @param enable true if tracing should be enabled for all clients/all
	 * servers. False otherwise.
	 */
	public synchronized void enableTracing(boolean enable) {
		isEnabled = enable;
		IEclipsePreferences prefs = CloudFoundryPlugin.getDefault().getPreferences();
		prefs.putBoolean(PREFERENCE_TRACE, isEnabled);
		try {
			prefs.flush();
		}
		catch (BackingStoreException e) {
			CloudFoundryPlugin.logError(e);
		}

		if (isEnabled) {
			CloudFoundryPlugin.getCallback().showTraceView(isEnabled);
		}
	}

	/**
	 * 
	 * @return true if tracing is enabled for all clients and all servers. False
	 * otherwise.
	 */
	public synchronized boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Enables tracing based on the stored preference. It also initialises any
	 * other components that need to be available when tracing is enabled.
	 * @return true if tracing is enabled in the preference store. False
	 * otherwise.
	 */
	public synchronized boolean loadTracePreference() {
		isEnabled = CloudFoundryPlugin.getDefault().getPreferences().getBoolean(PREFERENCE_TRACE, false);
		// When loading from preference store, if tracing is enabled, also show
		// the tracing view (e.g. a console)
		if (isEnabled) {
			CloudFoundryPlugin.getCallback().showTraceView(isEnabled);
		}
		return isEnabled;
	}

	public static class PrintingApplicationLogListener implements RestLogCallback {

		public PrintingApplicationLogListener() {
		}

		public void onNewLogEntry(RestLogEntry restLogEntry) {
			CloudTracer.getCurrentCloudTracer().traceNewLogEntry(restLogEntry);
		}
	}

}
