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

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneRuntimeType;
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
		Composite composite = super.createContents(parent);
		StandaloneRuntimeType type = null;
		if (getWizard() instanceof CloudFoundryApplicationWizard) {
			CloudFoundryApplicationWizard appWizard = (CloudFoundryApplicationWizard) getWizard();
			type = appWizard.getStandaloneDescriptor().getRuntimeType();
		}

		if (type == null) {
			setErrorMessage("Unable to publish standalone application. Application runtime cannot be determined.");
		}
		else {
			setErrorMessage(null);
			createRuntimeArea(composite, type);
		}

		return composite;
	}

	protected void createRuntimeArea(Composite composite, StandaloneRuntimeType type) {
		Label runtimeLabel = new Label(composite, SWT.NONE);
		runtimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		runtimeLabel.setText("Runtime: ");

		Label runtime = new Label(composite, SWT.NONE);
		runtime.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
		runtime.setText(type.name());
	}

}
