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

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class StartStopApplicationAction extends CloudFoundryEditorAction {

	private final ApplicationAction action;

	private final CloudFoundryApplicationModule application;

	private final IModule module;

	private final CloudFoundryServerBehaviour serverBehaviour;

	public StartStopApplicationAction(CloudFoundryApplicationsEditorPage editorPage, ApplicationAction action,
			CloudFoundryApplicationModule application, CloudFoundryServerBehaviour serverBehaviour, IModule module) {
		super(editorPage, RefreshArea.DETAIL);
		this.action = action;
		this.application = application;
		this.serverBehaviour = serverBehaviour;
		this.module = module;
	}

	@Override
	public String getJobName() {
		StringBuilder jobName = new StringBuilder();
		switch (action) {
		case START:
			jobName.append("Starting");
			break;
		case STOP:
			jobName.append("Stopping");
			break;
		case RESTART:
			jobName.append("Restarting");
			break;
		case UPDATE_RESTART:
			jobName.append("Update and Restarting");
			break;
		}

		jobName.append(" application " + application.getDeployedApplicationName());
		return jobName.toString();
	}

	public ICloudFoundryOperation getOperation() throws CoreException {
		ICloudFoundryOperation operation = null;
		IModule[] modules = new IModule[] { module };
		switch (action) {
		case START:
			operation = serverBehaviour.getDeployStartApplicationOperation(modules, false);
			break;
		case STOP:
			operation = serverBehaviour.getStopAppOperation(modules);
			break;
		case RESTART:
			operation = serverBehaviour.getRestartOperation(modules);
			break;
		case UPDATE_RESTART:
			operation = serverBehaviour.getUpdateRestartOperation(modules, getIncrementalPublish());
			break;
		}
		return operation;
	}

	protected boolean getIncrementalPublish() {
		return CloudFoundryPlugin.getDefault().getIncrementalPublish();
	}

}
