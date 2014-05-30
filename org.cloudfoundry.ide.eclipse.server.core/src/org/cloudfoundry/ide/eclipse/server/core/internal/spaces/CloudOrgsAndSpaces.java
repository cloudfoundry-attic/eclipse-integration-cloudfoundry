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
package org.cloudfoundry.ide.eclipse.server.core.internal.spaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.core.runtime.CoreException;

/**
 * Given a list of CloudSpaces, this will parse and cache all the orgs in the
 * CloudSpaces, as well as provide mechanisms to find a list of CloudSpaces per
 * organization.
 * 
 */
public class CloudOrgsAndSpaces {

	private final List<CloudSpace> originalSpaces;

	private Map<String, List<CloudSpace>> orgIDtoSpaces;

	private Map<String, CloudOrganization> orgIDtoOrg;

	/**
	 * 
	 * @param spaces a flat list of all spaces for a given set of credentials
	 * and server URL. Should not be empty or null.
	 * @throws CoreException if given cloud server does not support orgs and
	 * spaces
	 */
	public CloudOrgsAndSpaces(List<CloudSpace> spaces) {
		this.originalSpaces = spaces;
		setValues();
	}

	public CloudSpace getSpace(String orgName, String spaceName) {
		List<CloudSpace> oSpaces = orgIDtoSpaces.get(orgName);
		if (oSpaces != null) {
			for (CloudSpace clSpace : oSpaces) {
				if (clSpace.getName().equals(spaceName)) {
					return clSpace;
				}
			}
		}
		return null;
	}

	public List<CloudOrganization> getOrgs() {

		Collection<CloudOrganization> orgList = orgIDtoOrg.values();
		return new ArrayList<CloudOrganization>(orgList);
	}

	protected void setValues() {
		orgIDtoSpaces = new HashMap<String, List<CloudSpace>>();
		orgIDtoOrg = new HashMap<String, CloudOrganization>();
		// Parse the orgs and restructure the spaces per org for quick lookup,
		// as the original list of spaces is flat and does
		// not convey the org -> spaces structure.
		for (CloudSpace clSpace : originalSpaces) {
			CloudOrganization org = clSpace.getOrganization();
			List<CloudSpace> spaces = orgIDtoSpaces.get(org.getName());
			if (spaces == null) {
				spaces = new ArrayList<CloudSpace>();
				orgIDtoSpaces.put(org.getName(), spaces);
				orgIDtoOrg.put(org.getName(), org);
			}

			spaces.add(clSpace);
		}
	}

	/**
	 * @param orgName
	 * @return
	 */
	public List<CloudSpace> getOrgSpaces(String orgName) {
		return orgIDtoSpaces.get(orgName);
	}

	public CloudSpace getDefaultCloudSpace() {
		// Select the first space as the default space
		if (originalSpaces != null && originalSpaces.size() > 0) {
			return originalSpaces.get(0);
		}
		return null;
	}

	/**
	 * @return all spaces available for the given account. Never null, although
	 * may be empty if no spaces are resolved.
	 */
	public List<CloudSpace> getAllSpaces() {
		return originalSpaces != null ? new ArrayList<CloudSpace>(originalSpaces) : new ArrayList<CloudSpace>(0);
	}
}
