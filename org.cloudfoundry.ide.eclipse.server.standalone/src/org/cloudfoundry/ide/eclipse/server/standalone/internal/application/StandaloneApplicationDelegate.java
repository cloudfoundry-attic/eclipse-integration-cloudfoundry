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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ModuleResourceApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.eclipse.core.runtime.IStatus;

/**
 * 
 * Determines if a give module is a Java standalone application. Also provides
 * an archiving mechanism that is specific to Java standalone applications.
 * 
 */
public class StandaloneApplicationDelegate extends
		ModuleResourceApplicationDelegate {

	public StandaloneApplicationDelegate() {

	}

	public boolean requiresURL() {
		// URLs are optional for Java standalone applications
		return false;
	}

	@Override
	public IStatus validateDeploymentInfo(
			ApplicationDeploymentInfo deploymentInfo) {
		IStatus status = super.validateDeploymentInfo(deploymentInfo);
		if (status.isOK()) {
			String errorMessage = null;

			if (deploymentInfo.getStaging() == null) {
				errorMessage = "Missing staging in application deployment information.";
			} else if (ValueValidationUtil.isEmpty(deploymentInfo.getStaging()
					.getCommand())) {
				errorMessage = "Missing Java standalone start command in staging.";
			}
			if (errorMessage != null) {
				status = CloudFoundryPlugin.getErrorStatus(errorMessage);
			}
		}

		return status;
	}

}
