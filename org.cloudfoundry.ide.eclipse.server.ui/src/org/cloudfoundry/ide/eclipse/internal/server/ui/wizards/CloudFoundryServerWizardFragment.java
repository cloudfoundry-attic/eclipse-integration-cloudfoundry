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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryCredentialsPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudServerSpacesDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.ui.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerWizardValidator;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ValidationEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
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

	private CloudFoundryServer cloudServer;

	private CloudFoundryCredentialsPart credentialsPart;

	private CloudFoundrySpacesWizardFragment spacesFragment;

	private ValidationEventHandler validationHandler;

	private WizardFragmentStatusHandler statusHandler;

	private IRunnableContext context;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle wizardHandle) {
		initServer();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		context = wizardHandle != null ? new WizardHandleContext(wizardHandle).getRunnableContext() : null;

		// Performs validation on credentials and cloud spaces when creating a
		// new server.
		validationHandler = new ValidationEventHandler();

		// Status handler that displays status to the wizard
		statusHandler = new WizardFragmentStatusHandler(wizardHandle);

		credentialsPart = new CloudFoundryCredentialsPart(cloudServer, new WizardHandleContext(wizardHandle));

		// Changes in the credentials part (e.g. username, password, server URL)
		// should
		// notify the validation handler so that it can perform validations
		credentialsPart.addPartChangeListener(validationHandler);

		// The credentials part should be notified when validations are
		// complete, as to enable/disable UI components accordingly
		validationHandler.addValidationListener(credentialsPart);

		// Must wire the wizard status handler to the validation handler, so
		// that the wizard
		// can be notified when to display validation status or errors.
		validationHandler.addStatusHandler(statusHandler);

		credentialsPart.createPart(composite);

		return composite;
	}

	@Override
	public void enter() {
		initServer();

		// New validator should be created as the cloud server may have changed.

		ServerWizardValidator validator = new CredentialsWizardValidator(cloudServer, new CloudServerSpacesDelegate(
				cloudServer));
		if (validationHandler != null) {
			validationHandler.updateValidator(validator);
		}

		spacesFragment = new CloudFoundrySpacesWizardFragment(cloudServer, validator.getSpaceDelegate(),
				validationHandler);

		credentialsPart.setServer(cloudServer);
	}

	public void exit() {
		// Validate credentials when exiting. This covers cases when a user:
		// 1. Clicks 'Next'
		// 2. Clicks 'Finish'
		if (validationHandler != null) {
			validationHandler.validate(context);
		}
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public boolean isComplete() {
		return validationHandler.isOK();
	}

	@Override
	protected void createChildFragments(List<WizardFragment> list) {
		if (spacesFragment != null) {
			list.add(spacesFragment);

		}
		super.createChildFragments(list);
	}

	protected void dispose() {
		spacesFragment = null;
	}

	@Override
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		super.performCancel(monitor);
		dispose();
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		// Check the current credentials without server validation first, as if
		// they are
		// valid, there is no need to send a server request.
		if (!validationHandler.isOK()) {

			IStatus status = validationHandler.getNextNonOKEvent();
			if (status == null) {
				status = CloudFoundryPlugin.getErrorStatus(Messages.ERROR_UNKNOWN_SERVER_CREATION_ERROR);
			}
			throw new CoreException(status);
		}

		ServerLifecycleAdapter listener = new ServerLifecycleAdapter() {
			@Override
			public void serverAdded(IServer server) {
				ServerCore.removeServerLifecycleListener(this);

				Job job = new ConnectJob(cloudServer, server);
				// this is getting called before
				// CloudFoundryServer.saveConfiguration() has flushed the
				// configuration therefore delay job
				job.schedule(500L);
			}
		};
		ServerCore.addServerLifecycleListener(listener);
		dispose();
	}

	private void initServer() {
		ServerWorkingCopy server = (ServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
		cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		if (cloudServer == null) {
			CloudFoundryPlugin
					.logError("Cloud Foundry Server Framework Error: Failed to create a Cloud Foundry server working copy for: "
							+ server.getId() + ". Please check if the plugin has been installed correctly.");
		}
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

}
