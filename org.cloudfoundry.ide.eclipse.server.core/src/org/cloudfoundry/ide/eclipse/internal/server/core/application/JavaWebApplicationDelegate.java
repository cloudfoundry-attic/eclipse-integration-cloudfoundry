/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryConstants;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Java Web applications are the standard type of applications supported on
 * Cloud Foundry. They include Spring, Grails, Lift and Java Web.
 * <p/>
 * This application delegate supports the above Java Web frameworks.
 */
public class JavaWebApplicationDelegate implements IApplicationDelegate {

	private static final Map<String, String> JAVA_WEB_SUPPORTED_FRAMEWORKS = getJavaWebSupportedFrameworks();

	public JavaWebApplicationDelegate() {

	}

	protected static Map<String, String> getJavaWebSupportedFrameworks() {
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put(CloudFoundryConstants.SPRING, "Spring");
		valuesByLabel.put(CloudFoundryConstants.GRAILS, "Grails");
		valuesByLabel.put(CloudFoundryConstants.LIFT, "Lift");
		valuesByLabel.put(CloudFoundryConstants.JAVA_WEB, "Java Web");
		return valuesByLabel;
	}

	/**
	 * Attempts to determine the framework based on the contents and nature of
	 * the project. Returns null if no framework was determined.
	 * @param project
	 * @return Framework type or null if framework was not determined.
	 * @deprecated kept for reference as application type is being determined by
	 * checking properties of a Java project
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
				if (project.isAccessible() && project.getFolder("grails-app").exists()
						&& project.getFile("application.properties").exists()) {
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
							"Unexpected error during auto detection of application type", e));
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
			return Pattern.matches("lift-webkit.*\\.jar", name);
		}
		return false;
	}

	private boolean isSpringLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches(".*spring.*\\.jar", name);
		}
		return false;
	}

	public boolean requiresURL() {
		// All Java Web applications require a URL when pushed to a CF server
		return true;
	}

	public boolean providesApplicationArchive(IModule module) {
		// No need for application archive as Java Web applications
		// require a .war file created by the CF plugin framework
		return false;
	}

	public ApplicationArchive getApplicationArchive(IModule module, IModuleResource[] moduleResources)
			throws CoreException {
		// No need for application archive, as the CF plugin framework generates
		// .war files for Java Web applications.
		return null;
	}

	public boolean isValidDescriptor(DeploymentDescriptor descriptor) {
		if (descriptor == null || descriptor.deploymentMode == null) {
			return false;
		}

		ApplicationInfo info = descriptor.applicationInfo;
		if (info == null || info.getAppName() == null) {
			return false;
		}

		DeploymentInfo deploymentInfo = descriptor.deploymentInfo;

		return deploymentInfo != null && deploymentInfo.getDeploymentName() != null && deploymentInfo.getMemory() > 0
				&& deploymentInfo.getUris() != null && !deploymentInfo.getUris().isEmpty();

	}
}
