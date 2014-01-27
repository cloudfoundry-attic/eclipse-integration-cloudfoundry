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
package org.cloudfoundry.ide.eclipse.server.rse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
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
					String blob = server.getBehaviour().getFile(app.getName(), id, parent.substring(1), monitor);
					String[] files = blob.split("\n");
					long timestamp = Calendar.getInstance().getTimeInMillis();
					for (int i = 0; i < files.length; i++) {
						String[] content = files[i].split("\\s+");
						String name = content[0];
						if (name.trim().length() > 0) {
							FileResource resource = new FileResource();
							if (name.endsWith("/")) {
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
			catch (CoreException e) {
				CloudFoundryRsePlugin.logError("An error occurred while retrieving files for application " + app.getName(), e);
			}
		}
		return list;
	}
}
