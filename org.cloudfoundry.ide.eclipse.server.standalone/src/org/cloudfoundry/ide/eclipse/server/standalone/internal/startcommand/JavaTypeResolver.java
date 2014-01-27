/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.startcommand;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;

/**
 * 
 * Helper methods for UI components that require Java type searching, given a
 * valid java project.
 */
public class JavaTypeResolver {

	private final IJavaProject project;

	public JavaTypeResolver(IJavaProject project) {
		this.project = project;
	}

	protected IJavaProject getJavaProject() {
		return project;
	}

	public IType[] getMainTypes(IProgressMonitor monitor) {
		IJavaProject javaProject = getJavaProject();

		if (javaProject != null) {
			// REturns main method types
			boolean includeSubtypes = true;
			MainMethodSearchEngine engine = new MainMethodSearchEngine();
			int constraints = IJavaSearchScope.SOURCES;
//			constraints |= IJavaSearchScope.APPLICATION_LIBRARIES;
			IJavaSearchScope scope = SearchEngine
					.createJavaSearchScope(new IJavaElement[] { javaProject }, constraints);
			return engine.searchMainMethods(monitor, scope, includeSubtypes);
		}
		return new IType[] {};

	}

	public IType getMainTypesFromSource(IProgressMonitor monitor) {
		if (project != null) {
			IType firstEncounteredSourceType = null ;
			IType[] types = getMainTypes(monitor);
					// Enable when dependency to org.springsource.ide.eclipse.commons.core is
					// added. This should be the common way to obtain main types
//					MainTypeFinder.guessMainTypes(project, monitor);
	
			if (types != null) {
				for (IType type : types) {
					if (!type.isBinary()) {
						firstEncounteredSourceType = type;
						break;
					}
				}
			}
			 return firstEncounteredSourceType;
		}
		return null;

	}

	

}
