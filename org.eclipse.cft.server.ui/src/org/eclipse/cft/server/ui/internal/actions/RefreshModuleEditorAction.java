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
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.jface.action.Action;
import org.eclipse.wst.server.core.IModule;

/**
 * Refreshes a single module selected in the given editor page, as well as its
 * related instances and stats.
 * <p/>
 * No refresh occurs is no module is selected in the editor page.
 */
public class RefreshModuleEditorAction extends Action {

	private final CloudFoundryApplicationsEditorPage editorPage;

	protected RefreshModuleEditorAction(CloudFoundryApplicationsEditorPage editorPage) {
		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText(Messages.RefreshApplicationEditorAction_TEXT_REFRESH);
		this.editorPage = editorPage;
	}

	@Override
	public void run() {
		IModule selectedModule = editorPage.getMasterDetailsBlock().getCurrentModule();
		CloudFoundryServerBehaviour behaviour = editorPage.getCloudServer().getBehaviour();
		behaviour.getRefreshHandler().schedulesRefreshApplication(selectedModule);
	}

}