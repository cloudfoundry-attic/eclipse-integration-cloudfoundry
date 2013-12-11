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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

/**
 * Required factory to support Java applications in the Eclipse
 * WST-based Cloud Foundry server, including Spring boot applications.
 * 
 */
public class StandAloneModuleFactory extends ProjectModuleFactoryDelegate {

	@Override
	public ModuleDelegate getModuleDelegate(IModule module) {
		return new JavaLauncherModuleDelegate(module.getProject());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate#createModules
	 * (org.eclipse.core.resources.IProject)
	 */
	public IModule[] createModules(IProject project) {
		if (canCreateModule(project)) {
			IModule module = createModule(project.getName(), project.getName(),
					StandaloneFacetHandler.ID_MODULE_STANDALONE,
					StandaloneFacetHandler.ID_JAVA_STANDALONE_APP_VERSION,
					project);
			if (module != null) {
				return new IModule[] { module };
			}
		}
		// Return null if a module was not created. Returning an empty module
		// list will cache the empty list in WTP
		// preventing a new module from being created if the project state
		// changes in the future
		return null;
	}

	protected boolean canCreateModule(IProject project) {
		// Check if it is a Java project that isn't already supported by another
		// framework (Spring, Grails, etc..), as those
		// modules are created separately.
		return canHandle(project);
	}

	public static boolean canHandle(IProject project) {
		StandaloneFacetHandler handler = new StandaloneFacetHandler(project);
		return CloudFoundryProjectUtil.hasNature(project, JavaCore.NATURE_ID)
				&& handler.hasFacet();
	}
}
