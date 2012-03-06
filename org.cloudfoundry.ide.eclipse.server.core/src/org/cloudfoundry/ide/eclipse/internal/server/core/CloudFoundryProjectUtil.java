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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class CloudFoundryProjectUtil {

	public static final String SPRING_NATURE_ID = "org.springframework.ide.eclipse.core.springnature";

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
					CloudFoundryPlugin.logError(e);
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
		if (project.isAccessible()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					return (IJavaProject) project.getNature(JavaCore.NATURE_ID);
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError("Error getting Java project for project '" + project.getName() + "'", e);
			}
		}
		return null;
	}

}
