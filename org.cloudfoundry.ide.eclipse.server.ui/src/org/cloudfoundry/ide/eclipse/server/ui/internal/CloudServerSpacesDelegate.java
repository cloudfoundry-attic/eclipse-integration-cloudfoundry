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
 *     
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudSpacesDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableContext;

/**
 * Handles cloud space descriptor updates and also sets a cloud space in a given
 * cloud server, including a default cloud space.
 * <p/>
 * Note that cloud server changes are not saved. It is up to the invoker to
 * decide when to save changes to the cloud server.
 *
 */
public class CloudServerSpacesDelegate extends CloudSpacesDelegate {

	public CloudServerSpacesDelegate(CloudFoundryServer cloudServer) {
		super(cloudServer);
	}

	protected CloudSpacesDescriptor internalUpdateDescriptor(String urlText, String userName, String password,
			boolean selfSigned, IRunnableContext context) throws CoreException {
		CloudSpacesDescriptor spacesDescriptor = super.internalUpdateDescriptor(urlText, userName, password,
				selfSigned, context);
		internalDescriptorChanged();

		return spacesDescriptor;

	}

	/**
	 * Invoked if the descriptor containing list of orgs and spaces has changed.
	 * If available, a default space will be set in the server
	 */
	protected void internalDescriptorChanged() throws CoreException {
		// Set a default space, if one is available
		if (getCurrentSpacesDescriptor() != null) {
			CloudSpace defaultCloudSpace = getSpaceWithNoServerInstance();
			setSelectedSpace(defaultCloudSpace);
		}
		else {

			// clear the selected space if there is no available spaces
			// descriptor
			setSelectedSpace(null);
		}
	}

	public void setSelectedSpace(CloudSpace selectedCloudSpace) {
		if (hasSpaceChanged(selectedCloudSpace)) {
			// Only set space if a change has occurred. 
			getCloudServer().setSpace(selectedCloudSpace);
		}

	}

	protected boolean hasSpaceChanged(CloudSpace selectedCloudSpace) {
		CloudFoundrySpace existingSpace = getCloudServer().getCloudFoundrySpace();
		return !matchesExisting(selectedCloudSpace, existingSpace);
	}

	@Override
	public boolean hasSpace() {
		return getCloudServer().hasCloudSpace();
	}

	public CloudSpace getCurrentCloudSpace() {
		return getCloudServer().getCloudFoundrySpace() != null ? getCloudServer().getCloudFoundrySpace().getSpace()
				: null;
	}

}
