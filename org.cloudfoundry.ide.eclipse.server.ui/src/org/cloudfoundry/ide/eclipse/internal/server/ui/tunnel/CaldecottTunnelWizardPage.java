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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CaldecottTunnelWizardPage extends WizardPage {

	private final CloudFoundryServer cloudServer;

	private final Set<CaldecottTunnelDescriptor> removeDescriptors = new HashSet<CaldecottTunnelDescriptor>();

	private TunnelDisplayPart part;

	public CaldecottTunnelWizardPage(CloudFoundryServer cloudServer) {
		super("Active Tunnels");
		this.cloudServer = cloudServer;
		setTitle("Active Tunnels");
		setDescription("Manage active tunnels");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {
		part = new TunnelDisplayPart(getShell(), cloudServer, null) {

			@Override
			protected List<IAction> getViewerActions(List<CaldecottTunnelDescriptor> descriptors) {
				List<IAction> actions = super.getViewerActions(descriptors);
				actions = actions != null ? new ArrayList<IAction>(actions) : new ArrayList<IAction>();

				if (!descriptors.isEmpty()) {
					Action caldecottAction = new DeleteTunnelAction("Disconnect", CloudFoundryImages.DISCONNECT);
					actions.add(caldecottAction);

				}
				return actions;
			}

		};
		Control area = part.createControl(parent);

		setControl(area);
	}

	public Set<CaldecottTunnelDescriptor> getDescriptorsToRemove() {
		return removeDescriptors;
	}

	protected class DeleteTunnelAction extends Action {

		public DeleteTunnelAction(String actionName, ImageDescriptor actionImage) {
			super(actionName, actionImage);
		}

		public void run() {

			Collection<CaldecottTunnelDescriptor> descriptors = (Collection) part.getViewer().getInput();
			if (descriptors == null || descriptors.isEmpty()) {
				return;
			}

			List<CaldecottTunnelDescriptor> selectedDescriptors = part.getSelectedCaldecotTunnelDescriptors();

			for (CaldecottTunnelDescriptor desc : selectedDescriptors) {
				if (!removeDescriptors.contains(desc)) {
					removeDescriptors.add(desc);
				}
			}

			descriptors = new HashSet<CaldecottTunnelDescriptor>(descriptors);
			if (!removeDescriptors.isEmpty()) {
				for (Iterator<?> it = removeDescriptors.iterator(); it.hasNext();) {
					Object obj = it.next();

					if (obj instanceof CaldecottTunnelDescriptor) {
						descriptors.remove(obj);

					}
				}

				part.getViewer().setInput(descriptors);
				part.getViewer().refresh();
			}
		}

		public String getToolTipText() {
			return "Remove the selected connection(s)";
		}
	};

}
