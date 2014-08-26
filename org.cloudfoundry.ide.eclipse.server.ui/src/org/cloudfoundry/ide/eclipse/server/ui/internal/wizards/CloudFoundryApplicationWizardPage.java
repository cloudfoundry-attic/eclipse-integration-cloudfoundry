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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.ModuleCache;
import org.cloudfoundry.ide.eclipse.server.core.internal.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UIPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.WizardPartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * First page in the application deployment wizard that prompts the user for an
 * application name.
 * 
 */
public class CloudFoundryApplicationWizardPage extends PartsWizardPage {

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	protected static final String DEFAULT_DESCRIPTION = "Specify application details.";

	private String appName;

	private String buildpack;

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
		super("Deployment Wizard", null, null);
		this.server = server;
		this.deploymentPage = deploymentPage;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();
	}

	protected void init() {
		appName = descriptor.getDeploymentInfo().getDeploymentName();
		buildpack = descriptor.getDeploymentInfo().getStaging() != null ? descriptor.getDeploymentInfo().getStaging()
				.getBuildpackUrl() : null;
	}

	protected CloudFoundryApplicationWizard getApplicationWizard() {
		return (CloudFoundryApplicationWizard) getWizard();
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

	}

	protected Composite createContents(Composite parent) {

		// This must be called first as the values are then populate into the UI
		// widgets
		init();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final UIPart appPart = new AppNamePart();
		// Add the listener first so that it can be notified of changes during
		// the part creation.
		appPart.addPartChangeListener(deploymentPage);
		appPart.addPartChangeListener(this);

		appPart.createPart(composite);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nameLabel.setText("Buildpack URL (optional):");

		buildpackText = new Text(composite, SWT.BORDER);

		if (buildpack != null) {
			buildpackText.setText(buildpack);
		}

		buildpackText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		buildpackText.setEditable(true);

		buildpackText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				buildpack = buildpackText.getText().trim();

				validateBuildPack();
			}
		});

		final Button saveToManifest = new Button(composite, SWT.CHECK);
		saveToManifest.setText("Save to manifest file");
		saveToManifest
				.setToolTipText("Save the deployment configuration to the application's manifest file. If a manifest file does not exist, a new one will be created. If one already exists, changes will be merged with the existing file.");

		GridDataFactory.fillDefaults().grab(false, false).applyTo(saveToManifest);

		saveToManifest.setSelection(false);

		saveToManifest.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				descriptor.persistDeploymentInfo(saveToManifest.getSelection());
			}

		});
		return composite;

	}

	protected void validateBuildPack() {

		IStatus status = Status.OK_STATUS;
		// Clear the previous buildpack anyway, as either a new one valid one
		// will be set
		// or if invalid, it should be cleared from the descriptor. The
		// descriptor should only contain
		// either null or a valid URL.
		descriptor.setBuildpack(null);

		// buildpack URL is optional, so an empty URL is acceptable
		if (!ValueValidationUtil.isEmpty(buildpack)) {
			try {
				URL urlObject = new URL(buildpack);
				String host = urlObject.getHost();
				if (host == null || host.length() == 0) {
					status = CloudFoundryPlugin.getErrorStatus("Enter a valid URL.");
				}
				else {
					// Only set valid buildpack URLs
					descriptor.setBuildpack(buildpack);
				}
			}
			catch (MalformedURLException e) {
				status = CloudFoundryPlugin.getErrorStatus("Enter a valid URL.");
			}
		}

		handleChange(new PartChangeEvent(buildpack, status, CloudUIEvent.BUILD_PACK_URL));

	}

	protected IStatus getUpdateNameStatus() {
		IStatus status = Status.OK_STATUS;
		if (ValueValidationUtil.isEmpty(appName)) {
			status = CloudFoundryPlugin.getStatus("Enter an application name.", IStatus.ERROR);
		}
		else {
			Matcher matcher = VALID_CHARS.matcher(appName);
			if (!matcher.matches()) {
				status = CloudFoundryPlugin.getErrorStatus("The entered name contains invalid characters.");
			}
			else {
				ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
				ServerData data = moduleCache.getData(server.getServerOriginal());
				Collection<CloudFoundryApplicationModule> applications = data.getExistingCloudModules();
				boolean duplicate = false;

				for (CloudFoundryApplicationModule application : applications) {
					if (application != module && application.getDeployedApplicationName().equals(appName)) {
						duplicate = true;
						break;
					}
				}

				if (duplicate) {
					status = CloudFoundryPlugin
							.getErrorStatus("The entered name conflicts with an application deployed.");
				}
			}
		}

		return status;

	}

	protected class AppNamePart extends UIPart {

		private Text nameText;

		@Override
		public Control createPart(Composite parent) {

			Label nameLabel = new Label(parent, SWT.NONE);
			nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			nameLabel.setText("Name:");

			nameText = new Text(parent, SWT.BORDER);
			nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			nameText.setEditable(true);

			nameText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					appName = nameText.getText();
					// If first time initialising, dont update the wizard
					// buttons as the may not be available to update yet
					boolean updateButtons = true;
					notifyChange(new WizardPartChangeEvent(appName, getUpdateNameStatus(),
							CloudUIEvent.APP_NAME_CHANGE_EVENT, updateButtons));
				}
			});

			// Set the app name after adding the listener to fire event
			if (appName != null) {
				nameText.setText(appName);
			}

			return parent;
		}
	}
}
