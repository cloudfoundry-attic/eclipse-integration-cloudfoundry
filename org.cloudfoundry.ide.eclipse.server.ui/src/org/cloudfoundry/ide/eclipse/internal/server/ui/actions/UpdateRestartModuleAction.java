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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public class UpdateRestartModuleAction extends AbstractCloudFoundryServerAction {

	protected String getJobName() {
		return "Update and restarting module";
	}

	protected String getFailureMessage() {
		return "Unable to update and restart module";
	}

	public void run(IAction action) {
		Job job = new Job(getJobName()) {

			protected IStatus run(IProgressMonitor monitor) {
				CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer.loadAdapter(
						CloudFoundryServer.class, null);
				try {
					IModule[] modules = new IModule[] { selectedModule };
					if (CloudFoundryProperties.isApplicationRunningInDebugMode.testProperty(modules,
							getCloudFoundryServer())) {
						cloudServer.getBehaviour().updateRestartDebugModule(modules, getIncrementalPublish(), monitor);
					}
					else {
						cloudServer.getBehaviour()
								.updateRestartModuleRunMode(modules, getIncrementalPublish(), monitor);
					}

				}
				catch (CoreException e) {
					CloudFoundryPlugin.getDefault().getLog()
							.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, getFailureMessage(), e));
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	protected boolean getIncrementalPublish() {
		return CloudFoundryPlugin.getDefault().getIncrementalPublish();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	protected void serverSelectionChanged(IAction action) {
		if (selectedServer != null && (selectedServer.getServerState() == IServer.STATE_STARTED)) {
			CloudFoundryApplicationModule cloudModule = getSelectedCloudAppModule();
			if (cloudModule != null) {
				int state = cloudModule.getState();
				// Do not enable the action if the associated module project is
				// not accessible, as users shoudln't
				// be able to modify files within Eclipse if the project is not
				// accessible (i.e it is not open and writable in the workspace)
				if (state == IServer.STATE_STARTED
						&& !cloudModule.isExternal()
						&& CloudFoundryProperties.isModuleProjectAccessible.testProperty(
								new IModule[] { selectedModule }, getCloudFoundryServer())) {
					action.setEnabled(true);
					return;
				}
			}
		}
		action.setEnabled(false);

	}

}
