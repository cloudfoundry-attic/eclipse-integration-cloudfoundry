/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
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
	 * and server URL.
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
}
