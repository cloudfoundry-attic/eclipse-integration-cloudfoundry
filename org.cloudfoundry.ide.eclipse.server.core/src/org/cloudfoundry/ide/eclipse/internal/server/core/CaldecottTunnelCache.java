/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CaldecottTunnelCache {

	public static final int PORT_BASE = 10000;

	private Map<String, Map<String, CaldecottTunnelDescriptor>> caldecottTunnels = new HashMap<String, Map<String, CaldecottTunnelDescriptor>>();

	public synchronized CaldecottTunnelDescriptor getDescriptor(CloudFoundryServer server, String serviceName) {
		String id = server.getServerId();
		Map<String, CaldecottTunnelDescriptor> descriptors = caldecottTunnels.get(id);
		if (descriptors != null) {
			return descriptors.get(serviceName);
		}
		return null;
	}

	/**
	 * Returns a unmodifiable copy of all descriptors for the given server, or
	 * null if no tunnels have been opened for the server yet.
	 * @param server
	 * @return unmodifiable list of tunnel descriptors, or null if non exist.
	 */
	public synchronized Collection<CaldecottTunnelDescriptor> getDescriptors(CloudFoundryServer server) {
		String id = server.getServerId();
		Map<String, CaldecottTunnelDescriptor> descriptors = caldecottTunnels.get(id);
		if (descriptors != null) {
			return Collections.unmodifiableCollection(descriptors.values());
		}
		return null;
	}

	public synchronized CaldecottTunnelDescriptor removeDescriptor(CloudFoundryServer server, String serviceName) {
		String id = server.getServerId();
		Map<String, CaldecottTunnelDescriptor> descriptors = caldecottTunnels.get(id);
		if (descriptors != null) {
			CaldecottTunnelDescriptor descr = descriptors.remove(serviceName);
			return descr;
		}
		return null;
	}

	public synchronized void addDescriptor(CloudFoundryServer server, CaldecottTunnelDescriptor descriptor) {
		String id = server.getServerId();
		Map<String, CaldecottTunnelDescriptor> descriptors = caldecottTunnels.get(id);
		if (descriptors == null) {

			descriptors = new HashMap<String, CaldecottTunnelDescriptor>();
			caldecottTunnels.put(id, descriptors);
		}
		descriptors.put(descriptor.getServiceName(), descriptor);
	}

	public synchronized int getUnusedPort() {
		int base = PORT_BASE;

		Set<Integer> used = new HashSet<Integer>();
		for (Entry<String, Map<String, CaldecottTunnelDescriptor>> entry : caldecottTunnels.entrySet()) {
			for (Entry<String, CaldecottTunnelDescriptor> descEntry : entry.getValue().entrySet()) {
				used.add(descEntry.getValue().tunnelPort());
			}
		}

		while (used.contains(base)) {
			base++;
		}

		return base;
	}
}
