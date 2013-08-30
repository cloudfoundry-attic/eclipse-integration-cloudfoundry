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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * First page in the application deployment wizard that prompts the user for an
 * application name.
 * 
 */
@SuppressWarnings("restriction")
public class CloudFoundryApplicationWizardPage extends WizardPage {

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	protected static final String DEFAULT_DESCRIPTION = "Specify application details";

	private String appName;

	private String buildpack;

	private boolean canFinish;

	private Text nameText;

	private Text buildpackText;

	private String serverTypeId;

	protected final CloudFoundryServer server;

	protected final CloudFoundryApplicationModule module;

	protected String filePath;

	protected final ApplicationWizardDescriptor descriptor;

	protected final CloudFoundryDeploymentWizardPage deploymentPage;

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, CloudFoundryApplicationModule module,
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

	protected String getAppName(CloudFoundryApplicationModule module) {
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
			}
		});

		nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nameLabel.setText("Buildpack URL (optional):");

		buildpackText = new Text(composite, SWT.BORDER);
		buildpackText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		buildpackText.setEditable(true);

		buildpackText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				buildpack = buildpackText.getText().trim();

				validateBuildPack();
			}
		});

		return composite;

	}

	protected void update() {
		update(true);
	}

	protected void validateBuildPack() {
		canFinish = true;
		setErrorMessage(null);

		// Clear the previous buildpack anyway, as either a new one valid one
		// will be set
		// or if invalid, it should be cleared from the descriptor. The
		// descriptor should only contain
		// either null or a valid URL.
		descriptor.setBuildpack(null);

		// buildpack URL is optional, so an empty URL is acceptable
		if (buildpack != null && buildpack.length() > 0) {
			try {
				URL urlObject = new URL(buildpack);
				String host = urlObject.getHost();
				if (host == null || host.length() == 0) {
					canFinish = false;
					setErrorMessage("Enter a valid URL");
				}
				else {
					// Only set valid buildpack URLs
					descriptor.setBuildpack(buildpack);
				}
			}
			catch (MalformedURLException e) {
				setErrorMessage("Enter a valid URL");
				canFinish = false;
			}
		}

		getWizard().getContainer().updateButtons();
	}

	protected void update(boolean updateButtons) {
		canFinish = true;

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
		Collection<CloudFoundryApplicationModule> applications = data.getApplications();
		boolean duplicate = false;

		for (CloudFoundryApplicationModule application : applications) {
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
