/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommandHelper;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ExternalToolLaunchCommandsServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.PreferencePage;
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
			List<ExternalToolLaunchCommandsServer> actualServers = new ServiceCommandHelper().getUpdatedServerServiceCommands(null);
			part = new ServiceTunnelCommandPart(actualServers);
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
			return part.createControl(parent);
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
			List<ExternalToolLaunchCommandsServer> updatedServers = part.getUpdatedServers();
			try {
				new ServiceCommandHelper().saveServerServiceCommands(updatedServers, null);
			}
			catch (CoreException e) {
				setErrorMessage("Failed to save command preferences: " + e.getMessage());
			}
		}
	}
}
