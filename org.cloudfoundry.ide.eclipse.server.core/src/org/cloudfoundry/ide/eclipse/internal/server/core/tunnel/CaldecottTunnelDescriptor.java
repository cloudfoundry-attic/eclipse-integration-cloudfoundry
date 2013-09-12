/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import org.cloudfoundry.caldecott.client.TunnelServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;

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
				builtURL.append("jdbc:mysql");
				builtURL.append("://");
				builtURL.append(TunnelBehaviour.LOCAL_HOST);
				builtURL.append(":");
				builtURL.append(tunnelPort());
				if (getDatabaseName() != null) {
					builtURL.append("/");
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
				builtURL.append("jdbc:postgresql");
				builtURL.append("://");
				builtURL.append(TunnelBehaviour.LOCAL_HOST);
				builtURL.append(":");
				builtURL.append(tunnelPort());
				if (getDatabaseName() != null) {
					builtURL.append("/");
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
