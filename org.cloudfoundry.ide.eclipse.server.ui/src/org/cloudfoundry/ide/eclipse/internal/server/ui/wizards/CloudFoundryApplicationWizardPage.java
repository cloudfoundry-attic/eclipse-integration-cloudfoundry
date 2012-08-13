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

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 */

public class CloudFoundryApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {

	protected String filePath;

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module) {
		super(server, deploymentPage, module);
	}

	protected Map<String, String> getValuesByLabel() {
		// Rails, Spring, Grails, Roo, JavaWeb, Sinatra, Node
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put("Spring", CloudApplication.SPRING);
		valuesByLabel.put("Grails", CloudApplication.GRAILS);
		valuesByLabel.put("Lift", DeploymentConstants.LIFT);
		valuesByLabel.put("Java Web", CloudApplication.JAVA_WEB);
		return valuesByLabel;
	}

	protected ApplicationInfo detectApplicationInfo(ApplicationModule module) {
		ApplicationInfo applicationInfo = super.detectApplicationInfo(module);
		String framework = CloudUtil.getFramework(module);
		applicationInfo.setFramework(framework);
		return applicationInfo;
	}

	public File getWarFile() {
		if (filePath != null) {
			return new File(filePath);
		}
		return null;
	}

	protected String getInitialValue() {
		ApplicationInfo info = getLastApplicationInfo();
		return info != null ? info.getFramework() : null;
	}

	public ApplicationInfo getApplicationInfo() {
		ApplicationInfo info = super.getApplicationInfo();
		info.setFramework(getSelectedValue());
		return info;
	}

	protected String getValueLabel() {
		return "Application Type";
	}

}
