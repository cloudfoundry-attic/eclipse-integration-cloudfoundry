/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback.DeploymentDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationDelegate;

/**
 * 
 * Determines if a give module is a Java standalone application. Also provides
 * an archiving mechanism that is specific to Java standalone applications.
 * 
 */
public class StandaloneApplicationDelegate extends ApplicationDelegate {

	public StandaloneApplicationDelegate() {

	}

	public boolean requiresURL() {
		// URLs are optional for Java standalone applications
		return false;
	}

	public boolean isValidDescriptor(DeploymentDescriptor descriptor) {
		if (descriptor == null || descriptor.deploymentMode == null) {
			return false;
		}

		ApplicationInfo info = descriptor.applicationInfo;
		if (info == null || info.getAppName() == null) {
			return false;
		}

		DeploymentInfo deploymentInfo = descriptor.deploymentInfo;

		return deploymentInfo != null
				&& deploymentInfo.getDeploymentName() != null
				&& deploymentInfo.getMemory() > 0
				// URLs are optional for standalone applications
				&& descriptor.staging != null
				&& descriptor.staging.getCommand() != null;

	}

}
