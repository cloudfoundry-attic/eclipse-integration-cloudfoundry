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

import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.EditorAction.RefreshArea;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.jface.action.Action;
import org.eclipse.wst.server.core.IModule;

/**
 * Performs a full refresh of all published modules. This may be a long running
 * operation, especially with Cloud spaces with a large list of published
 * applications. In addition, it also updates the instances and stats of any
 * selected module in the editor
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RefreshEditorAction extends Action {

	private final CloudFoundryApplicationsEditorPage editorPage;

	public RefreshEditorAction(CloudFoundryApplicationsEditorPage editorPage) {

		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText(Messages.RefreshApplicationEditorAction_TEXT_REFRESH);

		this.editorPage = editorPage;
	}

	/**
	 * Returns a refresh editor action appropriate to the area being refreshed.
	 * @param editorPage
	 * @param area to refresh
	 * @return Editor action for the given area. If no area is specified,
	 * returns a general refresh action. Never null.
	 */
	public static Action getRefreshAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {

		if (area == RefreshArea.DETAIL && editorPage.getMasterDetailsBlock().getCurrentModule() != null) {
			return new RefreshModuleEditorAction(editorPage);
		}
		else {
			return new RefreshEditorAction(editorPage);
		}
	}

	@Override
	public void run() {
		IModule selectedModule = editorPage.getMasterDetailsBlock().getCurrentModule();
		CloudFoundryServerBehaviour behaviour = editorPage.getCloudServer().getBehaviour();
		behaviour.getRefreshHandler().scheduleRefresh(behaviour.operations().refreshAll(selectedModule));
	}
}