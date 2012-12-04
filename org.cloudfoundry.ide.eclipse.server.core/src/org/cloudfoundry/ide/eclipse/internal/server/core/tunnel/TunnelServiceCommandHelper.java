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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.CoreException;

public class TunnelServiceCommandHelper {

	private final static ObjectMapper mapper = new ObjectMapper();

	public TunnelServiceCommandHelper() {

	}

	public ServicesServer getTunnelServiceCommands(String json) {
		ServicesServer server = null;
		if (json != null) {
			try {
				server = mapper.readValue(json, ServicesServer.class);
			}
			catch (IOException e) {

				CloudFoundryPlugin.logError("Error while reading Java Map from JSON response: ", e);
			}
		}
		return server;
	}

	public String getTunnelServiceCommands(ServicesServer server) throws CoreException {
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
