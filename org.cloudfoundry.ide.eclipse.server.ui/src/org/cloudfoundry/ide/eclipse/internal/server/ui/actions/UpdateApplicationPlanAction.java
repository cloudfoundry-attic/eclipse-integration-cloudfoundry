/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationPlan;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class UpdateApplicationPlanAction extends CloudFoundryEditorAction {

	private final ApplicationModule module;
	
	private ApplicationPlan updatedPlan;

	public UpdateApplicationPlanAction(CloudFoundryApplicationsEditorPage editorPage, ApplicationModule module, ApplicationPlan updatedPlan) {
		super(editorPage, RefreshArea.DETAIL);
		this.module = module;
		this.updatedPlan = updatedPlan;
	}

	@Override
	public String getJobName() {
		return "Updating application plan";
	}

	@Override
	protected IStatus performAction(IProgressMonitor monitor) throws CoreException {
		getBehavior().updateApplicationPlan(module, updatedPlan, monitor);
		return Status.OK_STATUS;
	}

}
