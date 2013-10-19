/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.EnvironmentVariablesPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.springframework.util.Assert;

public class CloudFoundryApplicationEnvVarWizardPage extends PartsWizardPage {

	private final CloudFoundryServer cloudServer;

	protected final ApplicationDeploymentInfo deploymentInfo;

	private EnvironmentVariablesPart envVarPart;

	public CloudFoundryApplicationEnvVarWizardPage(CloudFoundryServer cloudServer,
			ApplicationDeploymentInfo deploymentInfo) {
		super("Environment Variables Wizard Page", null, null);
		Assert.notNull(deploymentInfo);
		this.cloudServer = cloudServer;
		this.deploymentInfo = deploymentInfo;
	}

	public void createControl(Composite parent) {
		setTitle("Environment Variables");
		setDescription("Edit application environment variables");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}

		Composite mainArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(mainArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mainArea);
		envVarPart = new EnvironmentVariablesPart();

		envVarPart.addPartChangeListener(new IPartChangeListener() {

			public void handleChange(PartChangeEvent event) {
				deploymentInfo.setEnvVariables(envVarPart.getVariables());
			}
		});
		envVarPart.createPart(mainArea);

		if (deploymentInfo.getEnvVariables() != null) {
			envVarPart.setInput(deploymentInfo.getEnvVariables());
		}

		setControl(mainArea);

	}

	public boolean isPageComplete() {
		return true;
	}

}
