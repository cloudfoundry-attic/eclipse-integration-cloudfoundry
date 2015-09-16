/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 *     IBM - Turning into base WAR packaging provider.
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryConstants;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Java Web applications are the standard type of applications supported on
 * Cloud Foundry. They include Spring, Grails, Lift and Java Web.
 * <p/>
 * This application delegate supports the above Java Web frameworks.
 */
public class JavaWebApplicationDelegate extends AbstractApplicationDelegate {

	public JavaWebApplicationDelegate() {

	}

	/**
	 * 
	 * @deprecated No longer used for v2 CF servers. Kept only as a reference
	 * for legacy v1 CF servers.
	 */
	protected static Map<String, String> getJavaWebSupportedFrameworks() {
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put(CloudFoundryConstants.SPRING, "Spring"); //$NON-NLS-1$
		valuesByLabel.put(CloudFoundryConstants.GRAILS, "Grails"); //$NON-NLS-1$
		valuesByLabel.put(CloudFoundryConstants.LIFT, "Lift"); //$NON-NLS-1$
		valuesByLabel.put(CloudFoundryConstants.JAVA_WEB, "Java Web"); //$NON-NLS-1$
		return valuesByLabel;
	}

	/**
	 * Attempts to determine the framework based on the contents and nature of
	 * the project. Returns null if no framework was determined.
	 * @param project
	 * @return Framework type or null if framework was not determined.
	 * @deprecated kept for reference as to how application type was being
	 * determined from a Java project for legacy v1 CF servers. v2 Servers no
	 * longer require a framework for an application, as frameworks have been
	 * replaced with buildpacks.
	 */
	protected String getFramework(IProject project) {
		if (project != null) {
			IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(project);
			if (javaProject != null) {
				if (CloudFoundryProjectUtil.hasNature(project, CloudFoundryConstants.GRAILS_NATURE)) {
					return CloudFoundryConstants.GRAILS;
				}

				// in case user has Grails projects without the nature
				// attached
				if (project.isAccessible() && project.getFolder("grails-app").exists() //$NON-NLS-1$
						&& project.getFile("application.properties").exists()) { //$NON-NLS-1$
					return CloudFoundryConstants.GRAILS;
				}

				IClasspathEntry[] entries;
				boolean foundSpringLibrary = false;
				try {
					entries = javaProject.getRawClasspath();
					for (IClasspathEntry entry : entries) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
							if (isLiftLibrary(entry)) {
								return CloudFoundryConstants.LIFT;
							}
							if (isSpringLibrary(entry)) {
								foundSpringLibrary = true;
							}
						}
						else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
							IClasspathContainer container = JavaCore
									.getClasspathContainer(entry.getPath(), javaProject);
							if (container != null) {
								for (IClasspathEntry childEntry : container.getClasspathEntries()) {
									if (isLiftLibrary(childEntry)) {
										return CloudFoundryConstants.LIFT;
									}
									if (isSpringLibrary(childEntry)) {
										foundSpringLibrary = true;
									}
								}
							}
						}
					}
				}
				catch (JavaModelException e) {
					// Log the error but don't throw it again as there may be
					// other ways to detect the framework
					CloudFoundryPlugin.log(new Status(IStatus.WARNING, CloudFoundryPlugin.PLUGIN_ID,
							"Unexpected error during auto detection of application type", e)); //$NON-NLS-1$
				}

				if (CloudFoundryProjectUtil.isSpringProject(project)) {
					return CloudFoundryConstants.SPRING;
				}

				if (foundSpringLibrary) {
					return CloudFoundryConstants.SPRING;
				}
			}
		}
		return null;
	}

	private boolean isLiftLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches("lift-webkit.*\\.jar", name); //$NON-NLS-1$
		}
		return false;
	}

	private boolean isSpringLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches(".*spring.*\\.jar", name); //$NON-NLS-1$
		}
		return false;
	}

	public boolean requiresURL() {
		// All Java Web applications require a URL when pushed to a CF server
		return true;
	}

	public boolean providesApplicationArchive(IModule module) {
		// Returns a default WAR archive package
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.core.internal.application.
	 * AbstractApplicationDelegate
	 * #getApplicationArchive(org.cloudfoundry.ide.eclipse.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer,
	 * org.eclipse.wst.server.core.model.IModuleResource[],
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ApplicationArchive getApplicationArchive(CloudFoundryApplicationModule module,
			CloudFoundryServer cloudServer, IModuleResource[] moduleResources, IProgressMonitor monitor)
			throws CoreException {

		ApplicationArchive manifestArchive = getArchiveFromManifest(module, cloudServer);
		if (manifestArchive != null) {
			return manifestArchive;
		}
		try {
			File warFile = CloudUtil.createWarFile(new IModule[] { module.getLocalModule() },
					(Server) cloudServer.getServer(), monitor);

			CloudFoundryPlugin.trace("War file " + warFile.getName() + " created"); //$NON-NLS-1$ //$NON-NLS-2$

			return new CloudZipApplicationArchive(new ZipFile(warFile));
		}
		catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
					"Failed to create war file. " + //$NON-NLS-1$
							"\nApplication: " + module.getApplication().getName() + //$NON-NLS-1$
							"\nModule: " + module.getName() + //$NON-NLS-1$
							"\nException: " + e.getMessage(), e)); //$NON-NLS-1$
		}
	}

	@Override
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {

		IStatus status = super.validateDeploymentInfo(deploymentInfo);
		if (status.isOK() && ((deploymentInfo.getUris() == null || deploymentInfo.getUris().isEmpty()))) {
			String errorMessage = Messages.JavaWebApplicationDelegate_ERROR_NO_MAPPED_APP_URL;
			status = CloudFoundryPlugin.getErrorStatus(errorMessage);
		}

		return status;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.core.internal.application.
	 * ApplicationDelegate
	 * #getDefaultApplicationDeploymentInfo(org.cloudfoundry.ide
	 * .eclipse.internal.server.core.client.CloudFoundryApplicationModule,
	 * org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException {
		ApplicationDeploymentInfo info = super.getDefaultApplicationDeploymentInfo(appModule, cloudServer, monitor);

		// Set a default URL for the application.
		if ((info.getUris() == null || info.getUris().isEmpty()) && info.getDeploymentName() != null) {

			CloudApplicationURL url = ApplicationUrlLookupService.update(cloudServer, monitor)
					.getDefaultApplicationURL(info.getDeploymentName());
			info.setUris(Arrays.asList(url.getUrl()));
		}
		return info;
	}

	public static ApplicationArchive getArchiveFromManifest(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) throws CoreException {
		String archivePath = null;
		ManifestParser parser = new ManifestParser(appModule, cloudServer);
		// Read the path again instead of deployment info, as a user may be
		// correcting the path after the module was creating and simply
		// attempting to push it again without the
		// deployment wizard
		if (parser.hasManifest()) {
			archivePath = parser.getApplicationProperty(null, ManifestParser.PATH_PROP);
		}

		File packagedFile = null;
		if (archivePath != null) {
			// Only support paths that point to archive files
			IPath path = new Path(archivePath);
			if (path.getFileExtension() != null) {
				// Check if it is project relative first
				IFile projectRelativeFile = null;
				IProject project = CloudFoundryProjectUtil.getProject(appModule);

				if (project != null) {
					projectRelativeFile = project.getFile(archivePath);
				}

				if (projectRelativeFile != null && projectRelativeFile.exists()) {
					packagedFile = projectRelativeFile.getLocation().toFile();
				}
				else {
					// See if it is an absolute path
					File absoluteFile = new File(archivePath);
					if (absoluteFile.exists() && absoluteFile.canRead()) {
						packagedFile = absoluteFile;
					}
				}
			}
			// If a path is specified but no file found stop further deployment
			if (packagedFile == null) {
				String message = NLS.bind(Messages.JavaWebApplicationDelegate_ERROR_FILE_NOT_FOUND_MANIFEST_YML,
						archivePath);
				throw CloudErrorUtil.toCoreException(message);
			}
			else {
				try {
					return new CloudZipApplicationArchive(new ZipFile(packagedFile));
				}
				catch (ZipException e) {
					throw CloudErrorUtil.toCoreException(e);
				}
				catch (IOException e) {
					throw CloudErrorUtil.toCoreException(e);
				}
			}
		}
		return null;
	}
}
