/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryCredentialsPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceChangeHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.server.core.util.ServerLifecycleAdapter;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
@SuppressWarnings("restriction")
public class CloudFoundryServerWizardFragment extends WizardFragment {

	private CloudFoundryServer cfServer;

	private CloudFoundryCredentialsPart credentialsPart;

	private CloudFoundrySpacesWizardFragment spacesFragment;

	private CloudSpaceChangeHandler spaceChangeHandler;

	private CredentialsWizardUpdateHandler wizardUpdateHandler;

	@Override
	public void exit() {
		if (!wizardUpdateHandler.isValid() && wizardUpdateHandler.credentialsFilled()) {
			credentialsPart.validate();
		}

		super.exit();
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle wizardHandle) {
		initServer();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (cfServer != null) {
			spaceChangeHandler = new WizardFragmentSpaceChangeHandler(cfServer, wizardHandle);
		}

		wizardUpdateHandler = new CredentialsWizardUpdateHandler(wizardHandle);
		credentialsPart = new CloudFoundryCredentialsPart(cfServer, spaceChangeHandler, wizardUpdateHandler,
				wizardHandle);

		credentialsPart.createPart(composite);
		return composite;
	}

	@Override
	public void enter() {
		initServer();
		credentialsPart.setServer(cfServer);
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public boolean isComplete() {
		// Enable the Next and Finish buttons, even if credentials are not
		// validated.
		return wizardUpdateHandler != null && wizardUpdateHandler.credentialsFilled();
	}

	@Override
	protected void createChildFragments(List<WizardFragment> list) {
		if (spacesFragment != null) {
			list.add(spacesFragment);
		}
		super.createChildFragments(list);
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		if (!wizardUpdateHandler.isValid() && wizardUpdateHandler.credentialsFilled()) {
			credentialsPart.validate();
		}

		if (!wizardUpdateHandler.isValid()) {
			IStatus status = wizardUpdateHandler.getLastValidationStatus() != null ? wizardUpdateHandler
					.getLastValidationStatus()
					: CloudFoundryPlugin
							.getErrorStatus("Invalid credentials. Please enter a correct username and password, and validate your credentials.");
			throw new CoreException(status);
		}

		ServerLifecycleAdapter listener = new ServerLifecycleAdapter() {
			@Override
			public void serverAdded(IServer server) {
				ServerCore.removeServerLifecycleListener(this);

				Job job = new ConnectJob(cfServer, server);
				// this is getting called before
				// CloudFoundryServer.saveConfiguration() has flushed the
				// configuration therefore delay job
				job.schedule(500L);
			}
		};
		ServerCore.addServerLifecycleListener(listener);
	}

	private void initServer() {
		ServerWorkingCopy server = (ServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
		cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
	}

	private static class ConnectJob extends Job {

		private final CloudFoundryServer originalServer;

		private final IServer server;

		public ConnectJob(CloudFoundryServer originalServer, IServer server) {
			super("Connect account");
			this.originalServer = originalServer;
			this.server = server;
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			CloudFoundryServer cf = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, monitor);
			if (cf.getPassword() == null) {
				// configuration has not been saved, yet, ignore
				return Status.CANCEL_STATUS;
			}

			if (cf != null && cf.getUsername().equals(originalServer.getUsername())
					&& cf.getPassword().equals(originalServer.getPassword())
					&& cf.getUrl().equals(originalServer.getUrl())) {

				boolean connect = false;

				if (cf.hasCloudSpace() && originalServer.hasCloudSpace()) {
					CloudFoundrySpace originalSpace = originalServer.getCloudFoundrySpace();
					CloudFoundrySpace space = cf.getCloudFoundrySpace();
					connect = space.getOrgName().equals(originalSpace.getOrgName())
							&& space.getSpaceName().equals(originalSpace.getSpaceName());
				}

				if (connect) {
					CloudFoundryServerBehaviour behaviour = cf.getBehaviour();
					if (behaviour != null) {
						try {
							behaviour.connect(monitor);
						}
						catch (CoreException e) {
							CloudFoundryServerUiPlugin.getDefault().getLog().log(e.getStatus());
						}
					}
				}

			}
			return Status.OK_STATUS;
		}
	}

	/**
	 * Add or removes the orgs and spaces wizard page based on whether a cloud
	 * space descriptor, which contains a list of orgs and spaces, is changed or
	 * not. If the changed descriptor is null, the spaces wizard page is
	 * removed. Otherwise, it is added. Changes in user credentials, and
	 * validation of the credentials typically trigger spaces descriptor
	 * changes, which affects whether the spaces page is shown or not in the New
	 * CF Server wizard.
	 * 
	 */
	protected class WizardFragmentSpaceChangeHandler extends CloudSpaceChangeHandler {

		private final IWizardHandle wizardHandle;

		public WizardFragmentSpaceChangeHandler(CloudFoundryServer cloudServer, IWizardHandle wizardHandle) {
			super(cloudServer);
			this.wizardHandle = wizardHandle;
		}

		@Override
		protected void handleCloudSpaceDescriptorSelection(CloudSpacesDescriptor spacesDescriptor) {
			initServer();
			// Add the spaces page if passed a valid descriptor
			// Clear the org and spaces page if there the descriptor is null
			// (i.e. there is no list of orgs and spaces), to avoid showing a
			// blank spaces page
			if (spacesDescriptor == null) {
				spacesFragment = null;
			}
			else {
				spacesFragment = new CloudFoundrySpacesWizardFragment(spaceChangeHandler, cfServer);
			}
		}

		protected void handleCloudSpaceSelection(CloudSpace cloudSpace) {
			if (wizardHandle != null) {
				wizardHandle.update();
			}
		}

	}

}
