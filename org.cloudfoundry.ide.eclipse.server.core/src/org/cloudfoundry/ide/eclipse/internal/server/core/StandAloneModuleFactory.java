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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

public class StandAloneModuleFactory extends ProjectModuleFactoryDelegate {

	@Override
	public ModuleDelegate getModuleDelegate(IModule arg0) {
		return new StandaloneModuleDelegate();
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
					CloudFoundryServer.ID_JAVA_STANDALONE_APP, CloudFoundryServer.ID_JAVA_STANDALONE_APP_VERSION,
					project);
			return new IModule[] { module };
		}
		return new IModule[0];
	}

	protected boolean canCreateModule(IProject project) {
		return true;
	}
}
