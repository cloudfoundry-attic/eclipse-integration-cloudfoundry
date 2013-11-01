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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryCredentialsPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceHandler;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServerWizardValidator;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServerWizardValidator.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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

	private ServerWizardValidator validator;

	private WizardFragmentChangeListener wizardListener;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle wizardHandle) {
		initServer();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		validator = new ServerWizardValidator(cloudServer, new CloudSpaceHandler(cloudServer));

		spacesFragment = new CloudFoundrySpacesWizardFragment(cloudServer, validator);
		wizardListener = new WizardFragmentChangeListener(wizardHandle);
		credentialsPart = new CloudFoundryCredentialsPart(cloudServer, validator, wizardListener, wizardHandle);

		credentialsPart.createPart(composite);

		return composite;
	}

	@Override
	public void enter() {
		initServer();
		credentialsPart.setServer(cloudServer);

		// Validate currently credentials but not against the server
		if (validator != null && wizardListener != null) {
			// Display an errors from the last validation on entering
			ValidationStatus lastStatus = validator.getPreviousValidationStatus();

			if (lastStatus == null || lastStatus.getStatus().getSeverity() != IStatus.ERROR) {
				// Otherwise validate locally
				lastStatus = validator.validate(false);
			}
			wizardListener.handleChange(lastStatus.getStatus());
		}
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public boolean isComplete() {
		// Enable the Next and Finish buttons, even if credentials are not
		// validated.
		return validator.areCredentialsFilled();
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
		// Check the current credentials without server validation first, as if
		// they are
		// valid, there is no need to send a server request.
		if (!validator.validate(false).getStatus().isOK()) {
			final IStatus[] validationStatus = { Status.OK_STATUS };
			// Must run in UI thread since errors result in a dialogue opening.
			Display.getDefault().syncExec(new Runnable() {

				public void run() {
					ValidationStatus status = validator.validate(true);
					validationStatus[0] = status.getStatus();
				}
			});
			if (!validationStatus[0].isOK()) {
				throw new CoreException(validationStatus[0]);
			}
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
