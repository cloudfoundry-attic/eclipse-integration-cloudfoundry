/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.wst.server.core.IModule;

public class CaldecottEditorActionAdapter {

	private final CloudFoundryApplicationsEditorPage editorPage;

	private final CloudFoundryServerBehaviour behaviour;

	public CaldecottEditorActionAdapter(CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		this.behaviour = serverBehaviour;
		this.editorPage = editorPage;
	}

	public void addServiceAndCreateTunnel(ISelection selection) {

		IModule caldecottModule = behaviour.getCaldecottModule();
		if (caldecottModule instanceof ApplicationModule && selection instanceof IStructuredSelection) {
			new StartAndAddCaldecottService((IStructuredSelection) selection, (ApplicationModule) caldecottModule,
					behaviour, editorPage).run();

		}

	}

}
