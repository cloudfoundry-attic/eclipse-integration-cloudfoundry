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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel.TunnelDisplayPart;
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
		super(Messages.CaldecottTunnelWizardPage_TEXT_ACTIVE_TUNN_PAGE);
		this.cloudServer = cloudServer;
		setTitle(Messages.CaldecottTunnelWizardPage_TEXT_ACTIVE_TUNN_PAGE);
		setDescription(Messages.CaldecottTunnelWizardPage_TEXT_MANAGE_TUNN_DESCRIP);
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
					Action caldecottAction = new DeleteTunnelAction(Messages.CaldecottTunnelWizardPage_TEXT_DISCONN_ACTION, CloudFoundryImages.DISCONNECT);
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
			return Messages.CaldecottTunnelWizardPage_TEXT_REMOVE_TOOLTIP;
		}
	};

}
