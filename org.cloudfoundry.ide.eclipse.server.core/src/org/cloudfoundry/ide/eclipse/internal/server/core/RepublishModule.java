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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.eclipse.wst.server.core.IModule;

public class RepublishModule {

	private final IModule module;

	private final ApplicationDeploymentInfo deploymentInfo;

	public RepublishModule(IModule module, ApplicationDeploymentInfo deploymentInfo) {
		this.module = module;
		this.deploymentInfo = deploymentInfo;
	}

	public IModule getModule() {
		return module;
	}

	public ApplicationDeploymentInfo getDeploymentInfo() {
		return deploymentInfo;
	}

}
