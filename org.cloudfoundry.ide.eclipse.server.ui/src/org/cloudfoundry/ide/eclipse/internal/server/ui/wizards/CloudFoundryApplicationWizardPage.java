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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 */

public class CloudFoundryApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {
	private static final String GRAILS_NATURE = "com.springsource.sts.grails.core.nature";

	protected String filePath;

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module) {
		super(server, deploymentPage, module);
	}

	@Override
	protected Map<String, String> getValuesByLabel() {
		// Rails, Spring, Grails, Roo, JavaWeb, Sinatra, Node
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put("Spring", CloudApplication.SPRING);
		valuesByLabel.put("Grails", CloudApplication.GRAILS);
		valuesByLabel.put("Lift", DeploymentConstants.LIFT);
		valuesByLabel.put("Java Web", CloudApplication.JAVA_WEB);
		return valuesByLabel;
	}

	private static String getFramework(ApplicationModule module) {
		if (module != null && module.getLocalModule() != null) {
			IProject project = module.getLocalModule().getProject();
			if (project != null) {
				IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(project);
				if (javaProject != null) {
					if (CloudFoundryProjectUtil.hasNature(project, GRAILS_NATURE)) {
						return CloudApplication.GRAILS;
					}

					// in case user has Grails projects without the nature
					// attached
					if (project.isAccessible() && project.getFolder("grails-app").exists()
							&& project.getFile("application.properties").exists()) {
						return CloudApplication.GRAILS;
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
								IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(),
										javaProject);
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
						CloudFoundryServerUiPlugin
								.getDefault()
								.getLog()
								.log(new Status(IStatus.WARNING, CloudFoundryServerUiPlugin.PLUGIN_ID,
										"Unexpected error during auto detection of application type", e));
					}

					if (CloudFoundryProjectUtil.isSpringProject(project)) {
						return CloudApplication.SPRING;
					}

					if (foundSpringLibrary) {
						return CloudApplication.SPRING;
					}
				}
			}
		}

		return CloudApplication.JAVA_WEB;
	}

	private static boolean isLiftLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches("lift-webkit.*\\.jar", name);
		}
		return false;
	}

	private static boolean isSpringLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches(".*spring.*\\.jar", name);
		}
		return false;
	}

	protected ApplicationInfo detectApplicationInfo(ApplicationModule module) {
		ApplicationInfo applicationInfo = super.detectApplicationInfo(module);
		String framework = getFramework(module);
		applicationInfo.setFramework(framework);
		return applicationInfo;
	}

	public File getWarFile() {
		if (filePath != null) {
			return new File(filePath);
		}
		return null;
	}

	@Override
	protected String getComparisonValue() {
		ApplicationInfo info = getLastApplicationInfo();
		return info != null ? info.getFramework() : null;
	}

	public ApplicationInfo getApplicationInfo() {
		ApplicationInfo info = super.getApplicationInfo();
		info.setFramework(getSelectedValue());
		return info;
	}

	@Override
	protected String getValueLabel() {
		return "Application Type";
	}

}
