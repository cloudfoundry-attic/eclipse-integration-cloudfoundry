/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
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
		return new EditorOperation() {

			@Override
			protected void performEditorOperation(IProgressMonitor monitor) throws CoreException {
				// Do Nothing. Use the base editor operation refreshing
				// mechanism as the refresh action
			}
		};
	}

}
