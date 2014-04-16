/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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