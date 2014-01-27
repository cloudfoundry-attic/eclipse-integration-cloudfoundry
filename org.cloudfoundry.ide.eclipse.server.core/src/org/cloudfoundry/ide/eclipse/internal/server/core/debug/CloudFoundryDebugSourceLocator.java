/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class CloudFoundryDebugSourceLocator implements ISourcePathComputer {
	
	public CloudFoundryDebugSourceLocator() {
		init();
	}
	
	protected void init() {
		
	}

	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		List<IRuntimeClasspathEntry> entries = new ArrayList<IRuntimeClasspathEntry>();

		IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
		if (jreEntry != null) {
			entries.add(jreEntry);
		}

		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		
		if (projectName == null) {
			return null;
		}

		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null && javaProject.isOpen() && ("".equals(projectName) || projectName.equals(javaProject.getElementName()))) {
				entries.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
			}
		}

		IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath( //
				entries.toArray(new IRuntimeClasspathEntry[entries.size()]), configuration);
		return JavaRuntime.getSourceContainers(resolved);
	}

	public String getId() {
		return "org.cloudfoundry.ide.eclipse.debug.sourcepathcomputer";
	}

}
