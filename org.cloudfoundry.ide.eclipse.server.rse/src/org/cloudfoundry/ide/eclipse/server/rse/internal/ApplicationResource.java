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
package org.cloudfoundry.ide.eclipse.server.rse.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class ApplicationResource extends CloudFoundryHostFile {

	private CloudApplication app;

	private int id;

	private FilesContentProvider provider;

	private CloudFoundryServer server;

	private Map<String, List<FileResource>> fileMap;

	public ApplicationResource(CloudFoundryServer server, CloudApplication app, int id) {
		super();
		this.app = app;
		this.server = server;
		this.id = id;
		provider = new FilesContentProvider(server, app, id);
		fileMap = new HashMap<String, List<FileResource>>();
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
	 * Fetches the list of children for the given path over the network. Do not
	 * call this method from a UI thread, call
	 * {@link #getChildren(String, IProgressMonitor)} instead.
	 * 
	 * @param path
	 * @param monitor
	 * @return
	 */
	public List<FileResource> fetchChildren(String path, IProgressMonitor monitor) {
		List<FileResource> files = provider.getElements(path, monitor);
		fileMap.put(path, files);
		return files;
	}

	public String getAbsolutePath() {
		return getAbsolutePath(app, id + "");
	}

	public static String getAbsolutePath(CloudApplication app, String id) {
		StringBuilder builder = new StringBuilder();
		List<String> uris = app.getUris();
		if (uris != null && !uris.isEmpty()) {
			builder.append(uris.get(0));
		} else {
			// for standalone, append the app name
			builder.append(app.getName());
		}
		builder.append("/");
		builder.append(id);
		return builder.toString();
	}

	/**
	 * Returns the cached list of children for the given path.
	 * 
	 * @param path
	 * @param monitor
	 * @return
	 */
	public List<FileResource> getChildren(String path, IProgressMonitor monitor) {
		return fileMap.get(path);
	}

	@Override
	public String getClassification() {
		return "application";
	}

	public CloudApplication getCloudApplication() {
		return app;
	}

	public int getInstanceId() {
		return id;
	}

	public long getModifiedDate() {
		return 0;
	}

	public String getName() {
		return app.getName() + "#" + id;
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
		return false;
	}

	public void renameTo(String newAbsolutePath) {
		// TODO Auto-generated method stub

	}

}
