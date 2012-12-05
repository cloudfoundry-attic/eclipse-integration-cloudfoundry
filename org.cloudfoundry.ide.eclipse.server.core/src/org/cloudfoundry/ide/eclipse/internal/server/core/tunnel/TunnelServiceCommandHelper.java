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
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class TunnelServiceCommandHelper {

	private final static ObjectMapper mapper = new ObjectMapper();

	private final CloudFoundryServer cloudServer;

	public TunnelServiceCommandHelper(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	public ServicesServer parseAndUpdateTunnelServiceCommands(String json, IProgressMonitor monitor)
			throws CoreException {
		ServicesServer servicesServer = null;
		if (json != null) {
			try {
				servicesServer = mapper.readValue(json, ServicesServer.class);
			}
			catch (IOException e) {

				CloudFoundryPlugin.logError("Error while reading Java Map from JSON response: ", e);
			}
		}

		if (servicesServer == null) {
			servicesServer = new ServicesServer();
			servicesServer.setServerID(cloudServer.getServerId());
			servicesServer.setServerName(cloudServer.getServer().getName());
		}

		// Obtain updated list of services
		List<CloudService> services = cloudServer.getBehaviour().getServices(monitor);

		Map<String, ServerService> updatedServerServices = new HashMap<String, ServerService>();

		// Service names are always unique.
		if (services != null) {
			for (CloudService actualService : services) {
				ServerService serService = new ServerService();
				serService.setServiceName(actualService.getName());

				// Handle both v1 and v2 services. For v2, vendors are "labels".
				serService.setVendor(actualService.getLabel() != null ? actualService.getLabel() : actualService
						.getVendor());
				serService.setVersion(actualService.getVersion());
				updatedServerServices.put(serService.getServiceName(), serService);
			}
		}

		List<ServerService> serverServices = servicesServer.getServices();
		if (serverServices != null) {
			for (ServerService existingService : serverServices) {
				// Merge if existing, and skip those those entries that are no
				// longer contained
				// in the list of actual services from the server
				ServerService updatedService = updatedServerServices.get(existingService.getServiceName());
				if (updatedService != null) {
					updatedService.setCommands(existingService.getCommands());
				}
			}
		}

		servicesServer.setServices(new ArrayList<ServerService>(updatedServerServices.values()));

		return servicesServer;
	}

	public String serialiseServerServiceCommands(ServicesServer server) throws CoreException {
		if (mapper.canSerialize(server.getClass())) {
			try {
				return mapper.writeValueAsString(server);
			}
			catch (IOException e) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(
						"Error while serialising Java Map from JSON response: ", e));
			}
		}
		else {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus("Value of type " + server.getClass().getName()
					+ " can not be serialized to JSON."));
		}
	}

}
