/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.DebugConnectionDescriptor;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.IDebugProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;

/**
 * Wrapper around a {@link IDebugProvider} that performs UI-aware provider
 * operations, for example prompting the user for additional information via
 * dialogues.
 */
class DebugUIProvider implements IDebugProvider {

	private final IDebugProvider provider;

	public DebugUIProvider(IDebugProvider provider) {
		this.provider = provider;
	}

	@Override
	public DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException {
		return provider.getDebugConnectionDescriptor(appModule, cloudServer, monitor);
	}

	@Override
	public boolean canLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		return provider.canLaunch(appModule, cloudServer, monitor);
	}

	@Override
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		return provider.isDebugSupported(appModule, cloudServer);
	}

	@Override
	public String getLaunchConfigurationID() {
		return provider.getLaunchConfigurationID();
	}

	@Override
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		IModule[] mod = new IModule[] { appModule.getLocalModule() };

		// If app is stopped then it is already in a state where it can be
		// launched. Otherwise if application is running but it also cannot
		// be launched in debug mode yet because it still needs to be
		// configured, prompt the
		// user whether to restart the application.
		boolean shouldRestart = CloudFoundryProperties.isModuleStopped.testProperty(mod, cloudServer);

		// If the application cannot yet launch and it is running, then it may
		// require stopping the application
		// before configuring it to launch in debug mode.
		if (!shouldRestart && !provider.canLaunch(appModule, cloudServer, monitor)) {

			final boolean[] shouldProceed = new boolean[] { true };

			// Ask if the module should be restarted in its current state.
			Display.getDefault().syncExec(new Runnable() {

				public void run() {
					Shell shell = CloudUiUtil.getShell();
					shouldProceed[0] = MessageDialog.openQuestion(shell, Messages.DebugUIProvider_DEBUG_TITLE,
							Messages.DebugUIProvider_DEBUG_APP_RESTART_MESSAGE);
				}

			});

			shouldRestart = shouldProceed[0];
			if (!shouldProceed[0]) {
				return false;
			}
		}
		provider.configureApp(appModule, cloudServer, monitor);

		if (shouldRestart) {
			cloudServer.getBehaviour().operations()
					.applicationDeployment(mod, ApplicationAction.UPDATE_RESTART).run(monitor);
		}
		return true;

	}
}