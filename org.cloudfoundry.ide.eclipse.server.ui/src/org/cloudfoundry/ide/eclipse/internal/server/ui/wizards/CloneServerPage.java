/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValidationEvents;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.EventSource;
import org.cloudfoundry.ide.eclipse.internal.server.ui.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ValidationEventHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;

/**
 * Wizard page to allow users to select a target cloud space when cloning an
 * existing server.
 */
class CloneServerPage extends CloudFoundryCloudSpaceWizardpage {

	private String serverName;

	private Text serverNameText;

	private CloudSpace selectedSpace;

	private ValidationEventHandler serverEventHandler = new ValidationEventHandler();

	private Set<String> existingServerNames = null;

	CloneServerPage(CloudFoundryServer server) {
		super(server, null);

		cloudServerSpaceDelegate = new CloudSpacesDelegate(cloudServer) {

			@Override
			public void setSelectedSpace(CloudSpace selectedCloudSpace) {

				CloneServerPage.this.selectedSpace = selectedCloudSpace;
				CloneServerPage.this.setServerNameInUI(getSuggestedServerName(selectedCloudSpace));
			}

			@Override
			protected CloudSpace getCurrentCloudSpace() {
				return selectedSpace;
			}
		};
	}

	protected String getSuggestedServerName(CloudSpace selectedCloudSpace) {

		if (selectedCloudSpace == null) {
			return null;
		}

		String suggestedName = selectedCloudSpace.getName();

		int i = 1;

		while (nameExists(suggestedName)) {
			int openParIndex = suggestedName.indexOf('(');
			int closeParIndex = suggestedName.indexOf(')');
			if (openParIndex > 0 && closeParIndex > openParIndex) {
				suggestedName = suggestedName.substring(0, openParIndex).trim();
			}
			suggestedName += " (" + i++ + ")";
		}
		return suggestedName;
	}

	/**
	 * 
	 * @param name to check if it is already used by an existing Cloud Foundry
	 * server instance.
	 * @return true if the name exists. False otherwise
	 */
	protected boolean nameExists(String name) {

		if (name == null) {
			return false;
		}
		// Servers should not change during the same clone wizard session, so
		// cache the names for rapid checks should be fine
		if (existingServerNames == null) {
			existingServerNames = new HashSet<String>();

			List<CloudFoundryServer> cloudServers = CloudServerUtil.getCloudServers();

			if (cloudServers != null) {
				for (CloudFoundryServer server : cloudServers) {
					existingServerNames.add(server.getServer().getName());
				}
			}
		}

		return existingServerNames.contains(name);

	}

	public String getServerName() {
		return serverName;
	}

	@Override
	public boolean isPageComplete() {
		boolean isComplete = super.isPageComplete();
		if (isComplete) {
			isComplete = serverEventHandler.isOK();
		}

		return isComplete;
	}

	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(mainComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mainComposite);

		Composite serverNameComp = new Composite(mainComposite, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(serverNameComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(serverNameComp);
		Label label = new Label(serverNameComp, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(label);
		label.setText("Server Name:");

		serverNameText = new Text(serverNameComp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(serverNameText);

		serverNameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent arg0) {

				serverName = serverNameText.getText();
				refresh();

			}
		});

		WizardStatusHandler listener = new WizardPageStatusHandler(this);

		// Note that the validator will validate against the EXISTING server
		// that is being cloned (e.g. will
		// perform space validation checks, etc). The cloned server will not be
		// created until the user performs finish
		serverEventHandler.updateValidator(new CloneServerWizardValidator(cloudServer, cloudServerSpaceDelegate));

		serverEventHandler.addStatusHandler(listener);

		spacesPart = new CloudSpacesSelectionPart(cloudServerSpaceDelegate, cloudServer, this) {
			@Override
			public void setInitialSelectionInViewer() {
				if (cloudServerSpaceDelegate == null) {
					return;
				}
				CloudSpace selectedSpace = cloudSpaceServerDelegate.getSpaceWithNoServerInstance();

				// First set the default cloud space as the selected
				// space
				setSpaceSelection(selectedSpace);
				if (selectedSpace != null) {
					setSelectionInViewer(selectedSpace);
				}
			}
		};
		spacesPart.addPartChangeListener(serverEventHandler);
		spacesPart.createPart(mainComposite);

		// Make sure the description is set after the part is created, to
		// avoid using the default parent description.
		setDescription(Messages.CLONE_SERVER_WIZARD_OK_MESSAGE);

		setControl(mainComposite);
	}

	public CloudSpace getSelectedCloudSpace() {
		return selectedSpace;
	}

	protected void refresh() {

		IStatus status = Status.OK_STATUS;
		if (serverName == null || serverName.trim().length() == 0) {
			status = CloudFoundryPlugin.getErrorStatus(Messages.ERROR_VALID_SERVER_NAME);
		}
		else if (nameExists(serverName)) {
			status = CloudFoundryPlugin.getErrorStatus(Messages.ERROR_SERVER_NAME_ALREADY_EXISTS);
		}

		serverEventHandler.handleChange(new PartChangeEvent(null, status, new EventSource<CloneServerPage>(this),
				ValidationEvents.VALIDATION));
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			// Launch it as a job, to give the wizard time to display
			// the spaces viewer
			UIJob job = new UIJob("Refreshing list of organization and spaces") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					updateSpacesDescriptor();
					refreshListOfSpaces();

					return Status.OK_STATUS;
				}

			};
			job.setSystem(true);
			job.schedule();
		}
	}

	protected void setServerNameInUI(String name) {
		if (serverNameText != null && name != null) {
			serverName = name;
			serverNameText.setText(name);
		}
	}

	protected void updateSpacesDescriptor() {
		serverEventHandler.validate(getWizard().getContainer());
	}
}