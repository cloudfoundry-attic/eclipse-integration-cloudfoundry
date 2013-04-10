/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;

/**
 * Java Web applications are the standard type of applications supported on
 * Cloud Foundry. They include Spring, Grails, Lift and Java Web.
 * <p/>
 * This application delegate supports the above Java Web frameworks.
 */
public class JavaWebApplicationDelegate extends ApplicationDelegate {

	private static final Map<String, String> JAVA_WEB_SUPPORTED_FRAMEWORKS = getJavaWebSupportedFrameworks();

	public JavaWebApplicationDelegate() {

	}

	protected static Map<String, String> getJavaWebSupportedFrameworks() {
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put(DeploymentConstants.SPRING, "Spring");
		valuesByLabel.put(DeploymentConstants.GRAILS, "Grails");
		valuesByLabel.put(DeploymentConstants.LIFT, "Lift");
		valuesByLabel.put(DeploymentConstants.JAVA_WEB, "Java Web");
		return valuesByLabel;
	}

	public ApplicationFramework getFramework(IModule module) throws CoreException {
		IProject project = module != null ? module.getProject() : null;
		// Determine if it is Grails, Spring or Lift
		String framework = getFramework(project);

		// Otherwise determine if it is a Java Web module.
		if (framework == null) {
			// Determine from the module type
			if (module != null) {
				String moduleType = module.getModuleType() != null ? module.getModuleType().getId() : null;
				if (DeploymentConstants.ID_WEB_MODULE.equals(moduleType)) {
					framework = DeploymentConstants.JAVA_WEB;
				}
			}
			else {
				// Attempt to determine from the project facet
				try {
					IFacetedProject facetedProject = ProjectFacetsManager.create(project);
					IProjectFacet facet = ProjectFacetsManager.getProjectFacet(DeploymentConstants.ID_WEB_MODULE);
					if (facetedProject != null && facet != null && facetedProject.hasProjectFacet(facet)) {
						framework = DeploymentConstants.JAVA_WEB;
					}
				}
				catch (CoreException e) {
					// Ignore the exception for now
				}
			}

		}
		if (framework != null) {

			String displayName = JAVA_WEB_SUPPORTED_FRAMEWORKS.get(framework);
			if (displayName == null) {
				displayName = framework;
			}
			return new ApplicationFramework(framework, displayName);
		}
		else {
			return null;
		}

	}

	public List<ApplicationFramework> getSupportedFrameworks() {
		List<ApplicationFramework> supportedFrameworks = new ArrayList<ApplicationFramework>();

		for (Entry<String, String> entry : JAVA_WEB_SUPPORTED_FRAMEWORKS.entrySet()) {
			supportedFrameworks.add(new ApplicationFramework(entry.getKey(), entry.getValue()));
		}
		return supportedFrameworks;
	}

	/**
	 * Attempts to determine the framework based on the contents and nature of
	 * the project. Returns null if no framework was determined.
	 * @param project
	 * @return Framework type or null if framework was not determined.
	 */
	protected String getFramework(IProject project) {
		if (project != null) {
			IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(project);
			if (javaProject != null) {
				if (CloudFoundryProjectUtil.hasNature(project, DeploymentConstants.GRAILS_NATURE)) {
					return DeploymentConstants.GRAILS;
				}

				// in case user has Grails projects without the nature
				// attached
				if (project.isAccessible() && project.getFolder("grails-app").exists()
						&& project.getFile("application.properties").exists()) {
					return DeploymentConstants.GRAILS;
				}

				IClasspathEntry[] entries;
				boolean foundSpringLibrary = false;
				try {
					entries = javaProject.getRawClasspath();
					for (IClasspathEntry entry : entries) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
							if (isLiftLibrary(entry)) {
								return DeploymentConstants.LIFT;
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
										return DeploymentConstants.LIFT;
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
					CloudFoundryPlugin.logError(new Status(IStatus.WARNING, CloudFoundryPlugin.PLUGIN_ID,
							"Unexpected error during auto detection of application type", e));
				}

				if (CloudFoundryProjectUtil.isSpringProject(project)) {
					return DeploymentConstants.SPRING;
				}

				if (foundSpringLibrary) {
					return DeploymentConstants.SPRING;
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

	public boolean isSupportedFramework(String frameworkName) {
		return DeploymentConstants.SPRING.equals(frameworkName) || DeploymentConstants.GRAILS.equals(frameworkName)
				|| DeploymentConstants.LIFT.equals(frameworkName) || DeploymentConstants.JAVA_WEB.equals(frameworkName);

	}

	public boolean providesApplicationArchive(IModule module) {
		// No need for application archive as Java Web applications
		// require a .war file created by the CF plugin framework
		return false;
	}

	public boolean requiresURL() {
		// All Java Web applications require a URL when pushed to a CF server
		return true;
	}

	public List<ApplicationRuntime> getRuntimes(CloudFoundryServer activeServer) throws CoreException {
		return new JavaRuntimeTypeHelper(activeServer).getRuntimeTypes();
	}
}
