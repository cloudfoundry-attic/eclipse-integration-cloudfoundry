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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;
import org.cloudfoundry.ide.eclipse.internal.server.core.RuntimeType;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.AbstractCloudFoundryApplicationWizardPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;
import org.eclipse.swt.widgets.Composite;

public class StandaloneApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {

	public StandaloneApplicationWizardPage(CloudFoundryServer server, CloudFoundryDeploymentWizardPage deploymentPage,
			ApplicationModule module) {
		super(server, deploymentPage, module, DeploymentConstants.STANDALONE_FRAMEWORK);
	}

	@Override
	protected Composite createContents(Composite parent) {
		List<RuntimeType> standaloneRuntimes = getApplicationWizard().getRuntimes();

		if (standaloneRuntimes.isEmpty()) {
			setErrorMessage("Unable to publish standalone application. Application runtime cannot be determined.");
		}
		else {
			setErrorMessage(null);
		}

		return super.createContents(parent);
	}

}
