/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.core.internal.spaces;

import org.cloudfoundry.client.lib.domain.CloudSpace;

/**
 * Local representation of a Cloud space, used when the cloud space is stored in
 * the WST server. It always contains an org and space name, but it may not
 * always be mapped to an actual client CloudSpace instance (so it may not
 * contain client metadata like GUID).
 * <p/>
 * For example, when start an existing Cloud Foundry WST server instance that
 * has the org and space name stored, a CloudFoundrySpace will be created with
 * this information, but it may not yet be linked to an actual client CloudSpace
 * until a Cloud space lookup is performed by the plugin (for example, when
 * creating a client to connect to the CF remote server).
 * 
 * @see CloudSpace
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
