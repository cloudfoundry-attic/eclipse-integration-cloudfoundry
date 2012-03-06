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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


public class DebugApplicationEditorAction extends CloudFoundryEditorAction {

	private final DebugCommand command;

	public DebugApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage, DebugCommand command) {
		super(editorPage, RefreshArea.DETAIL);

		this.command = command;
	}

	public String getJobName() {
		StringBuilder jobName = new StringBuilder();

		jobName.append(command.getCommandName());
		jobName.append(" application " + command.getApplicationID());
		return jobName.toString();
	}

	public IStatus performAction(IProgressMonitor monitor) throws CoreException {
		command.run(monitor);
		return Status.OK_STATUS;
	}
}
