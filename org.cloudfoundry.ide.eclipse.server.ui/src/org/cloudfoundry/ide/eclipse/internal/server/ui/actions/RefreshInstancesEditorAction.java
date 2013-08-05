/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;

/**
 * Editor action that also refreshes application instances stats after the
 * action is completed.
 */
public abstract class RefreshInstancesEditorAction extends CloudFoundryEditorAction {

	public RefreshInstancesEditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		super(editorPage, area);
	}

	@Override
	protected IStatus refreshApplication(IModule module, RefreshArea area, IProgressMonitor monitor)
			throws CoreException {
		// Refresh instances stats
		return doRefreshApplication(module, area, true, monitor);
	}

}
