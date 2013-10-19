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

import java.lang.reflect.InvocationTargetException;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.springframework.util.Assert;

/**
 * Allows an application's environment variables to be edited and set in the
 * app's deployment info. The environment variables are also set in the server.
 */
public class EnvVarsWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	private DeploymentInfoWorkingCopy infoWorkingCopy;

	private CloudFoundryApplicationEnvVarWizardPage envVarPage;

	public EnvVarsWizard(CloudFoundryServer server, CloudFoundryApplicationModule appModule) {
		Assert.notNull(server);
		Assert.notNull(appModule);
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
		this.appModule = appModule;
	}

	@Override
	public void addPages() {
		infoWorkingCopy = appModule.getDeploymentInfoWorkingCopy();

		envVarPage = new CloudFoundryApplicationEnvVarWizardPage(cloudServer, infoWorkingCopy);
		envVarPage.setWizard(this);
		addPage(envVarPage);
	}

	@Override
	public boolean performFinish() {
		infoWorkingCopy.save();
		final IStatus[] result = new IStatus[1];
		try {

			envVarPage.setMessage("Updating environment variables. Please wait while the process completes.");
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						cloudServer.getBehaviour().updateEnvironmentVariables(appModule, monitor);

					}
					catch (CoreException e) {
						result[0] = e.getStatus();
					}
				}
			});
			envVarPage.setMessage(null);
		}
		catch (InvocationTargetException e) {
			result[0] = CloudFoundryPlugin.getErrorStatus(e);
		}
		catch (InterruptedException e) {
			result[0] = CloudFoundryPlugin.getErrorStatus(e);

		}
		if (result[0] != null && !result[0].isOK()) {
			envVarPage.setErrorMessage("Environment variables may not have changed correctly due to: "
					+ result[0].getMessage());
			return false;
		}
		else {
			return true;
		}
	}

}
