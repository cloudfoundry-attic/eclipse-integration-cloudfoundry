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
package org.eclipse.cft.server.standalone.internal.application;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.standalone.internal.startcommand.JavaStartCommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ModuleFolder;
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
 * <p/>
 * Note also that module resources for corresponding local resources are created
 * recursively, and the list of module resources generated are root level module
 * resources that may contain child members. THe list of module resources is not
 * a flat list, and root level folder module resources may have child members.
 * Consequently, when handling the list of generated module resources, it should
 * be done recursively.
 * <p/>
 * See implementations of IModule
 * 
 * @see StandAloneModuleFactory
 * @see IModule
 * @deprecated As of CF 1.6.0 Java applications are now packaged into jars prior to deployment to Cloud Foundry. See {@link JavaCloudFoundryArchiver}
 */
public class StandaloneModuleDelegate extends ProjectModule {

	public StandaloneModuleDelegate(IProject project) {
		super(project);
	}

	/**
	 * Generates module resources for the given resource location. If the
	 * resource location is a workspace folder or project, module resources for
	 * all of its children are also generated recursively. The relative module
	 * resource path is the path where the resource should be published
	 * remotely, and it need not match the relative or absolute local resource
	 * location. Therefore a resource in
	 * /Users/myuser/workspace/myproject/myfolder maybe be published to /lib, so
	 * remotely, myfolder will appear as /lib/myfolder. Although /lib need not
	 * exist locally, it must have a corresponding module resource element.
	 * 
	 * @param localResourceLocation
	 * @param relativeModuleResourcePath
	 * @return module resources for the given location, or null if module
	 *         resources could not be resolved for the given location
	 * @throws CoreException
	 */
	protected IModuleResource[] collectResources(String localResourceLocation,
			IPath relativeModuleResourcePath) throws CoreException {

		IProject project = getProject();

		if (project.isAccessible()) {

			IPath projectPath = project.getLocation();

			// Handle the case where the resource location points to the project
			// itself
			if (projectPath != null
					&& localResourceLocation.equals(projectPath.toOSString())) {
				return getModuleResources(relativeModuleResourcePath, project);
			} else {

				// Module resources should be mapped to corresponding workspace
				// resources, unless they are external files, at which point
				// they should be mapped to the external File.
				// At this stage, find workspace or File resources that
				// correspond to
				// the module resource to be created.

				// At this point, the path may be full or relative to a
				// workspace. The path may indicate
				// a resource outside the workspace as well, in particular
				// point to dependency jars outside of the
				// workspace. All these cases must be handled.
				IResource workspaceResource = null;
				IPath resourcePath = new Path(localResourceLocation);
				File publishableFile = resourcePath.toFile();

				// First see if it is a full path to an existing file.
				if (projectPath != null && publishableFile != null
						&& publishableFile.exists()) {

					IPath relativeResourcepath = resourcePath
							.makeRelativeTo(projectPath);

					if (publishableFile.isFile()) {
						workspaceResource = project
								.getFile(relativeResourcepath);
					} else if (publishableFile.isDirectory()) {
						workspaceResource = project
								.getFolder(relativeResourcepath);
					}
				} else {
					// Otherwise check if the path is relative to the
					// workspace and it exists
					workspaceResource = ResourcesPlugin.getWorkspace()
							.getRoot().getFile(resourcePath);
					if (workspaceResource == null
							|| !workspaceResource.exists()) {
						workspaceResource = ResourcesPlugin.getWorkspace()
								.getRoot().getFolder(resourcePath);
					}
				}

				// If the publishable workspace resource exists, create a
				// corresponding module resource for it
				if (workspaceResource != null && workspaceResource.exists()) {
					if (workspaceResource instanceof IContainer) {
						return getModuleResources(relativeModuleResourcePath,
								(IContainer) workspaceResource);
					} else if (workspaceResource instanceof IFile) {
						return new IModuleResource[] { new ModuleFile(
								(IFile) workspaceResource,
								workspaceResource.getName(),
								relativeModuleResourcePath) };
					}
				}
				// Others check if it is an external file
				else if (publishableFile != null && publishableFile.exists()
						&& publishableFile.isFile()) {
					// Try creating a module if the file system file exists
					return new IModuleResource[] { new ModuleFile(
							publishableFile, publishableFile.getName(),
							relativeModuleResourcePath) };
				}
			}
		}

		return null;
	}

	@Override
	public IModuleResource[] members() throws CoreException {
		return computeRuntimeClasspathMembers();
	}

	protected IModuleResource[] computeRuntimeClasspathMembers()
			throws CoreException {
		List<IModuleResource> members = new ArrayList<IModuleResource>();

		IJavaProject javaProject = CloudFoundryProjectUtil
				.getJavaProject(getProject());

		if (javaProject != null) {
			StandaloneRuntimeResolver resolver = new StandaloneRuntimeResolver(
					javaProject, true);

			List<String> resolvedSource = resolver.getRuntimeSourceLocations();

			// Add the non-dependecy resources first at root level path. This
			// means that, when published,
			// these resources will be added at root level in the remote app
			// directory
			if (resolvedSource != null) {
				for (String path : resolvedSource) {
					addModuleResources(path, members, Path.EMPTY);
				}
			}

			// Add the dependencies in a /lib
			if (resolver.hasRuntimeDependencies()) {
				// check if at root level, there already exists a /lib folder
				ModuleFolder libFolder = createLibFolder(members);
				List<String> dependencies = resolver
						.getRuntimeDependencyLocations();
				List<IModuleResource> libMembers = new ArrayList<IModuleResource>();
				for (String path : dependencies) {
					addModuleResources(path, libMembers,
							JavaStartCommand.DEFAULT_LIB_PATH);
				}
				if (!libMembers.isEmpty()) {
					libFolder.setMembers(libMembers
							.toArray(new IModuleResource[0]));
				}
			}
		}
		return members.toArray(new IModuleResource[0]);
	}

	/**
	 * Either returns an existing /lib folder at root level in the give list of
	 * module resources, or creates one and adds it to the given list of module
	 * resources. As a consequence, the passed list of module resources should
	 * be the original list, and not a copy. Always returns a non-null /lib
	 * folder that has been added to the given list of module resources
	 * 
	 * @param moduleResources
	 * @return non-null /lib folder
	 */
	protected ModuleFolder createLibFolder(List<IModuleResource> moduleResources) {
		for (IModuleResource resource : moduleResources) {
			if (resource instanceof ModuleFolder
					&& JavaStartCommand.DEFAULT_LIB_PATH.equals(resource
							.getName())) {
				return (ModuleFolder) resource;
			}
		}

		// Otherwise create one with a root level module relative path (i.e. an
		// empty path)
		org.eclipse.wst.server.core.util.ModuleFolder folder = new ModuleFolder(
				null, JavaStartCommand.DEFAULT_LIB, Path.EMPTY);
		moduleResources.add(folder);
		return folder;
	}

	/**
	 * Generates module resources for the given local resource location, and
	 * adds it to the give list of existing module resources. If the resource
	 * location is a folder, all content of the folder will be recursively added
	 * as child members of the folder. The module relative path is the relative
	 * path associated with the resource, where the resource will be located
	 * remotely.
	 * <p/>
	 * Example: if the local resource is a folder at
	 * /Users/myuser/workspace/myproject/myfolder, and the relative module path
	 * is specified as /lib, then A module resource folder for "myfolder",
	 * containing all the content of myfolder as additional module resource
	 * members, will be created, and when published, the folder will appear in
	 * relative /lib path remotely, so the relative remote location will be
	 * /lib/myfolder.
	 * 
	 * <p/>
	 * 
	 * Note that the module relative path MUST have a corresponding module
	 * resource folder, if it is not an empty path, meaning that there must be a
	 * corresponding module resource folder for it. In the example above, there
	 * must be a module resource folder for "/lib", but it is not necessary for
	 * it to actually exists locally, since module resources do not have to map
	 * to actual local resources. If an empty path is specified, it means that
	 * all the module resources created will be published at root level
	 * relatively to the remote application directory.
	 * 
	 * <p/>
	 * 
	 * If the local resource whose module resource needs to be created should be
	 * added at root level remotely, then an empty path should be passed as a
	 * module relative path.
	 * 
	 * @param resourceLocation
	 *            resource location that needs to be mapped to corresponding
	 *            module resources. location can be relative or absolute.
	 * @param members
	 *            where module resource should be added
	 * @param moduleRelativePath
	 *            remote location path of the resource. It does not need to be
	 *            the same as the local resource location.
	 * @throws CoreException
	 */
	protected void addModuleResources(String resourceLocation,
			Collection<IModuleResource> members, IPath moduleRelativePath)
			throws CoreException {
		if (resourceLocation != null) {
			IModuleResource[] containerResources = collectResources(
					resourceLocation, moduleRelativePath);
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
