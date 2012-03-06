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

import org.eclipse.rse.subsystems.files.core.servicesubsystem.AbstractRemoteFile;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileContext;

/**
 * @author Leo Dos Santos
 */
public class CloudFoundryRemoteFile extends AbstractRemoteFile {

	private CloudFoundryHostFile hostFile;

	public CloudFoundryRemoteFile(FileServiceSubSystem subSystem, IRemoteFileContext context, IRemoteFile parent,
			CloudFoundryHostFile hostFile) {
		super(subSystem, context, parent, hostFile);
		this.hostFile = hostFile;
	}

	public String getCanonicalPath() {
		return getAbsolutePath();
	}

	public String getClassification() {
		return hostFile.getClassification();
	}

}
