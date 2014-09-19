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

import org.cloudfoundry.caldecott.client.TunnelServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.TunnelBehaviour;

public class CaldecottTunnelDescriptor {

	private final String userName;

	private final String password;

	private final int tunnelPort;

	private final TunnelServer server;

	private final String serviceName;

	private final String serviceVendor;

	private final String databaseName;

	public CaldecottTunnelDescriptor(String userName, String password, String databaseName, String serviceName,
			String serviceVendor, TunnelServer server, int tunnelPort) {
		this.server = server;
		this.userName = userName;
		this.password = password;
		this.tunnelPort = tunnelPort;
		this.serviceName = serviceName;
		this.serviceVendor = serviceVendor;
		this.databaseName = databaseName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getServiceVendor() {
		return serviceVendor;
	}

	public String getServiceName() {
		return serviceName;
	}

	public int tunnelPort() {
		return tunnelPort;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	protected ServiceVendor getVendor() {
		String serviceVendorName = getServiceVendor();
		if (serviceVendorName != null) {
			for (ServiceVendor vendor : ServiceVendor.values()) {
				if (serviceVendorName.equals(vendor.name())) {
					return vendor;
				}
			}
		}
		return null;
	}

	public String getURL() {
		ServiceVendor vendor = getVendor();
		if (vendor != null) {
			StringBuilder builtURL = null;
			switch (vendor) {
			case mysql:
				builtURL = new StringBuilder();
				builtURL.append("jdbc:mysql"); //$NON-NLS-1$
				builtURL.append("://"); //$NON-NLS-1$
				builtURL.append(TunnelBehaviour.LOCAL_HOST);
				builtURL.append(":"); //$NON-NLS-1$
				builtURL.append(tunnelPort());
				if (getDatabaseName() != null) {
					builtURL.append("/"); //$NON-NLS-1$
					builtURL.append(getDatabaseName());
				}

				break;
			// FIXNS: enable when corresponding test case is available
			// case mongodb:
			// builtURL = new StringBuilder();
			// builtURL.append("mongodb");
			// builtURL.append("://");
			// builtURL.append(CaldecottTunnelHandler.LOCAL_HOST);
			// builtURL.append(":");
			// builtURL.append(tunnelPort());
			// if (getDatabaseName() != null) {
			// builtURL.append("/");
			// builtURL.append(getDatabaseName());
			// }
			//
			// break;
			case postgresql:
				builtURL = new StringBuilder();
				builtURL.append("jdbc:postgresql"); //$NON-NLS-1$
				builtURL.append("://"); //$NON-NLS-1$
				builtURL.append(TunnelBehaviour.LOCAL_HOST);
				builtURL.append(":"); //$NON-NLS-1$
				builtURL.append(tunnelPort());
				if (getDatabaseName() != null) {
					builtURL.append("/"); //$NON-NLS-1$
					builtURL.append(getDatabaseName());
				}
				break;

			}

			if (builtURL != null) {
				return builtURL.toString();
			}
		}
		return null;
	}

	public TunnelServer getTunnelServer() {
		return server;
	}

	public enum ServiceVendor {
		postgresql, mysql, mongodb
	}
}
