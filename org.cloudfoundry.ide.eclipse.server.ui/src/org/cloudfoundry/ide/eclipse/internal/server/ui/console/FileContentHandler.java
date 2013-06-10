/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.io.OutputStream;

import org.cloudfoundry.ide.eclipse.internal.server.core.FileContent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Given a file content in a remote Cloud server, retrieves the content starting
 * from a particular offset in the file. This allows portions of file contents
 * to be fetched, rather than fetching the entire file content all the time, and
 * is meant to reduce amount of network I/O.
 * 
 * The handler incrementally keeps track of the offset, up until the offset is
 * reset.
 * 
 */
public class FileContentHandler {
	protected int offset = 0;

	protected final FileContent fileContent;

	protected final OutputStream outputStream;

	protected int instanceIndex;

	protected String appName;

	public FileContentHandler(FileContent fileContent, OutputStream outputStream, String appName, int instanceIndex) {
		this.fileContent = fileContent;
		this.outputStream = outputStream;
		this.appName = appName;
		this.instanceIndex = instanceIndex;
	}

	public String getContent(IProgressMonitor monitor) throws CoreException {
		String content = fileContent.getContent(appName, instanceIndex, outputStream, offset, monitor);
		if (content != null) {
			offset += content.length();
		}
		return content;
	}

	public int getOffset() {
		return offset;
	}

	public void reset() {
		offset = 0;
	}
}