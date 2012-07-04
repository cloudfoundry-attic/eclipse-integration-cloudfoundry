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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
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
		return computeDefaultRuntimeClasspathMembers();
	}

	protected IModuleResource[] computeDefaultRuntimeClasspathMembers() throws CoreException {
		Set<IModuleResource> members = new HashSet<IModuleResource>();

		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(getProject());

		if (javaProject != null) {
			String[] resolvedPaths = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
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

}
