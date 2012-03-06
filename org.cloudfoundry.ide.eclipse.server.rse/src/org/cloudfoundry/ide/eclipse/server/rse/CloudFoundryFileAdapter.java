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
package org.cloudfoundry.ide.eclipse.server.rse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.AbstractRemoteFile;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IHostFileToRemoteFileAdapter;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileContext;

/**
 * @author Leo Dos Santos
 */
public class CloudFoundryFileAdapter implements IHostFileToRemoteFileAdapter {

	public AbstractRemoteFile convertToRemoteFile(FileServiceSubSystem ss, IRemoteFileContext context,
			IRemoteFile parent, IHostFile node) {
		CloudFoundryRemoteFile file = new CloudFoundryRemoteFile(ss, context, parent, (CloudFoundryHostFile) node);
		ss.cacheRemoteFile(file);
		return file;
	}

	public AbstractRemoteFile[] convertToRemoteFiles(FileServiceSubSystem ss, IRemoteFileContext context,
			IRemoteFile parent, IHostFile[] nodes) {
		List<CloudFoundryRemoteFile> results = new ArrayList<CloudFoundryRemoteFile>();
		if (nodes != null) {
			for (int i = 0; i < nodes.length; i++) {
				CloudFoundryHostFile node = (CloudFoundryHostFile) nodes[i];
				CloudFoundryRemoteFile remote = new CloudFoundryRemoteFile(ss, context, parent, node);
				results.add(remote);
				ss.cacheRemoteFile(remote);
			}
		}
		return results.toArray(new CloudFoundryRemoteFile[results.size()]);
	}

}
