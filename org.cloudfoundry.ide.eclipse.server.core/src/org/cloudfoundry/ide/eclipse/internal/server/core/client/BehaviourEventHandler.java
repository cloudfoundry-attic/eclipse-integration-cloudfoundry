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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

/**
 * Listeners can be notified when certain types of behaviour events, like
 * starting or stopping an application, are completed.
 * 
 */
public class BehaviourEventHandler {

	private static BehaviourEventHandler handler;

	private Map<BehaviourEventType, List<BehaviourListener>> listenersPerEventType = new HashMap<BehaviourEventType, List<BehaviourListener>>();

	public static BehaviourEventHandler getHandler() {
		if (handler == null) {
			handler = new BehaviourEventHandler();
		}
		return handler;
	}

	public synchronized <T> void notify(CloudFoundryApplicationModule appModule, CloudFoundryServer server, T result,
			BehaviourEventType type) {

		if (type == null) {
			return;
		}

		List<BehaviourListener> listeners = listenersPerEventType.get(type);
		if (listeners != null) {
			BehaviourEvent<T> event = new BehaviourEvent<T>(appModule, server, result, type);
			for (BehaviourListener listener : listeners) {
				listener.handle(event);
			}
		}
	}

	public synchronized void addListener(BehaviourListener listener, BehaviourEventType[] types) {
		if (types == null || listener == null) {
			return;
		}

		for (BehaviourEventType type : types) {
			List<BehaviourListener> listeners = listenersPerEventType.get(type);
			if (listeners == null) {
				listeners = new ArrayList<BehaviourListener>();
				listenersPerEventType.put(type, listeners);
			}
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	public synchronized void removeListener(BehaviourListener listener) {
		for (Entry<BehaviourEventType, List<BehaviourListener>> entry : listenersPerEventType.entrySet()) {
			List<BehaviourListener> listeners = entry.getValue();
			if (listeners != null) {
				listeners.remove(listener);
			}
		}
	}

}
