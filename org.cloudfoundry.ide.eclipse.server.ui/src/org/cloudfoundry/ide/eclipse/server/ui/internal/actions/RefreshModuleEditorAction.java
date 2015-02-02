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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.wst.server.core.IModule;

/**
 * Refreshes a single module selected in the given editor page, as well as its
 * related instances and stats.
 * <p/>
 * No refresh occurs is no module is selected in the editor page.
 */
public class RefreshModuleEditorAction extends EditorAction {

	protected RefreshModuleEditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		super(editorPage, area, Messages.RefreshApplicationEditorAction_TEXT_REFRESH, CloudFoundryImages.REFRESH);
		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText(Messages.RefreshApplicationEditorAction_TEXT_REFRESH);
	}

	@Override
	public String getJobName() {
		return "Refresh application"; //$NON-NLS-1$
	}

	@Override
	protected IStatus display404Error(IStatus status) {
		IModule currentModule = getModule();
		int type = IMessageProvider.WARNING;
		String message = currentModule != null ? Messages.RefreshApplicationEditorAction_WARNING_CANNOT_REFRESH
				: Messages.RefreshApplicationEditorAction_MSG_REFRESH_NEEDED;
		return CloudFoundryPlugin.getStatus(message, type);
	}

	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}

	@Override
	protected ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return getBehaviour().operations().refreshInstances(getModule());
	}

}