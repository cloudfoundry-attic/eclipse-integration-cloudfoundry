/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;

/**
 * Required factory to support Java applications in the Eclipse WST-based Cloud
 * Foundry server, including Spring boot applications.
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
		if (!CloudFoundryProjectUtil.hasNature(project, JavaCore.NATURE_ID)) {
			return false;
		}

		StandaloneFacetHandler handler = new StandaloneFacetHandler(project);

		// If it is Spring boot, and it doesn't have the facet, add it to avoid
		// having users manually add the facet
		if (!CloudFoundryProjectUtil.isWarApp(project) && !handler.hasFacet()) {
			IJavaProject javaProject = CloudFoundryProjectUtil
					.getJavaProject(project);

			if (javaProject != null
					&& CloudFoundryProjectUtil.isSpringBootProject(javaProject)) {
				// Only add the face if jst.web is NOT present, to avoid
				// configuring a WAR Spring boot.
				handler.addFacet(new NullProgressMonitor());
			}
		}

		return handler.hasFacet();
	}
}
