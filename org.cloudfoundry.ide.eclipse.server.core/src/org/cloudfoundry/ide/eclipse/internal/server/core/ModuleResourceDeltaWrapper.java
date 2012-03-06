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

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

/**
 * Wraps around a module resource delta so that the delta is accessible to
 * operations that need to check if the resource has been removed
 * 
 */
public class ModuleResourceDeltaWrapper implements IModuleResource {

	private final IModuleResourceDelta resourceDelta;

	public ModuleResourceDeltaWrapper(IModuleResourceDelta resourceDelta) {
		this.resourceDelta = resourceDelta;
	}

	public Object getAdapter(Class clazz) {

		return resourceDelta.getModuleResource().getAdapter(clazz);
	}

	public IPath getModuleRelativePath() {

		return resourceDelta.getModuleResource().getModuleRelativePath();
	}

	public String getName() {
		return resourceDelta.getModuleResource().getName();
	}

	public IModuleResourceDelta getResourceDelta() {
		return resourceDelta;
	}
}