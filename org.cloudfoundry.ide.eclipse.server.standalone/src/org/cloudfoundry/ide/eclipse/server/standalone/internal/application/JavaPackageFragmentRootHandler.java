/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.startcommand.JavaTypeResolver;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Resolves all the package fragment roots and main type for a give Java
 * project. It includes handling all required Java projects for the given java
 * project.
 * 
 */
public class JavaPackageFragmentRootHandler {

	private IJavaProject javaProject;

	private IType type;

	public JavaPackageFragmentRootHandler(IJavaProject javaProject, IType type) {
		this.type = type;
		this.javaProject = javaProject;
	}

	public IPackageFragmentRoot[] getPackageFragmentRoots(
			IProgressMonitor monitor) throws CoreException {
		if (type == null) {
			type = new JavaTypeResolver(javaProject)
					.getMainTypesFromSource(monitor);
		}
		ILaunchConfiguration configuration = createConfiguration(type);
		IPath[] classpathEntries = getClasspath(configuration);

		List<IPackageFragmentRoot> pckRoots = new ArrayList<IPackageFragmentRoot>();

		// Since the java project may have other required projects, fetch the
		// ordered
		// list of required projects that will be used to search for package
		// fragment roots
		// corresponding to the resolved class paths. The order of the java
		// projects to search in should
		// start with the most immediate list of required projects of the java
		// project.
		List<IJavaProject> javaProjectsToSearch = getOrderedJavaProjects(javaProject);

		// Find package fragment roots corresponding to the path entries.
		// Search through all java projects, not just the immediate java project
		// for the application that is being pushed to CF.
		for (IPath path : classpathEntries) {

			for (IJavaProject javaProject : javaProjectsToSearch) {

				try {
					IPackageFragmentRoot[] roots = javaProject
							.getPackageFragmentRoots();

					if (roots != null) {
						List<IPackageFragmentRoot> foundRoots = new ArrayList<IPackageFragmentRoot>();

						for (IPackageFragmentRoot packageFragmentRoot : roots) {
							if (isRootAt(packageFragmentRoot, path)) {
								foundRoots.add(packageFragmentRoot);
							}
						}

						// Stop after the first successful search
						if (!foundRoots.isEmpty()) {
							pckRoots.addAll(foundRoots);
							break;
						}
					}

				} catch (Exception e) {
					CloudFoundryPlugin.logError(e);
				}
			}

		}

		return pckRoots.toArray(new IPackageFragmentRoot[pckRoots.size()]);

	}

	public IType getMainType() {
		return type;
	}

	protected List<IJavaProject> getOrderedJavaProjects(IJavaProject project) {
		List<String> collectedProjects = new ArrayList<String>();
		getOrderedJavaProjectNames(
				Arrays.asList(project.getProject().getName()),
				collectedProjects);

		List<IJavaProject> projects = new ArrayList<IJavaProject>();

		for (String name : collectedProjects) {
			IProject prj = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(name);
			if (prj != null) {
				IJavaProject jvPrj = JavaCore.create(prj);
				if (jvPrj != null && jvPrj.exists()) {
					projects.add(jvPrj);
				}
			}
		}

		return projects;

	}

	protected void getOrderedJavaProjectNames(
			List<String> sameLevelRequiredProjects,
			List<String> collectedProjects) {
		// The order in which required projects are collected is as follows,
		// with the RHS
		// being required projects of the LHS
		// A -> BC
		// B -> D
		// C -> E
		// = total 5 projects, added in the order that they are encountered.
		// so final ordered list should be ABCDE
		if (sameLevelRequiredProjects == null) {
			return;
		}
		List<String> nextLevelRequiredProjects = new ArrayList<String>();
		// First add the current level java projects in the order they appear
		// and also collect each one's required names.
		for (String name : sameLevelRequiredProjects) {
			try {
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(name);
				if (project != null) {
					IJavaProject jvPrj = JavaCore.create(project);
					if (jvPrj != null && jvPrj.exists()) {
						if (!collectedProjects.contains(name)) {
							collectedProjects.add(name);
						}
						String[] names = jvPrj.getRequiredProjectNames();
						if (names != null && names.length > 0) {
							for (String reqName : names) {
								if (!nextLevelRequiredProjects
										.contains(reqName)) {
									nextLevelRequiredProjects.add(reqName);
								}
							}
						}
					}
				}

			} catch (JavaModelException e) {
				CloudFoundryPlugin.logError(e);
			}

		}

		// Now recurse to fetch the required projects for the
		// list of java projects that were added at the current level above
		if (!nextLevelRequiredProjects.isEmpty()) {
			getOrderedJavaProjectNames(nextLevelRequiredProjects,
					collectedProjects);

		}
	}

	protected ILaunchConfiguration createConfiguration(IType type)
			throws CoreException {

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

		ILaunchConfigurationType configType = manager
				.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

		ILaunchConfigurationWorkingCopy workingCopy = configType.newInstance(
				null, manager.generateLaunchConfigurationName(type
						.getTypeQualifiedName('.')));
		workingCopy.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				type.getFullyQualifiedName());
		workingCopy.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type
						.getJavaProject().getElementName());
		workingCopy.setMappedResources(new IResource[] { type
				.getUnderlyingResource() });
		return workingCopy.doSave();
	}

	protected IPath[] getClasspath(ILaunchConfiguration configuration)
			throws CoreException {
		IRuntimeClasspathEntry[] entries = JavaRuntime
				.computeUnresolvedRuntimeClasspath(configuration);
		entries = JavaRuntime.resolveRuntimeClasspath(entries, configuration);

		ArrayList<IPath> userEntries = new ArrayList<IPath>(entries.length);
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {

				String location = entries[i].getLocation();
				if (location != null) {
					IPath entry = Path.fromOSString(location);
					if (!userEntries.contains(entry)) {
						userEntries.add(entry);
					}
				}
			}
		}
		return userEntries.toArray(new IPath[userEntries.size()]);
	}

	private static boolean isRootAt(IPackageFragmentRoot root, IPath entry) {
		try {
			IClasspathEntry cpe = root.getRawClasspathEntry();
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath outputLocation = cpe.getOutputLocation();
				if (outputLocation == null) {
					outputLocation = root.getJavaProject().getOutputLocation();
				}

				IPath location = ResourcesPlugin.getWorkspace().getRoot()
						.findMember(outputLocation).getLocation();
				if (entry.equals(location)) {
					return true;
				}
			}
		} catch (JavaModelException e) {
			CloudFoundryPlugin.logError(e);
		}

		IResource resource = root.getResource();
		if (resource != null && entry.equals(resource.getLocation())) {
			return true;
		}

		IPath path = root.getPath();
		if (path != null && entry.equals(path)) {
			return true;
		}

		return false;
	}
}
