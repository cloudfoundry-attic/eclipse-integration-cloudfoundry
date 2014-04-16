/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.rse;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.rse.core.IRSECoreRegistry;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.ui.view.SystemPerspectiveHelpers;
import org.eclipse.rse.internal.ui.view.SystemView;
import org.eclipse.rse.internal.ui.view.SystemViewPart;
import org.eclipse.ui.PlatformUI;



/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
@SuppressWarnings("restriction")
public class ConfigureRemoteCloudFoundryAction extends Action {

	private CloudFoundryServer server;

	private String serverTypeId;

	public ConfigureRemoteCloudFoundryAction(CloudFoundryServer server) {
		super();
		this.server = server;
		serverTypeId = server.getServer().getServerType().getId();
	}

	@Override
	public void run() {
		IRSECoreRegistry coreReg = RSECorePlugin.getTheCoreRegistry();
		ISystemRegistry sysReg = RSECorePlugin.getTheSystemRegistry();
		String systemTypeId = CloudFoundryBrandingExtensionPoint.getRemoteSystemTypeId(serverTypeId);
		if (systemTypeId != null && systemTypeId.trim().length() > 0) {
			IRSESystemType systemType = coreReg.getSystemTypeById(systemTypeId);
			IHost host = null;
			IHost[] hosts = sysReg.getHostsBySystemType(systemType);
			if (hosts.length >= 1) {
				host = hosts[0];
			}

			if (host == null) {
				try {
					host = sysReg.createHost(systemType, systemType.getLabel(), server.getUrl(), "");
					ISubSystem[] subSystems = sysReg.getSubSystems(host);
					CloudFoundryConnectorServiceManager serviceManager = CloudFoundryConnectorServiceManager.getInstance();
					CloudFoundryConnectorService service = (CloudFoundryConnectorService) serviceManager
							.createConnectorService(host);
					for (int i = 0; i < subSystems.length; i++) {
						service.registerSubSystem(subSystems[i]);
					}
				}
				catch (Exception e) {
					CloudFoundryRsePlugin.logError("An error occurred while connecting to service.", e);
				}
			}

			if (host != null) {
				SystemViewPart viewPart = (SystemViewPart) SystemPerspectiveHelpers.showView(SystemViewPart.ID);
				final SystemView view = viewPart.getSystemView();
				ISubSystem[] subSystems = sysReg.getSubSystems(host);
				if (subSystems.length > 0) {
					final ISubSystem system = subSystems[0];
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							view.setExpandedState(system.getHost(), true);
							view.setExpandedState(system, true);
							view.setSelection(new StructuredSelection(system));
						}
					});
				}
			}
		}
	}

}
