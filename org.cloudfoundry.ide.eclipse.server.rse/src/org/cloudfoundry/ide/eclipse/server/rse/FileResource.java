/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.rse;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class FileResource extends CloudFoundryHostFile {

	private boolean isDirectory = false;

	private boolean isFile = true;

	private long modifiedDate;

	private String name;

	private String parent;

	private String path;

	private String size;

	public FileResource() {
		super();
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

	public String getAbsolutePath() {
		return path;
	}

	public List<FileResource> getChildren(IProgressMonitor monitor) {
		return Collections.emptyList();
	}

	@Override
	public String getClassification() {
		if (isDirectory) {
			return "directory";
		}
		if (isFile) {
			return "file";
		}
		return "unknown";
	}

	public long getModifiedDate() {
		return modifiedDate;
	}

	public String getName() {
		return name;
	}

	public String getParentPath() {
		return parent;
	}

	public long getSize() {
		// An approximation!!
		long l = 0;
		try {
			String num = "0";
			if (size.endsWith("B")) {
				num = size.substring(0, size.length() - 1);
				l = Long.parseLong(num);
			}
			else if (size.endsWith("K")) {
				num = size.substring(0, size.length() - 1);
				float f = Float.parseFloat(num) * 1000;
				l = ((Float) f).longValue();
			}
			else if (size.endsWith("M")) {
				num = size.substring(0, size.length() - 1);
				float f = Float.parseFloat(num) * 1000 * 1000;
				l = ((Float) f).longValue();
			}
		}
		catch (Exception e) {
			// Swallow the exception and return 0;
		}
		return l;
	}

	public boolean isArchive() {
		return false;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public boolean isFile() {
		return isFile;
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

	public void setAbsolutePath(String path) {
		this.path = path;
	}

	public void setIsDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public void setIsFile(boolean isFile) {
		this.isFile = isFile;
	}

	public void setModifiedDate(long modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setParentPath(String parent) {
		this.parent = parent;
	}

	public void setSize(String size) {
		this.size = size;
	}

}
