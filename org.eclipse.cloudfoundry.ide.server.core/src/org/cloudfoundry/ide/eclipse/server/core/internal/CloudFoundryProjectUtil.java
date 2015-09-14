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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class CloudFoundryProjectUtil {

	public static final String ID_MODULE_STANDALONE = "cloudfoundry.standalone.app"; //$NON-NLS-1$

	public static final String SPRING_NATURE_ID = "org.springframework.ide.eclipse.core.springnature"; //$NON-NLS-1$

	private CloudFoundryProjectUtil() {
		// Utility Class
	}

	public static boolean hasNature(IResource resource, String natureId) {
		if (resource != null && resource.isAccessible()) {
			IProject project = resource.getProject();
			if (project != null) {
				try {
					return project.hasNature(natureId);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.log(e);
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if given resource's project is a Spring project.
	 */
	public static boolean isSpringProject(IResource resource) {
		return hasNature(resource, SPRING_NATURE_ID);
	}

	public static boolean isJavaProject(IProject project) {
		return getJavaProject(project) != null;
	}

	/**
	 * Returns the corresponding Java project or <code>null</code> a for given
	 * project.
	 * @param project the project the Java project is requested for
	 * @return the requested Java project or <code>null</code> if the Java
	 * project is not defined or the project is not accessible
	 */
	public static IJavaProject getJavaProject(IProject project) {
		if (project == null) {
			return null;
		}
		if (project.isAccessible()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					return (IJavaProject) project.getNature(JavaCore.NATURE_ID);
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError("Error getting Java project for project '" + project.getName() + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}

	/**
	 * Given an cloud module, attempt to find a corresponding workspace project
	 * that is accessible.
	 * @param appModule
	 * @return Accessible project related to the application module, or null if
	 * not accessible or does not exist.
	 */
	public static IProject getProject(CloudFoundryApplicationModule appModule) {
		IProject project = appModule.getLocalModule() != null ? appModule.getLocalModule().getProject() : null;

		return project != null && project.isAccessible() ? project : null;
	}

	/**
	 * 
	 * @param appModule with a possible corresponding workspace Project
	 * @return Java project, if it exists and is accessible in the workspace for
	 * the given appModule, or null.
	 */
	public static IJavaProject getJavaProject(CloudFoundryApplicationModule appModule) {
		return getJavaProject(getProject(appModule));
	}

	/**
	 *
	 * @param project
	 * @return true if the Spring boot application is a standalone Java
	 * application (jar app) configured for CF deployment. False if it is not
	 * Spring Boot app or it is not a standalone Java application.
	 */
	public static boolean isSpringBootCloudFoundryConfigured(IProject project) {

		if (isSpringBoot(project)) {
			IProjectFacet facet = ProjectFacetsManager.getProjectFacet(ID_MODULE_STANDALONE);
			try {
				IFacetedProject facetedProject = ProjectFacetsManager.create(project);
				return facetedProject != null && facetedProject.hasProjectFacet(facet);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.log(e);
			}
		}
		return false;
	}

	/**
	 * 
	 * 
	 * Derived from org.springframework.ide.eclipse.boot.core.BootPropertyTester
	 * 
	 * FIXNS: Remove when boot detection is moved to a common STS plug-in that
	 * can be shared with CF Eclipse.
	 * @return true if the given project is a Spring boot project, false
	 * otherwise
	 */
	public static boolean isSpringBoot(IProject project) {
		if (project != null && isSpringProject(project)) {
			try {
				IJavaProject javaProject = getJavaProject(project);

				if (javaProject != null) {
					IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
					// Look for a 'spring-boot' jar entry
					for (IClasspathEntry e : classpath) {
						if (hasBootDependencies(e)) {
							return true;
						}
					}
				}
			}
			catch (Exception e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return false;
	}

	/**
	 * 
	 * @param appModule
	 * @return true if it is a Spring Boot app. False otherwise. It makes no
	 * checks on the type of packaging (war, jar, etc..)
	 */
	public static boolean isSpringBoot(CloudFoundryApplicationModule appModule) {
		if (appModule == null) {
			return false;
		}
		return isSpringBoot(getProject(appModule));
	}

	private static boolean hasBootDependencies(IClasspathEntry e) {
		if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
			IPath path = e.getPath();
			String name = path.lastSegment();
			return name.endsWith(".jar") && name.startsWith("spring-boot"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return false;
	}

}
