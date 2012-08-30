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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.File;

import org.cloudfoundry.client.lib.domain.CloudApplication;

public class ApplicationInfo {
	private String appName;

	private File warFile;

	private String framework;

	public ApplicationInfo(String appName) {
		this.appName = appName;
		this.framework = CloudApplication.SPRING;
	}

	public String getAppName() {
		return appName;
	}

	public void setWarFile(File warFile) {
		this.warFile = warFile;
	}

	public File getWarFile() {
		return warFile;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}
}
