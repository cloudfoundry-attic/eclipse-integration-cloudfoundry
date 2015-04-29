/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

/**
 * JSON for server property contain URL, org and space
 *
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, creatorVisibility = Visibility.NONE)
public class ServerProperty {

	private String url;

	private String org;

	private String space;
	
	public ServerProperty() {
		
	}

	public ServerProperty(String url, String org, String space) {
		this.url = url;
		this.org = org;
		this.space = space;
	}

	public String getUrl() {
		return url;
	}

	public String getOrg() {
		return org;
	}

	public String getSpace() {
		return space;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((org == null) ? 0 : org.hashCode());
		result = prime * result + ((space == null) ? 0 : space.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerProperty other = (ServerProperty) obj;
		if (org == null) {
			if (other.org != null)
				return false;
		}
		else if (!org.equals(other.org))
			return false;
		if (space == null) {
			if (other.space != null)
				return false;
		}
		else if (!space.equals(other.space))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		}
		else if (!url.equals(other.url))
			return false;
		return true;
	}

}