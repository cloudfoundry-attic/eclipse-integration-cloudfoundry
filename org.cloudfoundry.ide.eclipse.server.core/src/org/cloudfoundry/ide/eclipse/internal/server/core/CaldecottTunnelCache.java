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
import java.util.Set;

public class CaldecottTunnelCache {
	private Map<String, Map<String, CaldecottTunnelDescriptor>> caldecottTunnels = new HashMap<String, Map<String, CaldecottTunnelDescriptor>>();

	private Set<Integer> usedPorts = new HashSet<Integer>();

	public synchronized CaldecottTunnelDescriptor getDescriptor(CloudFoundryServer server, String serviceName) {
		String id = server.getServerId();
		Map<String, CaldecottTunnelDescriptor> descriptors = caldecottTunnels.get(id);
		if (descriptors != null) {
			return descriptors.get(serviceName);
		}
		return null;
	}

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
			if (descr != null) {
				Integer port = new Integer(descr.tunnelPort());
				usedPorts.remove(port);
			}
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
		Integer port = new Integer(descriptor.tunnelPort());
		usedPorts.add(port);
		descriptors.put(descriptor.getServiceName(), descriptor);
	}

	public synchronized int getUnusedPort(int base) {

		boolean contains = true;
		while (contains) {
			contains = usedPorts.contains(new Integer(base));
			if (!contains) {
				return base;
			}
			else {
				base++;
			}
		}

		return -1;

	}
}
