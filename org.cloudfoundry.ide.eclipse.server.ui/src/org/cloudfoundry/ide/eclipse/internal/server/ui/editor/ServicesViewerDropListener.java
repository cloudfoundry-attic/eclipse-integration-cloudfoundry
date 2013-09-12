/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.AddServicesToApplicationAction;
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
