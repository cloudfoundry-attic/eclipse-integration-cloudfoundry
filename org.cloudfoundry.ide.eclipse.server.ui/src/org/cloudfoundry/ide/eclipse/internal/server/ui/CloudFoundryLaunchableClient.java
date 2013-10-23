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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.OpenHomePageAction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ClientDelegate;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Steffen Pingel
 */
public class CloudFoundryLaunchableClient extends ClientDelegate {

	@Override
	public IStatus launch(IServer server, Object launchable, String launchMode, ILaunch launch) {
		final CloudFoundryLaunchable cfLaunchable = (CloudFoundryLaunchable) launchable;
		final CloudFoundryServer cfServer = (CloudFoundryServer) server.getAdapter(CloudFoundryServer.class);
		final CloudFoundryServerBehaviour behaviour = cfServer.getBehaviour();
		Job deploymentJob = new Job(NLS.bind("Deploying {0}", cfLaunchable.getModule().getName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final CloudFoundryApplicationModule module = behaviour.startModuleWaitForDeployment(
							new IModule[] { cfLaunchable.getModule() }, monitor);
					if (module == null) {
						return Status.CANCEL_STATUS;
					}
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							OpenHomePageAction.open(module);
						}
					});
					return Status.OK_STATUS;
				}
				catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				catch (CoreException e) {
					return new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
							"Deployment of {0} failed: {1}", cfLaunchable.getModule().getName(), e.getMessage()), e);
				}
			}
		};
		deploymentJob.schedule();
		return null;
	}

	@Override
	public boolean supports(IServer server, Object launchable, String launchMode) {
		return (launchable instanceof CloudFoundryLaunchable);
	}

}
