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
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Deployment handler
 */
public class ApplicationDeploymentHandler {

	private static ApplicationDeploymentHandler handler;

	private CloudFoundryCallback callBack;

	protected ApplicationDeploymentHandler(CloudFoundryCallback callBack) {
		this.callBack = callBack;
	}

	public ApplicationDeploymentHandler getHandler() {
		if (handler == null) {
			CloudFoundryCallback callBack = CloudFoundryPlugin.getCallback();
			handler = new ApplicationDeploymentHandler(callBack);
		}
		return handler;
	}

	public DeploymentDescriptor prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) {
		return callBack != null ? callBack.prepareForDeployment(server, module, monitor) : null;
	}

}
