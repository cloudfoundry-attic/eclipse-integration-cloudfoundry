/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Terry Denney
 */
public class UpdateApplicationMemoryAction extends CloudFoundryEditorAction {

	private final int memory;

	private final CloudFoundryApplicationModule module;

	public UpdateApplicationMemoryAction(CloudFoundryApplicationsEditorPage editorPage, int memory,
			CloudFoundryApplicationModule module) {
		super(editorPage, RefreshArea.DETAIL);
		this.memory = memory;
		this.module = module;
	}

	@Override
	public String getJobName() {
		return "Updating application memory limit";
	}

	@Override
	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ModifyEditorOperation() {

			@Override
			protected void performOperation(IProgressMonitor monitor) throws CoreException {
				getBehavior().updateApplicationMemory(module, memory, monitor);
			}
		};
	}

}
