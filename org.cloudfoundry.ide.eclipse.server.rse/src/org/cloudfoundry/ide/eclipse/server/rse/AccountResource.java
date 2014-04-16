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
package org.cloudfoundry.ide.eclipse.server.rse;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class AccountResource extends CloudFoundryHostFile {

	private CloudFoundryServer server;

	private List<ApplicationResource> applications;

	public AccountResource(CloudFoundryServer server) {
		super();
		this.server = server;
		applications = new ArrayList<ApplicationResource>();
	}

	public boolean canRead() {
		return true;
	}

	public boolean canWrite() {
		return false;
	}

	public boolean exists() {
		return true;
	}

	/**
	 * Fetches the list of children over the network. Do not call this method
	 * from a UI thread, call {@link #getChildren(IProgressMonitor)} instead.
	 * 
	 * @param monitor
	 * @return
	 */
	public List<ApplicationResource> fetchChildren(IProgressMonitor monitor) {
		if (!applications.isEmpty()) {
			applications.clear();
		}
		try {
			List<CloudApplication> cloudApps = server.getBehaviour().getApplications(monitor);
			for (CloudApplication cloudApp : cloudApps) {
				int count = cloudApp.getInstances();
				for (int i = 0; i < count; i++) {
					ApplicationResource resource = new ApplicationResource(server, cloudApp, i);
					applications.add(resource);
				}
			}
		}
		catch (CoreException e) {
			CloudFoundryRsePlugin.logError("An error occurred while retrieving applications.", e);
		}
		return applications;
	}

	public String getAbsolutePath() {
		return server.getServer().getName() + "@" + server.getUrl();
	}

	/**
	 * Returns the cached list of children.
	 * 
	 * @param monitor
	 * @return
	 */
	public List<ApplicationResource> getChildren(IProgressMonitor monitor) {
		return applications;
	}

	@Override
	public String getClassification() {
		return "account";
	}

	public long getModifiedDate() {
		return 0;
	}

	public String getName() {
		return server.getServer().getName();
	}

	public String getParentPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public CloudFoundryServer getServer() {
		return server;
	}

	public long getSize() {
		return 0;
	}

	public boolean isArchive() {
		return false;
	}

	public boolean isDirectory() {
		return true;
	}

	public boolean isFile() {
		return false;
	}

	public boolean isHidden() {
		return false;
	}

	public boolean isRoot() {
		return true;
	}

	public void renameTo(String newAbsolutePath) {
		// TODO Auto-generated method stub

	}

}
