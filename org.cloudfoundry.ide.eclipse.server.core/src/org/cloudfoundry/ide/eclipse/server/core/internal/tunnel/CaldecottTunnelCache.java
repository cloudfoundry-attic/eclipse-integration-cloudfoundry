/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.tunnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;

public class CaldecottTunnelCache {

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
	 * Returns a copy of all descriptors for the given server, or null if no
	 * tunnels have been opened for the server yet.
	 * @param server
	 * @return list of tunnel descriptors, or null if non exist.
	 */
	public synchronized Collection<CaldecottTunnelDescriptor> getDescriptors(CloudFoundryServer server) {
		String id = server.getServerId();
		Map<String, CaldecottTunnelDescriptor> descriptors = caldecottTunnels.get(id);
		if (descriptors != null) {
			return new ArrayList<CaldecottTunnelDescriptor>(descriptors.values());
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

}
