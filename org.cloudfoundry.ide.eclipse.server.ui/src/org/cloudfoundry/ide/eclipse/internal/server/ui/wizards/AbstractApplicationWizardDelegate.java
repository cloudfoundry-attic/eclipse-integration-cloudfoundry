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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;

@SuppressWarnings("restriction")
public abstract class AbstractApplicationWizardDelegate implements IApplicationWizardDelegate {

	private CloudApplicationUrlLookup urlLookup;

	/*
	 * FIXNS: Note that a very similar logic is also present in corresponding
	 * core level IApplicationDelegate implementation for Java web. Use one
	 * descriptor for both the wizard and the core, instead of two separate
	 * ones, to avoid duplication.
	 */
	public boolean isValid(ApplicationWizardDescriptor applicationDescriptor) {

		if (applicationDescriptor == null) {
			return false;
		}

		ApplicationInfo info = applicationDescriptor.getApplicationInfo();
		if (info == null || info.getAppName() == null) {
			return false;
		}

		DeploymentInfo deploymentInfo = applicationDescriptor.getDeploymentInfo();

		return deploymentInfo != null && deploymentInfo.getDeploymentName() != null && deploymentInfo.getMemory() > 0;

	}

	public void initialiseWizardDescriptor(ApplicationWizardDescriptor applicationDescriptor,
			CloudFoundryServer cloudServer, CloudFoundryApplicationModule module) {

		urlLookup = CloudApplicationUrlLookup.getCurrentLookup(cloudServer);

		DeploymentInfo info = new DeploymentInfo();
		applicationDescriptor.setDeploymentInfo(info);

		DeploymentInfo lastInfo = (module != null) ? module.getLastDeploymentInfo() : null;

		String deploymentName = (lastInfo != null) ? lastInfo.getDeploymentName() : getNameFromModule(module);
		info.setDeploymentName(deploymentName);

		int memory = CloudUtil.DEFAULT_MEMORY;
		info.setMemory(memory);

		String appName = null;
		ApplicationInfo lastApplicationInfo = null;

		if (module != null) {
			lastApplicationInfo = module.getLastApplicationInfo();
		}

		if (lastApplicationInfo == null) {
			appName = getNameFromModule(module);
		}
		else {
			appName = lastApplicationInfo.getAppName();
		}

		if (appName != null) {
			ApplicationInfo appInfo = new ApplicationInfo(appName);
			applicationDescriptor.setApplicationInfo(appInfo);
		}

		// Default should be to start in regular mode upon deployment
		ApplicationAction deploymentMode = ApplicationAction.START;

		applicationDescriptor.setStartDeploymentMode(deploymentMode);

		String url = getDefaultURL(lastInfo, deploymentName);
		if (url != null) {
			List<String> urls = new ArrayList<String>();
			urls.add(url);
			applicationDescriptor.getDeploymentInfo().setUris(urls);
		}
	}

	private String getNameFromModule(CloudFoundryApplicationModule module) {
		if (module != null) {
			CloudApplication app = module.getApplication();
			if (app != null && app.getName() != null) {
				return app.getName();
			}
			return module.getName();
		}
		return null;
	}

	protected String getDefaultURL(DeploymentInfo previousInfo, String deploymentName) {

		String url = previousInfo != null && previousInfo.getUris() != null && !previousInfo.getUris().isEmpty() ? previousInfo
				.getUris().get(0) : null;

		if (urlLookup != null) {
			CloudApplicationURL appURL = urlLookup.getDefaultApplicationURL(deploymentName);
			url = appURL.getUrl();
		}

		return url;
	}

	/**
	 * @see CloudFoundryServerBehaviour#getApplicationUrlLookup()
	 */
	public CloudApplicationUrlLookup getApplicationUrlLookup() {
		return urlLookup;
	}

}
