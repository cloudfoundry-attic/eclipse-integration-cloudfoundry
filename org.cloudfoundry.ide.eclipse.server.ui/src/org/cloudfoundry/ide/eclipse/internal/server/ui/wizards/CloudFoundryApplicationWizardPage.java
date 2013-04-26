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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationFramework;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRuntime;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
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

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	protected static final String DEFAULT_DESCRIPTION = "Specify application details";

	private String appName;

	private boolean canFinish;

	private Text nameText;

	private String serverTypeId;

	protected final CloudFoundryServer server;

	protected final ApplicationModule module;

	protected final CloudFoundryDeploymentWizardPage deploymentPage;

	protected Map<String, ApplicationRuntime> runtimeByLabels;

	private Combo runtimeCombo;

	protected String filePath;

	protected Map<String, ApplicationFramework> frameworksByLabel;

	private Combo frameworkCombo;

	protected final ApplicationWizardDescriptor descriptor;

	protected ApplicationRuntime selectedRuntime;

	protected ApplicationFramework selectedFramework;

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module,
			ApplicationWizardDescriptor descriptor) {
		super("Deployment Wizard");
		this.server = server;
		this.deploymentPage = deploymentPage;
		this.module = module;
		this.descriptor = descriptor;

	}

	protected void initialiseFromLastDeployment() {

		ApplicationInfo lastApplicationInfo = null;

		if (module != null) {
			// this.app = module.getApplication();
			lastApplicationInfo = module.getLastApplicationInfo();
			this.serverTypeId = module.getServerTypeId();
		}

		if (lastApplicationInfo == null) {
			appName = getAppName(module);
		}
		else {
			appName = lastApplicationInfo.getAppName();
		}

		// Set the application info based on information from the previous
		// deployment
		setApplicationInfo();
	}

	protected CloudFoundryApplicationWizard getApplicationWizard() {
		return (CloudFoundryApplicationWizard) getWizard();
	}

	protected void initRuntimesAndFrameworks() {
		runtimeByLabels = new HashMap<String, ApplicationRuntime>();
		List<ApplicationRuntime> allRuntimes = getApplicationWizard().getRuntimes();

		if (allRuntimes != null) {
			for (ApplicationRuntime runtime : allRuntimes) {
				runtimeByLabels.put(runtime.getDisplayName(), runtime);
			}
		}

		List<ApplicationFramework> frameworks = getApplicationWizard().getFrameworks();
		frameworksByLabel = new HashMap<String, ApplicationFramework>();

		if (frameworks != null) {
			for (ApplicationFramework framework : frameworks) {
				frameworksByLabel.put(framework.getDisplayName(), framework);
			}
		}

		// See if there already is a current staging. If so read the framework
		// and runtime
		if (descriptor.getStaging() != null) {
			String frameworkValue = descriptor.getStaging().getFramework();
			String runtimeValue = descriptor.getStaging().getRuntime();

			for (ApplicationFramework fw : frameworksByLabel.values()) {
				if (fw.getFramework().equals(frameworkValue)) {
					selectedFramework = fw;
					break;
				}
			}

			for (ApplicationRuntime rt : runtimeByLabels.values()) {
				if (rt.getRuntime().equals(runtimeValue)) {
					selectedRuntime = rt;
					break;
				}
			}
		}
		else {
			// Otherwise, set default values
			selectedFramework = frameworksByLabel.size() > 0 ? frameworksByLabel.values().iterator().next() : null;
			selectedRuntime = runtimeByLabels.size() > 0 ? runtimeByLabels.values().iterator().next() : null;
			setStaging();
		}

		// Override the default framework value with the one in the last
		// application info, if available
		ApplicationInfo lastApplicationInfo = module.getLastApplicationInfo();
		if (lastApplicationInfo != null) {
			// Use the framework from the previous application info
			String lastFramework = lastApplicationInfo.getFramework();
			if (lastFramework != null) {
				selectedFramework = frameworksByLabel.get(lastFramework);
			}
		}

	}

	protected void createRuntimeArea(Composite composite) {

		if (runtimeByLabels.size() > 0) {

			Label runtimeLabel = new Label(composite, SWT.NONE);
			runtimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			runtimeLabel.setText("Runtime: ");

			// Either show the runtime as a label if only one runtime exists, or
			// show a combo if
			// multiple runtimes exist
			if (runtimeByLabels.size() == 1) {
				Label runtime = new Label(composite, SWT.NONE);
				runtime.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
				String label = selectedRuntime != null ? selectedRuntime.getDisplayName() : null;
				if (label != null) {
					runtime.setText(label);

				}
			}
			else if (runtimeByLabels.size() > 1) {

				runtimeCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
				runtimeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				int index = 0;
				for (Map.Entry<String, ApplicationRuntime> entry : runtimeByLabels.entrySet()) {
					runtimeCombo.add(entry.getKey());
					if (entry.getValue().equals(selectedRuntime)) {
						index = runtimeCombo.getItemCount() - 1;
					}
				}
				runtimeCombo.select(index);

				runtimeCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

						selectedRuntime = runtimeByLabels.get(runtimeCombo.getText());

						update();
					}
				});
			}
		}
	}

	protected void setStaging() {
		if (selectedFramework != null && selectedRuntime != null) {
			descriptor.setStaging(selectedFramework, selectedRuntime);
		}
	}

	protected String getAppName(ApplicationModule module) {
		CloudApplication app = module.getApplication();
		String appName = null;
		if (app != null && app.getName() != null) {
			appName = app.getName();
		}
		if (appName == null) {
			appName = module.getName();
		}
		return appName;
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

	protected void setApplicationInfo() {
		ApplicationInfo info = new ApplicationInfo(appName);
		// Backward compatibility. Also set the framework in the application
		// info, although the staging is the primary
		// way of setting a framework for an application
		if (descriptor.getStaging() != null) {
			info.setFramework(descriptor.getStaging().getFramework());
		}
		descriptor.setApplicationInfo(info);
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

	protected Composite createContents(Composite parent) {

		// This must be called first as the values are then populate into the UI
		// widgets
		initialiseFromLastDeployment();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nameLabel.setText("Name:");

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		nameText.setEditable(true);

		if (appName != null) {
			nameText.setText(appName);
		}
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				appName = nameText.getText();

				setApplicationInfo();
				update();

				// Also update the URL with the new name
				deploymentPage.updateUrl();
			}
		});

		// Show framework and runtime if it is not a CCNG server that supports
		// buildpacks, organisations and spaces
		if (!(getWizard() instanceof CloudFoundryApplicationWizard)
				|| !((CloudFoundryApplicationWizard) getWizard()).isCCNGServer()) {

			initRuntimesAndFrameworks();
			createFrameworkArea(composite);
			createRuntimeArea(composite);
		}

		return composite;

	}

	protected void createFrameworkArea(Composite composite) {

		// Don't show combo if only one framework entry is present
		if (frameworksByLabel.size() < 2) {
			return;
		}

		Label frameworkLabel = new Label(composite, SWT.NONE);
		frameworkLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		frameworkLabel.setText("Application Type:");

		frameworkCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		frameworkCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		int index = 0;
		for (Map.Entry<String, ApplicationFramework> entry : frameworksByLabel.entrySet()) {
			frameworkCombo.add(entry.getKey());
			if (entry.getValue().equals(selectedFramework)) {
				index = frameworkCombo.getItemCount() - 1;
			}
		}
		frameworkCombo.select(index);

		frameworkCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

				selectedFramework = frameworksByLabel.get(frameworkCombo.getText());

				update();
			}
		});
	}

	protected void update() {
		update(true);
	}

	protected void update(boolean updateButtons) {
		canFinish = true;
		setStaging();

		if (ValueValidationUtil.isEmpty(appName)) {
			setDescription("Enter an application name.");
			canFinish = false;
		}

		Matcher matcher = VALID_CHARS.matcher(appName);
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
			if (application != module && application.getApplicationId().equals(appName)) {
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
