/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

/**
 * @author Steffen Pingel
 */
public class ShowConsoleEditorAction extends Action {

	private final CloudFoundryServer server;

	private final CloudFoundryApplicationModule appModule;

	private final int instanceIndex;

	private final String SHOWING_CONSOLE = "Fetching console contents. Please wait...";

	private final String NO_CALLBACK_UNABLE_TO_REFRESH_CONSOLE = "No Cloud Foundry console callback available. Unable to refresh console contents.";

	public ShowConsoleEditorAction(CloudFoundryServer server, CloudFoundryApplicationModule appModule, int instanceIndex) {
		this.server = server;
		this.appModule = appModule;
		this.instanceIndex = instanceIndex;
		setText("Show Console");
	}

	@Override
	public void run() {
		Job job = new Job(SHOWING_CONSOLE) {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				if (CloudFoundryPlugin.getCallback() != null) {
					CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, server);

					CloudFoundryPlugin.getCallback().printToConsole(server, appModule, SHOWING_CONSOLE, true, false,
							monitor);

					CloudFoundryPlugin.getCallback().showCloudFoundryLogs(server, appModule, instanceIndex);
					return Status.OK_STATUS;
				}
				else {
					return CloudFoundryPlugin.getErrorStatus(NO_CALLBACK_UNABLE_TO_REFRESH_CONSOLE);
				}
			}

		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();

	}

}
