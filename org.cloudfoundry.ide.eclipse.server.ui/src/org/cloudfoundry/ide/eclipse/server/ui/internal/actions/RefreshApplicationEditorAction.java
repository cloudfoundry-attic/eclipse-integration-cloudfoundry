/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.wst.server.core.IModule;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RefreshApplicationEditorAction extends RefreshInstancesEditorAction {

	public RefreshApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage) {
		this(editorPage, RefreshArea.ALL);
	}

	public RefreshApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		super(editorPage, area);

		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText(Messages.RefreshApplicationEditorAction_TEXT_REFRESH);
	}

	@Override
	public String getJobName() {
		return "Refresh application"; //$NON-NLS-1$
	}

	@Override
	protected void display404Error(IStatus status) {
		IModule currentModule = getEditorPage().getMasterDetailsBlock().getCurrentModule();
		if (currentModule != null) {
			getEditorPage().setMessage(Messages.RefreshApplicationEditorAction_WARNING_CANNOT_REFRESH,
					IMessageProvider.WARNING);
		}
		else {
			getEditorPage().setMessage(Messages.RefreshApplicationEditorAction_MSG_REFRESH_NEEDED,
					IMessageProvider.WARNING);
		}
	}

	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}

}
