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

import java.util.LinkedHashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;
import org.eclipse.swt.widgets.Composite;

public class StandaloneApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {

	public StandaloneApplicationWizardPage(CloudFoundryServer server, CloudFoundryDeploymentWizardPage deploymentPage,
			ApplicationModule module) {
		super(server, deploymentPage, module);
	}

	@Override
	protected Map<String, String> getValuesByLabel() {
		// Rails, Spring, Grails, Roo, JavaWeb, Sinatra, Node
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put("Java", DeploymentConstants.JAVA_RUNTIME);
		valuesByLabel.put("Node", DeploymentConstants.NODE_RUNTIME);
		valuesByLabel.put("Node06", DeploymentConstants.NODE06_RUNTIME);
		valuesByLabel.put("Ruby18", DeploymentConstants.RUBY18);
		valuesByLabel.put("Ruby19", DeploymentConstants.RUBY19);
		return valuesByLabel;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		setDescription("Standalone application detected. " + DEFAULT_DESCRIPTION);
	}

	@Override
	protected String getComparisonValue() {
		return DeploymentConstants.JAVA_RUNTIME;
	}

	@Override
	protected String getValueLabel() {
		return "Runtime";
	}

	public String getRuntime() {
		return getSelectedValue();
	}

}
