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
package org.cloudfoundry.ide.eclipse.internal.server.ui.standalone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneRuntimeType;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.AbstractCloudFoundryApplicationWizardPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class StandaloneApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {

	public StandaloneApplicationWizardPage(CloudFoundryServer server, CloudFoundryDeploymentWizardPage deploymentPage,
			ApplicationModule module) {
		super(server, deploymentPage, module);
	}

	@Override
	protected Composite createContents(Composite parent) {
		List<StandaloneRuntimeType> standaloneRuntimes = getApplicationWizard().getStandaloneHandler()
				.getRuntimeTypes();

		if (standaloneRuntimes.isEmpty()) {
			setErrorMessage("Unable to publish standalone application. Application runtime cannot be determined.");
		}
		else {
			setErrorMessage(null);
		}

		return super.createContents(parent);
	}

	protected CloudFoundryApplicationWizard getApplicationWizard() {
		return (CloudFoundryApplicationWizard) getWizard();
	}

	protected void createFrameworkArea(Composite composite) {
		List<StandaloneRuntimeType> standaloneRuntimes = getApplicationWizard().getStandaloneHandler()
				.getRuntimeTypes();
		if (standaloneRuntimes.size() == 1) {
			Label runtimeLabel = new Label(composite, SWT.NONE);
			runtimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			runtimeLabel.setText(getValueLabel() + ": ");

			Label runtime = new Label(composite, SWT.NONE);
			runtime.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
			runtime.setText(standaloneRuntimes.get(0).getLabel());
		}
		else {
			super.createFrameworkArea(composite);
		}

	}

	@Override
	public String getSelectedValue() {
		if (getApplicationWizard().getStandaloneHandler().getRuntimeTypes().size() == 1) {
			return getApplicationWizard().getStandaloneHandler().getRuntimeTypes().get(0).name();
		}
		else {
			return super.getSelectedValue();
		}
	}

	@Override
	protected Map<String, String> getValuesByLabel() {
		Map<String, String> runtimes = new HashMap<String, String>();
		List<StandaloneRuntimeType> standaloneRuntimes = getApplicationWizard().getStandaloneHandler()
				.getRuntimeTypes();
		for (StandaloneRuntimeType type : standaloneRuntimes) {
			runtimes.put(type.getLabel(), type.name());
		}
		return runtimes;
	}

	@Override
	protected String getValueLabel() {
		return "Runtime";
	}

	@Override
	protected String getInitialValue() {
		List<StandaloneRuntimeType> standaloneRuntimes = getApplicationWizard().getStandaloneHandler()
				.getRuntimeTypes();
		if (!standaloneRuntimes.isEmpty()) {
			return standaloneRuntimes.get(0).name();
		}
		return null;
	}

}
