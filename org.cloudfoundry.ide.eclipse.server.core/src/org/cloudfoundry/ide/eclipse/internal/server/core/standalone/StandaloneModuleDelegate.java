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
package org.cloudfoundry.ide.eclipse.internal.server.core.standalone;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ProjectModule;

/**
 * The module delegate maps WTP module resources to the specified project's
 * resources. For standalone apps, all project files and folders are mapped to
 * corresponding module resources. If additional filtering is required, it can
 * be done by overriding module resource member getters from the parent class.
 * <p/>
 * The delegate gets invoked indirectly by WTP through an IModule created by the
 * Module factory, when it is adapted to a Module delegate.
 * <p/>
 * This may occur when a request is made for IModuleResource for all the
 * resources in a project , and this request may typically happen only when
 * publishing or updating the resources of a project
 * <p/>
 * Note that when a IModule is created by the module factory, a reference to
 * that factory is passed into the IModule. The factory then is responsible for
 * creating a module delegate, when a module delegate is requested via an
 * adapter call to the IModule.
 * </p>
 * See implementations of IModule
 * 
 * @see StandAloneModuleFactory
 * @see IModule
 */
public class StandaloneModuleDelegate extends ProjectModule {

	public static final IPath[] TEST_SOURCE_NAME_PATTERNS = { new Path("src/test") };

	public StandaloneModuleDelegate(IProject project) {
		super(project);
	}

	protected IModuleResource[] collectResources(String resourceLocation) throws CoreException {

		IProject project = getProject();

		if (project.isAccessible()) {

			IPath projectPath = project.getLocation();

			if (projectPath != null && resourceLocation.equals(projectPath.toString())) {

				return getModuleResources(Path.EMPTY, project);
			}
			else {

				// At this point, the path may be full or relative to a
				// workspace. The path may indicate
				// a resource outside the workspace as well, in particular
				// point to dependency jars outside of the
				// workspace. All these cases must be handled.
				IResource publishableResource = null;
				IPath resourcePath = new Path(resourceLocation);
				File publishableFile = resourcePath.toFile();

				// First see if it is a full path to an existing file.
				if (projectPath != null && publishableFile != null && publishableFile.exists()) {

					IPath relativeResourcepath = resourcePath.makeRelativeTo(projectPath);

					if (publishableFile.isFile()) {
						publishableResource = project.getFile(relativeResourcepath);
					}
					else if (publishableFile.isDirectory()) {
						publishableResource = project.getFolder(relativeResourcepath);
					}
				}
				else {
					// Otherwise check if the path is relative to the
					// workspace and it exists
					publishableResource = ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);
					if (publishableResource == null) {
						publishableResource = ResourcesPlugin.getWorkspace().getRoot().getFolder(resourcePath);
					}
				}

				// If the publishable resource exists, create a
				// corresponding module for it
				if (publishableResource != null && publishableResource.exists()) {
					if (publishableResource instanceof IContainer) {
						return getModuleResources(Path.EMPTY, (IContainer) publishableResource);
					}
					else if (publishableResource instanceof IFile) {
						return new IModuleResource[] { new ModuleFile((IFile) publishableResource,
								publishableResource.getName(), Path.EMPTY) };
					}
				}
				// Others check if it is an external file
				else if (publishableFile != null && publishableFile.exists() && publishableFile.isFile()) {
					// Try creating a module if the file system file exists
					return new IModuleResource[] { new ModuleFile(publishableFile, publishableFile.getName(),
							Path.EMPTY) };
				}
			}
		}

		return null;
	}

	@Override
	public IModuleResource[] members() throws CoreException {
		return computeRuntimeClasspathMembers();
	}

	protected IModuleResource[] computeRuntimeClasspathMembers() throws CoreException {
		Set<IModuleResource> members = new HashSet<IModuleResource>();

		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(getProject());

		if (javaProject != null) {
			String[] resolvedPaths = computeRuntimeClassPath(javaProject);
			if (resolvedPaths != null) {
				for (String path : resolvedPaths) {
					addModuleResources(path, members);
				}
			}
		}
		return members.toArray(new IModuleResource[0]);
	}

	protected void addModuleResources(String location, Collection<IModuleResource> members) throws CoreException {
		if (location != null) {
			IModuleResource[] containerResources = collectResources(location);
			if (containerResources != null) {
				for (IModuleResource resource : containerResources) {
					if (resource != null && !members.contains(resource)) {
						members.add(resource);
					}
				}
			}
		}
	}

	protected boolean shouldSkipTestSources() {
		return true;
	}

	/**
	 * Returns either test sources, or non-test sources, based on a flag setting. If nothing is found,
	 * returns empty list.
	 */
	protected Collection<IClasspathEntry> getSourceEntries(IJavaProject javaProject, boolean istest) {
		try {
			IClasspathEntry[] rawEntries = javaProject.getRawClasspath();
			if (rawEntries != null) {
				Collection<IClasspathEntry> sourceEntries = new HashSet<IClasspathEntry>();
				for (IClasspathEntry entry : rawEntries) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath path = entry.getPath();
						if (path != null) {
							boolean isTestSource = isTestSource(path.toOSString());
							if ((istest && isTestSource) || (!istest && !isTestSource)) {
								sourceEntries.add(entry);
							}
						}
					}
				}
				return sourceEntries;
			}
		}
		catch (JavaModelException e) {
			CloudFoundryPlugin.logError(e);
		}
		return Collections.emptyList();
	}

	protected Collection<String> getSourceOutputLocations(IJavaProject javaProject, boolean istest) {
		Collection<IClasspathEntry> entries = getSourceEntries(javaProject, istest);
		Set<String> locations = new HashSet<String>();
		for (IClasspathEntry entry : entries) {
			IPath path = entry.getOutputLocation();

			// For source entries, path is relative to workspace root
			if (path != null) {

				IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				// the path may be full and relative to the workspace root,
				// so strip the project name
				if (folder.isAccessible()) {
					path = folder.getLocation();
				}
			}

			if (path != null) {
				locations.add(path.toOSString());
			}
		}

		return locations;
	}

	protected Collection<String> getTestSourceOutputLocations(IJavaProject javaProject) {
		return getSourceOutputLocations(javaProject, true);
	}

	protected Collection<String> getNonTestSourceOutputLocations(IJavaProject javaProject) {
		Collection<String> outputs = getSourceOutputLocations(javaProject, false);
		Set<String> nonTestOutput = new HashSet<String>(outputs);
		// add the Java project default output location as well
		try {
			IPath location = javaProject.getOutputLocation();
			if (location != null) {
				nonTestOutput.add(location.toOSString());
			}
		}
		catch (JavaModelException e) {
			CloudFoundryPlugin.logError(e);
		}
		return nonTestOutput;
	}

	/**
	 * 
	 * @param location should be an OS specific location
	 * @return true if the OS path location contains a test source pattern like
	 * "src/test"
	 */
	protected boolean isTestSource(String location) {
		if (location != null) {
			for (IPath testPattern : TEST_SOURCE_NAME_PATTERNS) {
				if (location.contains(testPattern.toOSString())) {
					return true;
				}
			}
		}
		return false;
	}

	public String[] computeRuntimeClassPath(IJavaProject javaProject) throws CoreException {
		IRuntimeClasspathEntry[] unresolved = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);
		IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(javaProject);
		List<IRuntimeClasspathEntry> resolved = new ArrayList<IRuntimeClasspathEntry>();

		for (IRuntimeClasspathEntry rcEntry : unresolved) {

			if (rcEntry.equals(jreEntry)) {
				continue;
			}
			else {
				IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspathEntry(rcEntry, javaProject);

				if (entries != null) {
					resolved.addAll(Arrays.asList(entries));
				}
			}
		}

		if (shouldSkipTestSources()) {
			// The special case to consider is if both the test source and non
			// test source have the same output location
			// if that is the case, the runtime entry associated with that
			// output location CANNOT be skipped. The only time
			// the test source entry can be skipped is if the output location
			// for that entry is NOT also used by a non test source entry
			Collection<String> testSourceOutputLocations = getTestSourceOutputLocations(javaProject);
			Collection<String> nonTestSourceOutputLocations = getNonTestSourceOutputLocations(javaProject);
			List<IRuntimeClasspathEntry> nonTestEntries = new ArrayList<IRuntimeClasspathEntry>(resolved);
			for (IRuntimeClasspathEntry entry : resolved) {
				String entryLocation = entry.getLocation();
				if (testSourceOutputLocations.contains(entryLocation)
						&& !nonTestSourceOutputLocations.contains(entryLocation)) {
					nonTestEntries.remove(entry);
				}
			}
			resolved = nonTestEntries;
		}

		Set<String> resolvedEntryLocations = new HashSet<String>(resolved.size());
		for (IRuntimeClasspathEntry entry : resolved) {
			resolvedEntryLocations.add(entry.getLocation());
		}

		return resolvedEntryLocations.toArray(new String[resolvedEntryLocations.size()]);
	}

}
