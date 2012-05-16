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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class CaldecottTunnelWizardPage extends WizardPage {

	private final CloudFoundryServer cloudServer;

	private final Set<CaldecottTunnelDescriptor> removeDescriptors = new HashSet<CaldecottTunnelDescriptor>();

	private TunnelDisplayPart part;

	public CaldecottTunnelWizardPage(CloudFoundryServer cloudServer) {
		super("Caldecott Service Tunnels");
		this.cloudServer = cloudServer;
		setTitle("Caldecott Service Tunnels");
		setDescription("Manage Caldecott Tunnels");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {
		part = new TunnelDisplayPart(getShell(), cloudServer, null);
		Control area = part.createControl(parent);

		addTableActions();

		setControl(area);
	}

	protected void addTableActions() {
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				List<CaldecottTunnelDescriptor> descriptors = part.getSelectedCaldecotTunnelDescriptors();

				if (!descriptors.isEmpty()) {
					Action caldecottAction = new DeleteTunnelAction("Delete Connection", CloudFoundryImages.REMOVE);
					manager.add(caldecottAction);
					if (descriptors.size() == 1) {
						manager.add(new CopyPassword());
						manager.add(new CopyUserName());
					}
				}
			}
		});

		Menu menu = menuManager.createContextMenu(part.getViewer().getControl());
		part.getViewer().getControl().setMenu(menu);

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

	protected abstract class CopyTunnelInformation extends Action {

		public CopyTunnelInformation(String actionName, ImageDescriptor actionImage) {
			super(actionName, actionImage);
		}

		public void run() {
			Clipboard clipBoard = new Clipboard(getShell().getDisplay());
			CaldecottTunnelDescriptor descriptor = getSelectedTunnelDescriptor();
			if (descriptor != null) {
				String value = getTunnelInformation(descriptor);
				clipBoard.setContents(new Object[] { value }, new TextTransfer[] { TextTransfer.getInstance() });
			}
		}

		protected CaldecottTunnelDescriptor getSelectedTunnelDescriptor() {

			List<CaldecottTunnelDescriptor> descriptors = part.getSelectedCaldecotTunnelDescriptors();

			return !descriptors.isEmpty() ? descriptors.get(0) : null;
		}

		abstract public String getToolTipText();

		abstract String getTunnelInformation(CaldecottTunnelDescriptor descriptor);

	}

	protected class CopyUserName extends CopyTunnelInformation {

		public CopyUserName() {
			super("Copy username", CloudFoundryImages.EDIT);
		}

		@Override
		public String getToolTipText() {
			return "Copy username";
		}

		@Override
		String getTunnelInformation(CaldecottTunnelDescriptor descriptor) {
			return descriptor.getUserName();
		}

	}

	protected class CopyPassword extends CopyTunnelInformation {

		public CopyPassword() {
			super("Copy password", CloudFoundryImages.EDIT);
		}

		@Override
		public String getToolTipText() {
			return "Copy password";
		}

		@Override
		String getTunnelInformation(CaldecottTunnelDescriptor descriptor) {
			return descriptor.getPassword();
		}

	}

}
