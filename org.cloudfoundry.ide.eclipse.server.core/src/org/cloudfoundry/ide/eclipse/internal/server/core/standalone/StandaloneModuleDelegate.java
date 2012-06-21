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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
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

	 

	public StandaloneModuleDelegate(IProject project) {
		super(project);
	}

	protected IModuleResource[] collectResources(IPath resourceLocation) {
		IPath projectPath = getProject().getLocation();

		IPath relativeLocation = resourceLocation.makeRelativeTo(projectPath);
		IResource resource = getProject().getFolder(relativeLocation);

		if (resource != null && resource.exists() && resource instanceof IContainer) {
			try {
				IContainer container = (IContainer) resource;
				// Make all members of the out folder relative to an empty path.
				// for example, if .class files are in:
				// myproject/bin/com/myclasses, and the output folder is
				// myproject/bin, then the relative module resource location for
				// all content of myproject/bin should
				// NOT include the myproject/bin path segments, as these will
				// not be present in the CF server.
				// Note that whatever path is specified as the relative path
				// will have content path appended to it, so start
				// with an empty path
				return getModuleResources(Path.EMPTY, container);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

	@Override
	public IModuleResource[] members() throws CoreException {
		List<IModuleResource> members = new ArrayList<IModuleResource>();

		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(getProject());

		try {
			IClasspathEntry[] entries = javaProject.getRawClasspath();

			// First collect entries for user specified output folders.
			for (IClasspathEntry entry : entries) {
				if (entry.getEntryKind() == org.eclipse.jdt.core.IClasspathEntry.CPE_SOURCE) {
					// This will only return an output location IF it is a
					// user specified location. Otherwise
					// it returns null;

					IPath targetLocation = entry.getOutputLocation();
					if (targetLocation != null) {
						IModuleResource[] containerResources = collectResources(targetLocation);
						if (containerResources != null) {
							members.addAll(Arrays.asList(containerResources));
						}
					}
				}
			}

			// Now collect entries from the default output directory
			IPath outputLocation = javaProject.getOutputLocation();
			if (outputLocation != null) {
				IModuleResource[] containerResources = collectResources(outputLocation);
				if (containerResources != null) {
					members.addAll(Arrays.asList(containerResources));
				}
			}

		}
		catch (JavaModelException e) {

			CloudFoundryPlugin.logError(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
					"Unable to determine output or target location for: " + getProject().getName(), e));
		}
		return members.toArray(new IModuleResource[0]);
	}

}
