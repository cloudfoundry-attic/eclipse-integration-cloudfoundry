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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.TunnelServiceCommandStore;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.server.ui.internal.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ServiceTunnelCommandPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private ServiceTunnelCommandPart part;

	public ServiceTunnelCommandPreferencePage() {
		setPreferenceStore(CloudFoundryServerUiPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {
		// Nothing for now
	}

	@Override
	protected Control createContents(Composite parent) {
		try {
			ITunnelServiceCommands commands = TunnelServiceCommandStore.getCurrentStore().getTunnelServiceCommands();

			part = new ServiceTunnelCommandPart(commands, null);
			part.addPartChangeListener(new IPartChangeListener() {

				public void handleChange(PartChangeEvent event) {
					if (event != null) {
						IStatus status = event.getStatus();

						if (status != null && !status.isOK()) {
							setErrorMessage(status.getMessage());
						}
						else {
							setErrorMessage(null);
						}
					}
				}
			});
			return part.createPart(parent);
		}
		catch (CoreException e) {
			setErrorMessage(e.getMessage());
		}
		return null;
	}

	@Override
	protected void performApply() {
		handleServerServiceCommandSave();
		super.performApply();
	}

	@Override
	public boolean performOk() {
		handleServerServiceCommandSave();
		return super.performOk();
	}

	public void handleServerServiceCommandSave() {
		if (part != null) {
			ITunnelServiceCommands updatedCommands = part.getUpdatedCommands();
			try {
				TunnelServiceCommandStore.getCurrentStore().storeServerServiceCommands(updatedCommands);
			}
			catch (CoreException e) {
				setErrorMessage(NLS.bind(Messages.ServiceTunnelCommandPreferencePage_ERROR_FAIL_TO_SAVE, e.getMessage()));
			}
		}
	}
}
