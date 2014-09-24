/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class CloudFoundryProjectUtil {

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
	 * Judges whether the given resource is a Java project or not.
	 * 
	 * @param resouce the resource to be tested
	 * @return true if it is a Java project, otherwise false
	 */
	public static boolean isJavaProject(IResource resouce) {
		return hasNature(resouce, JavaCore.NATURE_ID);
	}
}
