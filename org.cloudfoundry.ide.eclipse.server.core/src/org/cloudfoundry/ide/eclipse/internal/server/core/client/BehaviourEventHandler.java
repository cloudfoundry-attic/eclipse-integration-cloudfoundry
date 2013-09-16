/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
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
