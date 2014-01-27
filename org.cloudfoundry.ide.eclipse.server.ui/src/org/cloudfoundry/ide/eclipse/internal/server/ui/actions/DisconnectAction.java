/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.ui.IServerModule;



/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class DisconnectAction implements IObjectActionDelegate, IViewActionDelegate {

	private IModule selectedModule;

	private IServer selectedServer;

	public DisconnectAction() {
	}

	public void init(IViewPart view) {
	}

	public void run(IAction action) {
		final CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer.loadAdapter(CloudFoundryServer.class, null);
		Job disconnectJob = new Job("Disconnecting server") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					cloudServer.getBehaviour().disconnect(monitor);
				}
				catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				catch (CoreException e) {
//					Trace.trace(Trace.STRING_SEVERE, "Error calling disconnect() ", e);
					return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, NLS.bind(
							"Failed to disconnect from server: {0}", e.getMessage()));
				}
				return Status.OK_STATUS;
			}
		};
		disconnectJob.schedule();
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		selectedServer = null;
		selectedModule = null;
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof IServer) {
					selectedServer = (IServer) obj;
				}
				else if (obj instanceof IServerModule) {
					IServerModule sm = (IServerModule) obj;
					IModule[] module = sm.getModule();
					selectedModule = module[module.length - 1];
					if (selectedModule != null) {
						selectedServer = sm.getServer();
					}
				}
			}
		}
		if (selectedServer != null) {
			action.setEnabled(selectedServer.getServerState() == IServer.STATE_STARTED);
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
