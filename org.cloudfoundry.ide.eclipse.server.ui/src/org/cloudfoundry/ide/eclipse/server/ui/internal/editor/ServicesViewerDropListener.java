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
package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.AddServicesToApplicationAction;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;


/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class ServicesViewerDropListener extends ViewerDropAdapter {

//	private CloudApplication application;

	private final CloudFoundryApplicationsEditorPage editorPage;

	private CloudFoundryApplicationModule appModule;

	private final CloudFoundryServerBehaviour serverBehaviour;

	protected ServicesViewerDropListener(Viewer viewer, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		super(viewer);
		this.serverBehaviour = serverBehaviour;
		this.editorPage = editorPage;
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT || event.detail == DND.DROP_NONE) {
			event.detail = DND.DROP_COPY;
		}
		super.dragEnter(event);
	}

	@Override
	public boolean performDrop(Object data) {
		IStructuredSelection selection = (IStructuredSelection) data;
		new AddServicesToApplicationAction(selection, appModule, serverBehaviour, editorPage).run();

		return true;
	}

	public void setModule(CloudFoundryApplicationModule module) {
		this.appModule = module;
		
//		if (module == null) {
//			this.application = null;
//		} else {
//			this.application = module.getApplication();
//		}
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData type) {
		overrideOperation(DND.DROP_COPY);
//		if (application == null)
//			return false;

		if (operation == DND.DROP_COPY || operation == DND.DROP_DEFAULT) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(type)) {
				IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer()
						.getSelection();
				Object[] objects = selection.toArray();
				for (Object obj : objects) {
					if (obj instanceof CloudService) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
