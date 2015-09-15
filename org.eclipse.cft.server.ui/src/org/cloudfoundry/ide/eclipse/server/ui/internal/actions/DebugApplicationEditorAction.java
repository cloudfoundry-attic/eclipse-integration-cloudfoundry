/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.ui.internal.DebugCommand;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class DebugApplicationEditorAction extends EditorAction {

	private final DebugCommand debugCommand;

	public DebugApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage, DebugCommand debugCommand) {
		super(editorPage, RefreshArea.DETAIL);
		this.debugCommand = debugCommand;
	}

	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ICloudFoundryOperation() {

			public void run(IProgressMonitor monitor) throws CoreException {
				if (debugCommand != null) {
					runDebugOperation(debugCommand, monitor);
				}
			}
		};
	}

	protected void runDebugOperation(DebugCommand command, IProgressMonitor monitor) throws CoreException {
		command.debug(monitor);
	}

	public String getJobName() {
		return getOperationLabel()
				+ " - " + debugCommand.getLaunch().getApplicationModule().getDeployedApplicationName(); //$NON-NLS-1$
	}

	protected String getOperationLabel() {
		return "Connecting to debugger"; //$NON-NLS-1$
	}
}
