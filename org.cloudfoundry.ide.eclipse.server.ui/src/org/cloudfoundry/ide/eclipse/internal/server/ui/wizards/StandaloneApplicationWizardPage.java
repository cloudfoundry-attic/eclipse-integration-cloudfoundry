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
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneRuntimeType;
import org.eclipse.swt.widgets.Composite;

public class StandaloneApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {

	public StandaloneApplicationWizardPage(CloudFoundryServer server, CloudFoundryDeploymentWizardPage deploymentPage,
			ApplicationModule module) {
		super(server, deploymentPage, module);
	}

	@Override
	protected Map<String, String> getValuesByLabel() {
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put(StandaloneRuntimeType.Java.name(), StandaloneRuntimeType.Java.getId());
		valuesByLabel.put(StandaloneRuntimeType.Node.name(), StandaloneRuntimeType.Node.getId());
		valuesByLabel.put(StandaloneRuntimeType.Node06.name(), StandaloneRuntimeType.Node06.getId());
		valuesByLabel.put(StandaloneRuntimeType.Ruby18.name(), StandaloneRuntimeType.Ruby18.getId());
		valuesByLabel.put(StandaloneRuntimeType.Ruby19.name(), StandaloneRuntimeType.Ruby19.getId());

		return valuesByLabel;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		setDescription("Standalone application detected. " + DEFAULT_DESCRIPTION);
	}

	@Override
	protected String getComparisonValue() {
		return StandaloneRuntimeType.Java.name();
	}

	@Override
	protected String getValueLabel() {
		return "Runtime";
	}

	public String getRuntime() {
		return getSelectedValue();
	}

}
