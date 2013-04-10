/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationFramework;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRuntime;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.JavaRuntimeTypeHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

/**
 * 
 * Determines if a give module is a Java standalone application. Also provides
 * an archiving mechanism that is specific to Java standalone applications.
 * 
 */
public class StandaloneApplicationDelegate extends ApplicationDelegate {

	public static final String STANDALONE_FRAMEWORK = "standalone";

	public StandaloneApplicationDelegate() {

	}

	public ApplicationFramework getFramework(IModule module)
			throws CoreException {

		StandaloneModuleHelper moduleHelper = new StandaloneModuleHelper(module);
		String framework = null;
		// Determine framework from the module itself
		if (moduleHelper.isSupportedStandalone()) {
			framework = STANDALONE_FRAMEWORK;
		} else {
			IProject project = module.getProject();
			// Otherwise attempt to determine the framework from the project
			// facet
			if (project != null) {
				StandaloneFacetHandler facetHandler = new StandaloneFacetHandler(
						project);
				if (facetHandler.hasFacet()) {
					framework = STANDALONE_FRAMEWORK;
				}
			}
		}

		if (framework != null) {
			return new ApplicationFramework(framework, framework);
		}

		return null;
	}

	@Override
	public List<ApplicationFramework> getSupportedFrameworks() {
		List<ApplicationFramework> frameworks = new ArrayList<ApplicationFramework>();
		frameworks.add(new ApplicationFramework(STANDALONE_FRAMEWORK,
				STANDALONE_FRAMEWORK));
		return frameworks;
	}

	@Override
	public boolean isSupportedFramework(String frameworkName) {
		return STANDALONE_FRAMEWORK.equals(frameworkName);
	}

	@Override
	public boolean providesApplicationArchive(IModule module) {
		// Standalone applications are archived differently than .war file based
		// Web Applications.
		return true;
	}

	@Override
	public boolean requiresURL() {
		// URLs are optional for Java standalone applications
		return false;
	}

	@Override
	public List<ApplicationRuntime> getRuntimes(CloudFoundryServer activeServer)
			throws CoreException {
		List<ApplicationRuntime> runtimes = new JavaRuntimeTypeHelper(
				activeServer).getRuntimeTypes();
		return runtimes;
	}

}
