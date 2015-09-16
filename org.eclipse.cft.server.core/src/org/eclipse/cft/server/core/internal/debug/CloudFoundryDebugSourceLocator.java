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
package org.eclipse.cft.server.core.internal.debug;

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

		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		
		if (projectName == null) {
			return null;
		}

		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null && javaProject.isOpen() && ("".equals(projectName) || projectName.equals(javaProject.getElementName()))) { //$NON-NLS-1$
				entries.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
			}
		}

		IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath( //
				entries.toArray(new IRuntimeClasspathEntry[entries.size()]), configuration);
		return JavaRuntime.getSourceContainers(resolved);
	}

	public String getId() {
		return "org.cloudfoundry.ide.eclipse.debug.sourcepathcomputer"; //$NON-NLS-1$
	}

}
