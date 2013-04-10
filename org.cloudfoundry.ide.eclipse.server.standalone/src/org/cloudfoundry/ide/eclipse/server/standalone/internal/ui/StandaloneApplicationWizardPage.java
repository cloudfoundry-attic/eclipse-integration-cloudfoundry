/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizardPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;

public class StandaloneApplicationWizardPage extends
		CloudFoundryApplicationWizardPage {

	public StandaloneApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage,
			ApplicationModule module, ApplicationWizardDescriptor descriptor) {
		super(server, deploymentPage, module, descriptor);
	}
	
	@Override
	protected void setStaging() {
		// For standalone, be sure that the start command in the Staging
		// is not accidentally cleared when setting a new framework and
		// runtime
		if (selectedFramework != null && selectedRuntime != null) {
			Staging staging = descriptor.getStaging();
			if (staging == null) {
				descriptor.setStaging(selectedFramework,
						selectedRuntime);

			} else {
				String startCommand = staging.getCommand();
				staging = new Staging(selectedRuntime.getRuntime(),
						selectedFramework.getFramework());
				staging.setCommand(startCommand);
				descriptor.setStaging(staging);
			}
		}
	}
}
