/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import java.io.File;

import org.cloudfoundry.client.lib.domain.DeploymentInfo;

/**
 * Describes the application that is to be pushed to a CF server, including the
 * application's name.
 */
public class ApplicationDeploymentInfo extends DeploymentInfo {

	private File warFile;

	public ApplicationDeploymentInfo(String appName) {
		setDeploymentName(appName);
	}

	public void setWarFile(File warFile) {
		this.warFile = warFile;
	}

	public File getWarFile() {
		return warFile;
	}

}
