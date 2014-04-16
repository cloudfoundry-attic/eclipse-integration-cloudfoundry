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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RemoveServicesFromApplicationAction extends ModifyServicesForApplicationAction {

	private final List<String> services;

	public RemoveServicesFromApplicationAction(IStructuredSelection selection, CloudFoundryApplicationModule application,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {
		super(application, serverBehaviour, editorPage);

		setText("Unbind from Application");
		setImageDescriptor(CloudFoundryImages.REMOVE);

		services = getServiceNames(selection);
	}

	@Override
	public String getJobName() {
		return "Unbinding services";
	}

	@Override
	public List<String> getServicesToAdd() {
		return new ArrayList<String>();
	}

	@Override
	public List<String> getServicesToRemove() {
		return services;
	}

	protected void updateServicesInClient(IProgressMonitor monitor, CloudFoundryApplicationModule appModule,
			CloudFoundryServerBehaviour serverBehaviour, List<String> updatedServices) throws CoreException {
		serverBehaviour.updateServicesAndCloseCaldecottTunnels(appModule.getDeployedApplicationName(), updatedServices, monitor);
	}

}
