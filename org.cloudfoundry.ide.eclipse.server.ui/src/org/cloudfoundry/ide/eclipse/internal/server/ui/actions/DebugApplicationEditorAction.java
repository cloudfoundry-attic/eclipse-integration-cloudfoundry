/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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

	public ICloudFoundryOperation getOperation() throws CoreException {
		return new ICloudFoundryOperation() {

			public void run(IProgressMonitor monitor) throws CoreException {
				command.run(monitor);
			}
		};
	}
}
