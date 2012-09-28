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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudSpacesDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryCredentialsPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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

	private CloudSpaceChangeListener cloudSpaceChangeListener;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle wizard) {
		initServer();

		if (cfServer != null) {
			cloudSpaceChangeListener = new WizardFragmentSpaceChangeListener(cfServer);
		}

		credentialsPart = new CloudFoundryCredentialsPart(cfServer, wizard, cloudSpaceChangeListener);
		return credentialsPart.createComposite(parent);
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
		if (credentialsPart == null) {
			return false;
		}
		return credentialsPart.isComplete();
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

				if (cf.supportsCloudSpaces() && originalServer.supportsCloudSpaces()) {
					CloudFoundrySpace originalSpace = originalServer.getCloudFoundrySpace();
					CloudFoundrySpace space = cf.getCloudFoundrySpace();
					connect = space.getOrgName().equals(originalSpace.getOrgName())
							&& space.getSpaceName().equals(originalSpace.getSpaceName());
				}
				else if (!cf.supportsCloudSpaces() && !originalServer.supportsCloudSpaces()) {
					connect = true;
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

	protected class WizardFragmentSpaceChangeListener extends CloudSpaceChangeListener {

		public WizardFragmentSpaceChangeListener(CloudFoundryServer cloudServer) {
			super(cloudServer);
		}

		@Override
		protected void handleCloudSpaceSelection(CloudSpacesDescriptor spacesDescriptor) {
			if (spacesDescriptor != null && spacesDescriptor.supportsSpaces()) {
				initServer();
				spacesFragment = new CloudFoundrySpacesWizardFragment(cloudSpaceChangeListener, cfServer);
			}
			else {
				spacesFragment = null;
			}
		}

	}

}
