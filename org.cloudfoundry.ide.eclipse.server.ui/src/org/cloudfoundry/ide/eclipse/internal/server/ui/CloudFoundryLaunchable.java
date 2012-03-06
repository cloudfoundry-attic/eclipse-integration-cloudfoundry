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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 */
public class CloudFoundryLaunchable {

	private final IModuleArtifact moduleArtifact;

	public CloudFoundryLaunchable(IModuleArtifact moduleArtifact) {
		this.moduleArtifact = moduleArtifact;
	}

	public IModule getModule() {
		return moduleArtifact.getModule();
	}

}
