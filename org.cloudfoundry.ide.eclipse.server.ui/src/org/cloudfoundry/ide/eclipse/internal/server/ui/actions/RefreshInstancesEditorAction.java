/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Editor action that also refreshes application instances stats after the
 * action is completed.
 */
public abstract class RefreshInstancesEditorAction extends CloudFoundryEditorAction {

	public RefreshInstancesEditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		super(editorPage, area);
	}

	@Override
	protected ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ModifyEditorOperation() {

			@Override
			protected void performOperation(IProgressMonitor monitor) throws CoreException {
				getBehavior().refreshModules(monitor);
			}
		};
	}

}
