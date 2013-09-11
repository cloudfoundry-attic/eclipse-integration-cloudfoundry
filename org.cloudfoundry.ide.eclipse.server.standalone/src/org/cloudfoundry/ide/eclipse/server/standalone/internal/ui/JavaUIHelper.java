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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;

/**
 * 
 * Helper methods for UI components that require Java type searching, given a
 * valid java project.
 */
public class JavaUIHelper {

	private final IJavaProject project;

	public JavaUIHelper(IJavaProject project) {
		this.project = project;
	}

	protected IJavaProject getJavaProject() {
		return project;
	}

	public IType[] getMainMethodTypes(IProgressMonitor monitor) {
		IJavaProject javaProject = getJavaProject();

		if (javaProject != null) {
			boolean includeSubtypes = true;
			MainMethodSearchEngine engine = new MainMethodSearchEngine();
			int constraints = IJavaSearchScope.SOURCES;
			constraints |= IJavaSearchScope.APPLICATION_LIBRARIES;
			IJavaSearchScope scope = SearchEngine
					.createJavaSearchScope(new IJavaElement[] { javaProject }, constraints);
			return engine.searchMainMethods(monitor, scope, includeSubtypes);
		}
		return new IType[] {};

	}

	public IType getMainMethodTypeFromSource(IProgressMonitor monitor) {
		IType[] types = getMainMethodTypes(monitor);
		IType firstEncounteredSourceType = null;
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

	public IPackageFragment getDefaultPackageFragment() {
		IJavaProject javaProject = getJavaProject();

		if (getJavaProject() == null) {
			return null;
		}

		IPackageFragmentRoot[] roots = null;
		try {

			IClasspathEntry[] entries = javaProject.getRawClasspath();

			for (IClasspathEntry entry : entries) {

				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					roots = javaProject.findPackageFragmentRoots(entry);
					if (roots != null) {
						break;
					}
				}
			}

		}
		catch (JavaModelException e) {
			CloudFoundryPlugin.log(e);
		}

		if (roots != null) {
			IPackageFragment fragment = null;
			for (IPackageFragmentRoot root : roots) {
				try {
					IJavaElement[] members = root.getChildren();
					if (members != null) {
						for (IJavaElement element : members) {
							if (element instanceof IPackageFragment) {
								IPackageFragment frag = (IPackageFragment) element;
								if (frag.isDefaultPackage()) {
									fragment = frag;
									break;
								}
							}
						}
					}
					if (fragment != null) {
						break;
					}
				}
				catch (JavaModelException e) {
					CloudFoundryPlugin.log(e);
				}
			}
			return fragment;
		}
		return null;
	}

}
