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
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.Arrays;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Contains a default implementation for generating a resources archive of all
 * the resources that are to be pushed to the Cloud Foundry server.
 * 
 */
public abstract class ApplicationDelegate implements IApplicationDelegate {

	public ApplicationDelegate() {

	}
	
	public boolean providesApplicationArchive(IModule module) {
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.core.application.
	 * IApplicationDelegate
	 * #getApplicationArchive(org.eclipse.wst.server.core.IModule,
	 * org.eclipse.wst.server.core.model.IModuleResource[])
	 */
	public ApplicationArchive getApplicationArchive(IModule module, IModuleResource[] moduleResources)
			throws CoreException {
		return new ModuleResourceApplicationArchive(module, Arrays.asList(moduleResources));
	}

}
