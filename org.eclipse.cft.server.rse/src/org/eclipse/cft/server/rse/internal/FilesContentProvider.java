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
package org.eclipse.cft.server.rse.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class FilesContentProvider {

	private CloudApplication app;

	private int id;

	private CloudFoundryServer server;

	public FilesContentProvider(CloudFoundryServer server, CloudApplication app, int id) {
		this.app = app;
		this.server = server;
		this.id = id;
	}

	public List<FileResource> getElements(Object inputElement, IProgressMonitor monitor) {
		List<FileResource> list = new ArrayList<FileResource>();
		if (inputElement instanceof String) {
			String parent = (String) inputElement;
			try {
				if (AppState.STARTED.equals(app.getState())) {
					String path = parent.substring(1);
					String blob = server.getBehaviour().getFile(app.getName(), id, path, monitor);
					if (blob != null) {
						String[] files = blob.split("\n"); //$NON-NLS-1$
						long timestamp = Calendar.getInstance().getTimeInMillis();
						for (int i = 0; i < files.length; i++) {
							String[] content = files[i].split("\\s+"); //$NON-NLS-1$
							String name = content[0];
							if (name.trim().length() > 0) {
								FileResource resource = new FileResource();
								if (name.endsWith("/")) { //$NON-NLS-1$
									resource.setIsDirectory(true);
									resource.setIsFile(false);
									name = name.substring(0, name.length() - 1);
								}
								resource.setName(name);
								resource.setModifiedDate(timestamp);
								String parentPath = ApplicationResource.getAbsolutePath(app, id + parent);
								resource.setParentPath(parentPath);
								resource.setAbsolutePath(parentPath.concat(content[0]));
								if (content.length > 1) {
									resource.setSize(content[1]);
								}
								list.add(resource);
							}
						}
					}
				}
			}
			catch (CoreException e) {
				CloudFoundryRsePlugin.logError(
						"An error occurred while retrieving files for application " + app.getName(), e); //$NON-NLS-1$
			}
		}
		return list;
	}
}
