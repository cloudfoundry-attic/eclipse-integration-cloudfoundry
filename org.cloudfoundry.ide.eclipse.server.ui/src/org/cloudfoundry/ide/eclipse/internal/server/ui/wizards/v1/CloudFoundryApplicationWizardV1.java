/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.v1;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationFramework;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRuntime;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardProviderDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizard;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 * Old V1 Cloud Foundry deployment wizard that supported frameworks and
 * runtimes. These were replaced by buildpacks in V2. Kept only as a reference.
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 * @author Nieraj Singh
 * @deprecated
 */
public class CloudFoundryApplicationWizardV1 extends CloudFoundryApplicationWizard {

	private List<ApplicationFramework> frameworks;

	private List<ApplicationRuntime> runtimes;

	public CloudFoundryApplicationWizardV1(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardProviderDelegate provider) {
		super(server, module, provider);

		loadApplicationOptions();
		setWindowTitle("Application");
	}

	public List<ApplicationFramework> getFrameworks() {
		return frameworks;
	}

	public List<ApplicationRuntime> getRuntimes() {
		return runtimes;
	}

	protected ApplicationFramework getApplicationFramework(IApplicationDelegate delegate) throws CoreException {
		ApplicationFramework framework = delegate.getFramework(module.getLocalModule());

		if (framework == null) {
			String error = "Failed to push the application to the Cloud Foundry server because the application framework could not be resolved for: "
					+ server.getServerId();
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(error));

		}
		else {
			return framework;
		}

	}

	protected void loadApplicationOptions() {

		if (provider != null) {
			IApplicationDelegate delegate = provider.getApplicationDelegate();

			try {

				frameworks = delegate.getSupportedFrameworks();
				runtimes = delegate.getRuntimes(server);

				ApplicationFramework defaultFramework = getApplicationFramework(delegate);

				if (defaultFramework != null && runtimes != null && !runtimes.isEmpty()) {
					// Set a staging using the default framework and the first
					// runtime encountered. These values can then be changed by
					// the user
					applicationDescriptor.setStaging(defaultFramework, runtimes.get(0));
				}
				else {
					CloudFoundryPlugin
							.logError("Unable to resolve a default framework and runtime for application when publishing to a Cloud Foundry server: "
									+ module.getApplicationId()
									+ " of type: "
									+ module.getLocalModule().getModuleType().getId());
				}

			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
	}

}
