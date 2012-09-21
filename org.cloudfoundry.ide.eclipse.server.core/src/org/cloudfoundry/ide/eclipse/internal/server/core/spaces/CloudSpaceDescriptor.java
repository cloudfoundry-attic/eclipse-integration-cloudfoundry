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
package org.cloudfoundry.ide.eclipse.internal.server.core.spaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.core.runtime.CoreException;

/**
 * Given a list of CloudSpaces, this will retrieve all the orgs in the
 * CloudSpaces, as well as provide mechanisms to find a list of CloudSpaces per
 * organization.
 * 
 */
public class CloudSpaceDescriptor {

	private final List<CloudSpace> spaces;

	private final boolean supportsSpaces;

	private Map<String, List<CloudSpace>> orgSpaces;

	private Map<String, CloudOrganization> orgs;

	/**
	 * 
	 * @param cloudServer
	 * @throws CoreException if given cloud server does not support orgs and
	 * spaces
	 */
	public CloudSpaceDescriptor(List<CloudSpace> spaces, boolean supportsSpaces) {
		this.spaces = spaces;
		this.supportsSpaces = supportsSpaces;
		setValues();
	}

	public boolean supportsSpaces() {
		return supportsSpaces;
	}

	public CloudSpace getSpace(String orgName, String spaceName) {
		List<CloudSpace> oSpaces = orgSpaces.get(orgName);
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

		Collection<CloudOrganization> orgList = orgs.values();
		return new ArrayList<CloudOrganization>(orgList);
	}

	protected void setValues() {
		orgSpaces = new HashMap<String, List<CloudSpace>>();
		orgs = new HashMap<String, CloudOrganization>();
		for (CloudSpace clSpace : spaces) {
			CloudOrganization org = clSpace.getOrganization();
			List<CloudSpace> spaces = orgSpaces.get(org.getName());
			if (spaces == null) {
				spaces = new ArrayList<CloudSpace>();
				orgSpaces.put(org.getName(), spaces);
				orgs.put(org.getName(), org);
			}

			spaces.add(clSpace);

		}
	}

	/**
	 * @param orgName
	 * @return
	 */
	public List<CloudSpace> getOrgSpaces(String orgName) {
		return orgSpaces.get(orgName);
	}

	public CloudSpace getDefaultCloudSpace() {
		if (spaces != null && spaces.size() > 0) {
			return spaces.get(0);
		}
		return null;
	}

}
