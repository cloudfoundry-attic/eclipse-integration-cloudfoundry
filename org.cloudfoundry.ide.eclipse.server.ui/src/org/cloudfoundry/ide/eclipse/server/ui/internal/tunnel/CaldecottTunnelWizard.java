/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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

import java.util.Set;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.jface.wizard.Wizard;

public class CaldecottTunnelWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CaldecottTunnelWizardPage page;

	public CaldecottTunnelWizard(CloudFoundryServer cloudServer) {
		super();
		this.cloudServer = cloudServer;

		setWindowTitle(Messages.CaldecottTunnelWizard_TITLE_ACTIVE_TUNNEL);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new CaldecottTunnelWizardPage(cloudServer);
		addPage(page);
	}
	
	public Set<CaldecottTunnelDescriptor> getDescriptorsToRemove() {
		return page != null ? page.getDescriptorsToRemove() : null;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
