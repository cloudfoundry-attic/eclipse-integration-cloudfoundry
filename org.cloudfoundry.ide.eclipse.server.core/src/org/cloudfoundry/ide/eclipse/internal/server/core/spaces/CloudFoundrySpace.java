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

import org.cloudfoundry.client.lib.domain.CloudSpace;

/**
 * A cloud space should be defined by a org name and space name. In some cases,
 * the actual CloudSpace may not be available, only the org name and space name
 * retrieved as properties from storage, therefore CloudSpace may be optional.
 * @author nierajsingh
 * 
 */
public class CloudFoundrySpace {

	private CloudSpace space;

	private final String spaceName;

	private final String orgName;

	public CloudFoundrySpace(CloudSpace space) {
		this(space.getOrganization().getName(), space.getName());
		this.space = space;
	}

	public CloudFoundrySpace(String orgName, String spaceName) {
		this.orgName = orgName;
		this.spaceName = spaceName;

	}

	public String getOrgName() {
		return orgName;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public CloudSpace getSpace() {
		return this.space;
	}

}
