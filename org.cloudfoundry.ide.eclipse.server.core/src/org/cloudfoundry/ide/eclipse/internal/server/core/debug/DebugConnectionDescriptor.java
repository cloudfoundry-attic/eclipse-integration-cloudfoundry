/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

public class DebugConnectionDescriptor {

	private final String ip;

	private final int port;

	public DebugConnectionDescriptor(String ip, int port) {

		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public boolean areValidIPandPort() {
		return ip != null && ip.length() > 0 && port > 0;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DebugConnectionDescriptor other = (DebugConnectionDescriptor) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		}
		else if (!ip.equals(other.ip))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

}