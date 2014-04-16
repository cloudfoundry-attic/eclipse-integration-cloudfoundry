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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
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
		setText("Refresh");
	}

	@Override
	public String getJobName() {
		return "Refresh application";
	}

	@Override
	protected void display404Error(IStatus status) {
		IModule currentModule = getEditorPage().getMasterDetailsBlock().getCurrentModule();
		if (currentModule != null) {
			getEditorPage().setMessage("Local module is not yet deployed. Cannot refresh with server.",
					IMessageProvider.WARNING);
		}
		else {
			getEditorPage().setMessage("Status is not up to date with server. Refresh needed.",
					IMessageProvider.WARNING);
		}
	}

	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}

}
