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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.archive.AbstractApplicationArchiveEntry;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.archive.DirectoryApplicationArchive;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Generates a deployable archive for standalone applications where the
 * structure of the deployed resources is derived from the target or output
 * directories of the project.
 * 
 */
class StandaloneApplicationArchive extends DirectoryApplicationArchive {
	private final String applicationId;

	private final IProject project;

	private List<Entry> targetEntries;

	StandaloneApplicationArchive(File directory, String applicationId, IProject project) {
		super(directory);
		this.applicationId = applicationId;
		this.project = project;
	}

	protected void collectEntries(List<Entry> entries, File subFolder, File outputLocationDirectory) {
		for (File child : subFolder.listFiles()) {
			entries.add(new StandaloneEntryAdapter(child, outputLocationDirectory));
			if (child.isDirectory()) {
				collectEntries(entries, child, outputLocationDirectory);
			}
		}
	}

	@Override
	public Iterable<Entry> getEntries() {
		if (targetEntries == null) {
			targetEntries = new ArrayList<ApplicationArchive.Entry>();

			IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(project);

			try {
				IClasspathEntry[] entries = javaProject.getRawClasspath();

				// First collect entries for user specified output folders.
				for (IClasspathEntry entry : entries) {
					if (entry.getEntryKind() == org.eclipse.jdt.core.IClasspathEntry.CPE_SOURCE) {
						// This will only return an output location IF it is a
						// user specified location. Otherwise
						// it returns null;
						IPath targetLocation = entry.getOutputLocation();
						String outputLocation = getAbsolutePath(targetLocation);
						if (outputLocation != null) {
							File locationFile = new File(outputLocation);
							collectEntries(targetEntries, locationFile, locationFile);
						}
					}
				}

				// Now collect entries from the default output directory
				String outputLocation = getAbsolutePath(javaProject.getOutputLocation());
				if (outputLocation != null) {
					File locationFile = new File(outputLocation);
					collectEntries(targetEntries, locationFile, locationFile);
				}

			}
			catch (JavaModelException e) {

				CloudFoundryPlugin.logError(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
						"Unable to determine output or target location for: " + applicationId, e));
			}

		}
		return targetEntries;
	}

	/**
	 * Returns the absolute path of the given output location, if is accessible,
	 * or null if the absolute path cannot be resolved, possibly because the
	 * underlying resource is not accessible.
	 * @param outputLocation
	 * @return absolute path of accessible output location, or null if it is not
	 * accessible, including not existing
	 */
	protected String getAbsolutePath(IPath outputLocation) {
		if (outputLocation != null) {
			IPath projectPath = project.getLocation();

			IPath relativeLocation = outputLocation.makeRelativeTo(projectPath);
			IResource resource = project.getFolder(relativeLocation);
			if (resource.isAccessible()) {
				return resource.getLocation().toString();
			}

		}
		return null;
	}

	static class StandaloneEntryAdapter extends AbstractApplicationArchiveEntry {

		private final File file;

		private String name;

		public StandaloneEntryAdapter(File file, File directory) {
			this.file = file;
			this.name = file.getAbsolutePath().substring(directory.getAbsolutePath().length() + 1);
			if (isDirectory()) {
				this.name = this.name + "/";
			}
		}

		public boolean isDirectory() {
			return file.isDirectory();
		}

		public String getName() {
			return name;
		}

		public InputStream getInputStream() throws IOException {
			if (isDirectory()) {
				return null;
			}
			return new FileInputStream(file);
		}
	}
}