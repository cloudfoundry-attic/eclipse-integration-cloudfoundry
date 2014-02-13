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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class UpdateInstanceCountAction extends CloudFoundryEditorAction {

	private final int instanceCount;

	private final CloudFoundryApplicationModule module;

	private final Spinner instanceSpinner;

	public UpdateInstanceCountAction(CloudFoundryApplicationsEditorPage editorPage, Spinner instanceSpinner,
			CloudFoundryApplicationModule module) {
		super(editorPage, RefreshArea.DETAIL);
		this.instanceSpinner = instanceSpinner;
		this.instanceCount = instanceSpinner.getSelection();
		this.module = module;
	}

	@Override
	public String getJobName() {
		return "Updating instance count";
	}

	@Override
	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {

		return new EditorOperation() {

			@Override
			protected void performEditorOperation(IProgressMonitor monitor) throws CoreException {

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						instanceSpinner.setEnabled(false);
					}
				});
				getBehavior().updateApplicationInstances(module, instanceCount, monitor);
				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						instanceSpinner.setEnabled(true);
					}
				});
			}
		};
	}

}
