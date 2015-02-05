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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.EditorAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class AddServiceStartCaldecottAction extends EditorAction {

	private final String jobName;

	private List<String> services;

	private CloudFoundryServerBehaviour serverBehaviour;

	public AddServiceStartCaldecottAction(List<String> services, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage, String jobName) {
		// Null application module, as it is resolved only at action run time
		super(editorPage, RefreshArea.ALL, jobName, CloudFoundryImages.CONNECT);
		this.jobName = jobName;
		this.serverBehaviour = serverBehaviour;
		this.services = new ArrayList<String>(services);

	}

	@Override
	protected ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ICloudFoundryOperation() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				if (services != null && !services.isEmpty()) {
					TunnelBehaviour handler = new TunnelBehaviour(getBehaviour().getCloudFoundryServer());
					handler.startCaldecottTunnel(services.get(0), monitor, true);
				}
			}
		};
	}

}
