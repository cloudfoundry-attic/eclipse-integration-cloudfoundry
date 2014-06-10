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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceInfo;
import org.eclipse.jface.wizard.Wizard;

public class TunnelCommandDefinitionWizard extends Wizard {



	private ITunnelServiceCommands originalCommands;

	private TunnelCommandDefinitionWizardPage page;

	private final ServiceInfo serviceContext;

	public TunnelCommandDefinitionWizard(ITunnelServiceCommands originalCommands, ServiceInfo serviceContext) {
		super();
		this.originalCommands = originalCommands;
		this.serviceContext = serviceContext;
		setWindowTitle("Service Tunnel Commands");
		setNeedsProgressMonitor(true);
	}

	public ServiceInfo getServiceContext() {
		return serviceContext;
	}

	@Override
	public void addPages() {
		page = new TunnelCommandDefinitionWizardPage(originalCommands, null);
		addPage(page);
	}

	public ITunnelServiceCommands getExternalToolLaunchCommandsServer() {
		return page != null ? page.getExtToolLaunchCommandsServer() : originalCommands;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
