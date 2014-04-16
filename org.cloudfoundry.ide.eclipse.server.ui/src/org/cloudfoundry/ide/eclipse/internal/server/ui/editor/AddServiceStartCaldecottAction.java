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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.TunnelActionProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class AddServiceStartCaldecottAction extends CloudFoundryEditorAction {

	private final String jobName;

	private List<String> services;

	public AddServiceStartCaldecottAction(List<String> services, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage, String jobName) {
		// Null application module, as it is resolved only at action run time
		super(editorPage, RefreshArea.ALL);
		this.jobName = jobName;
		setText(jobName);
		setImageDescriptor(CloudFoundryImages.CONNECT);
		this.services = new ArrayList<String>(services);

		/**
		 * FIXNS: Disabled for CF 1.5.0 until tunnel support at client level are
		 * updated.
		 */
		setEnabled(false);
		setToolTipText(TunnelActionProvider.DISABLED_V2_TOOLTIP_MESSAGE);
	}

	@Override
	public String getJobName() {
		return jobName;
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	@Override
	protected Job getJob() {
		Job job = super.getJob();
		// As starting a Caldecott tunnel may take time, show progress dialog
		// that also
		// allows the user to run it as a background job.
		job.setUser(true);
		return job;
	}

	@Override
	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {

		if (services != null && !services.isEmpty()) {
			return new ModifyEditorOperation() {

				@Override
				protected void performOperation(IProgressMonitor monitor) throws CoreException {
						TunnelBehaviour handler = new TunnelBehaviour(getBehavior().getCloudFoundryServer());
					handler.startCaldecottTunnel(services.get(0), monitor, true);
				}
				
			};
		}
		return null;

	}

}
