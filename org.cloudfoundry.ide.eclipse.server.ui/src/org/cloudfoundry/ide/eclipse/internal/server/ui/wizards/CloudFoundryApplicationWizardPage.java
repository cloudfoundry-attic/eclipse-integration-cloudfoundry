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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class CloudFoundryApplicationWizardPage extends WizardPage {

	private static final String LIFT = "lift/1.0";

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	private static final String DEFAULT_DESCRIPTION = "Specify application details";

	// private CloudApplication app;

	private String appName;

	private boolean canFinish;

	private ApplicationInfo lastApplicationInfo;

	private Text nameText;

	private String serverTypeId;

	protected String filePath;

	private Combo frameworkCombo;

	private Map<String, String> frameworkByLabel;

	private String framework;

	private final CloudFoundryServer server;

	private final ApplicationModule module;

	private final CloudFoundryDeploymentWizardPage deploymentPage;

	private static final String GRAILS_NATURE = "com.springsource.sts.grails.core.nature";

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module) {
		super("Deployment Wizard");
		this.server = server;
		this.deploymentPage = deploymentPage;
		this.module = module;

		if (module == null) {
			// this.app = null;
			this.lastApplicationInfo = null;
		}
		else {
			// this.app = module.getApplication();
			this.lastApplicationInfo = module.getLastApplicationInfo();
			this.serverTypeId = module.getServerTypeId();
		}

		if (lastApplicationInfo == null) {
			lastApplicationInfo = detectApplicationInfo(module);
		}
		appName = lastApplicationInfo.getAppName();

		// Rails, Spring, Grails, Roo, JavaWeb, Sinatra, Node
		frameworkByLabel = new LinkedHashMap<String, String>();
		frameworkByLabel.put("Spring", CloudApplication.SPRING);
		frameworkByLabel.put("Grails", CloudApplication.GRAILS);
		frameworkByLabel.put("Lift", LIFT);
		frameworkByLabel.put("Java Web", CloudApplication.JAVA_WEB);
	}

	public static ApplicationInfo detectApplicationInfo(ApplicationModule module) {
		CloudApplication app = module.getApplication();
		String appName = null;
		if (app != null && app.getName() != null) {
			appName = app.getName();
		}
		if (appName == null) {
			appName = module.getName();
		}

		String framework = getFramework(module);

		ApplicationInfo applicationInfo = new ApplicationInfo(appName);
		applicationInfo.setFramework(framework);
		return applicationInfo;
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
									return LIFT;
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
											return LIFT;
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

	public void createControl(Composite parent) {
		setTitle("Application details");
		setDescription(DEFAULT_DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createContents(composite);

		setControl(composite);

		update(false);
	}

	public ApplicationInfo getApplicationInfo() {
		ApplicationInfo info = new ApplicationInfo(appName);
		info.setFramework(getFramework());
		return info;
	}

	private String getFramework() {
		return frameworkByLabel.get(framework);
	}

	public File getWarFile() {
		if (filePath != null) {
			return new File(filePath);
		}
		return null;
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

	private void createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nameLabel.setText("Name:");

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		nameText.setEditable(true);
		appName = lastApplicationInfo.getAppName();
		nameText.setText(appName);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				appName = nameText.getText();
				deploymentPage.updateUrl();
				update();
			}
		});

		Label frameworkLabel = new Label(composite, SWT.NONE);
		frameworkLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		frameworkLabel.setText("Application Type:");

		frameworkCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		frameworkCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		int index = 0;
		for (Map.Entry<String, String> entry : frameworkByLabel.entrySet()) {
			frameworkCombo.add(entry.getKey());
			if (entry.getValue().equals(lastApplicationInfo.getFramework())) {
				index = frameworkCombo.getItemCount() - 1;
			}
		}
		frameworkCombo.select(index);
		framework = frameworkCombo.getText();
		frameworkCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				update();
				framework = frameworkCombo.getText();
			}
		});
	}

	private void update() {
		update(true);
	}

	private void update(boolean updateButtons) {
		canFinish = true;
		if (nameText.getText() == null || nameText.getText().length() == 0) {
			setDescription("Enter an application name.");
			canFinish = false;
		}

		Matcher matcher = VALID_CHARS.matcher(nameText.getText());
		if (canFinish && !matcher.matches()) {
			setErrorMessage("The entered name contains invalid characters.");
			canFinish = false;
		}
		else {
			setErrorMessage(null);
		}

		ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
		ServerData data = moduleCache.getData(server.getServerOriginal());
		Collection<ApplicationModule> applications = data.getApplications();
		boolean duplicate = false;

		for (ApplicationModule application : applications) {
			if (application != module && application.getApplicationId().equals(nameText.getText())) {
				duplicate = true;
				break;
			}
		}

		if (canFinish && duplicate) {
			setErrorMessage("The entered name conflicts with an application deployed.");
			canFinish = false;
		}

		if (updateButtons) {
			getWizard().getContainer().updateButtons();
		}
	}

}
