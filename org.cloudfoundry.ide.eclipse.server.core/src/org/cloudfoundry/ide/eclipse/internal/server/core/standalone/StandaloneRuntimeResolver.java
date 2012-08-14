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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Given a java project, two separate lists are resolved: <br/>
 * 1. List of resolved runtime dependency locations like .jar and .zip files <br/>
 * 2. List of resolved runtime source locations like Java .class files, .xml
 * files, and other resources needed during runtime. <br/>
 * In addition, callers can specify whether test sources should be skipped from
 * the list of resolve runtime source locations
 * <p/>
 * Note that this resolver is meant to be disposable, and only resolves
 * dependencies once, caching the results. If update runtime source and
 * dependency locations need to be obtain, a new resolver should be created.
 * 
 */
public class StandaloneRuntimeResolver {

	public static final IPath[] TEST_SOURCE_NAME_PATTERNS = { new Path("src/test") };

	private final boolean skipTestSources;

	private final IJavaProject javaProject;

	private List<String> runtimeSource;

	private List<String> runtimeDependencies;

	public StandaloneRuntimeResolver(IJavaProject javaProject, boolean skipTestSources) {
		this.javaProject = javaProject;
		this.skipTestSources = skipTestSources;
	}

	protected boolean shouldSkipTestSources() {
		return skipTestSources;
	}

	/**
	 * Returns either test sources, or non-test sources, based on a flag
	 * setting. If nothing is found, returns empty list.
	 */
	protected Collection<IClasspathEntry> getSourceEntries(boolean istest) {
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

	protected Collection<String> getSourceOutputLocations(boolean istest) {
		Collection<IClasspathEntry> entries = getSourceEntries(istest);
		Set<String> locations = new HashSet<String>();
		for (IClasspathEntry entry : entries) {
			IPath path = entry.getOutputLocation();

			// For source entries, path is relative to workspace root
			path = getWorkspaceFullPath(path);

			if (path != null) {
				locations.add(path.toOSString());
			}
		}

		return locations;
	}

	protected Collection<String> getTestSourceOutputLocations() {
		return getSourceOutputLocations(true);
	}

	protected Collection<String> getNonTestSourceOutputLocations() {
		Collection<String> outputs = getSourceOutputLocations(false);
		Set<String> nonTestOutput = new HashSet<String>(outputs);
		// add the Java project default output location as well
		try {
			IPath location = javaProject.getOutputLocation();
			location = getWorkspaceFullPath(location);

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
	 * Gets full path for workspace paths. The path may be a path to a project
	 * relative folder, or to a project itself
	 * @param relativePath
	 * @return
	 */
	protected IPath getWorkspaceFullPath(IPath relativePath) {
		if (relativePath == null) {
			return null;
		}
		IPath path = relativePath;
		if (path.segmentCount() == 1) {
			// The path may be the project itself
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
			if (project.isAccessible()) {
				path = project.getLocation();
			}
		}
		else {
			IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);

			if (folder.isAccessible()) {
				path = folder.getLocation();
			}
		}
		return path;
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

	public List<String> getRuntimeSourceLocations() throws CoreException {
		if (runtimeSource == null) {
			computeRuntimeClassPath();
		}
		return runtimeSource;
	}

	public List<String> getRuntimeDependencyLocations() throws CoreException {
		if (runtimeDependencies == null) {
			computeRuntimeClassPath();
		}
		return runtimeDependencies;
	}

	public boolean hasRuntimeDependencies() {
		try {
			return !getRuntimeDependencyLocations().isEmpty();
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	protected void computeRuntimeClassPath() throws CoreException {
		runtimeDependencies = new ArrayList<String>();

		IRuntimeClasspathEntry[] unresolved = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);
		IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(javaProject);
		List<IRuntimeClasspathEntry> resolved = new ArrayList<IRuntimeClasspathEntry>();

		// Resolve all runtime entries, and skip the jre entry
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

		// Separate dependency entries like archives from other runtime entries
		List<IRuntimeClasspathEntry> toSeparate = new ArrayList<IRuntimeClasspathEntry>(resolved);

		for (IRuntimeClasspathEntry entry : resolved) {

			String entryLocation = entry.getLocation();
			if (isAccessibleFile(entryLocation)) {
				toSeparate.remove(entry);
				runtimeDependencies.add(entry.getLocation());
			}
		}
		resolved = toSeparate;

		if (shouldSkipTestSources()) {
			// The special case to consider is if both the test source and non
			// test source have the same output location
			// if that is the case, the runtime entry associated with that
			// output location CANNOT be skipped. The only time
			// the test source entry can be skipped is if the output location
			// for that entry is NOT also used by a non test source entry
			Collection<String> testSourceOutputLocations = getTestSourceOutputLocations();
			Collection<String> nonTestSourceOutputLocations = getNonTestSourceOutputLocations();
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

		runtimeSource = new ArrayList<String>(resolvedEntryLocations);

	}

	protected boolean isAccessibleFile(String location) {
		File file = new File(location);
		try {
			return file.exists() && file.isFile();
		}
		catch (SecurityException e) {
			// Ignore
		}
		return false;
	}
}
