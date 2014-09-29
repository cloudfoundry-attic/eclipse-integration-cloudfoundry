/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
			// Returns main method types
			boolean includeSubtypes = true;
			MainMethodSearchEngine engine = new MainMethodSearchEngine();
			int constraints = IJavaSearchScope.SOURCES;
			constraints |= IJavaSearchScope.APPLICATION_LIBRARIES;
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
					new IJavaElement[] { javaProject }, constraints);
			return engine.searchMainMethods(monitor, scope, includeSubtypes);
		}
		return new IType[] {};

	}

	public IType getMainTypesFromSource(IProgressMonitor monitor) {
		if (project != null) {
			IType firstEncounteredSourceType = null;
			IType[] types = getMainTypes(monitor);
			// Enable when dependency to
			// org.springsource.ide.eclipse.commons.core is
			// added. This should be the common way to obtain main types
			// MainTypeFinder.guessMainTypes(project, monitor);

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
