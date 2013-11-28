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
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.Arrays;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Contains a default implementation for generating a resources archive of all
 * the resources that are to be pushed to the Cloud Foundry server.
 * 
 */
public abstract class ModuleResourceApplicationDelegate extends ApplicationDelegate {

	public ModuleResourceApplicationDelegate() {

	}

	public boolean providesApplicationArchive(IModule module) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.core.application.
	 * IApplicationDelegate
	 * #getApplicationArchive(org.cloudfoundry.ide.eclipse.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.eclipse.wst.server.core.model.IModuleResource[])
	 */
	public ApplicationArchive getApplicationArchive(CloudFoundryApplicationModule module,
			IModuleResource[] moduleResources) throws CoreException {
		return new ModuleResourceApplicationArchive(module.getLocalModule(), Arrays.asList(moduleResources));
	}
}
