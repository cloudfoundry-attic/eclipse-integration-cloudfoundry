/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.AbstractPublishApplicationOperation;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryCredentialsWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.wst.server.core.IModule;

/**
 * Abstract class implementing an app cloud action.
 * 
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public abstract class EditorAction extends Action {

	protected final CloudFoundryApplicationsEditorPage editorPage;

	private final RefreshArea area;

	public enum RefreshArea {
		MASTER, DETAIL, ALL
	}

	public EditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		this(editorPage, area, null, null);
	}

	public EditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area, String actionName,
			ImageDescriptor descriptor) {
		this.editorPage = editorPage;
		this.area = area;
		if (actionName != null) {
			setText(actionName);
		}
		if (descriptor != null) {
			setImageDescriptor(descriptor);
		}
	}

	public CloudFoundryApplicationsEditorPage getEditorPage() {
		return editorPage;
	}

	protected IModule getModule() {
		return editorPage.getMasterDetailsBlock().getCurrentModule();
	}

	public String getJobName() {
		return Messages.EditorAction_CLOUD_OPERATION;
	}

	protected abstract ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException;

	protected boolean shouldLogException(CoreException e) {
		return true;
	}

	@Override
	public void run() {
		Job job = getJob();
		runJob(job);
	}

	protected void runJob(Job job) {
		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) editorPage.getEditorSite().getService(
				IWorkbenchSiteProgressService.class);
		if (service != null) {
			service.schedule(job, 0L, true);
		}
		else {
			job.schedule();
		}
	}

	protected Job getJob() {
		final Job job = new Job(getJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					ICloudFoundryOperation operation = getOperation(monitor);

					if (operation == null) {
						throw CloudErrorUtil.toCoreException(Messages.CloudFoundryEditorAction_TEXT_NO_OP_EXECUTE);
					}
					if (operation instanceof AbstractPublishApplicationOperation) {
						String name = ((AbstractPublishApplicationOperation) operation).getOperationName();
						setName(name);
					}
					operation.run(monitor);
				}
				catch (CoreException ce) {
					if (CloudErrorUtil.isWrongCredentialsException(ce)) {
						Display.getDefault().syncExec(new Runnable() {

							@Override
							public void run() {
								CloudFoundryCredentialsWizard wizard = new CloudFoundryCredentialsWizard(editorPage
										.getCloudServer());
								WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
								if (dialog.open() == Dialog.OK) {
									schedule();
								}
							}
						});
					}
					else {
						// Process the error and fire error event
						IStatus status = Status.OK_STATUS;
						CloudFoundryException cfe = ce.getCause() instanceof CloudFoundryException ? (CloudFoundryException) ce
								.getCause() : null;
						if (cfe instanceof NotFinishedStagingException) {
							status = new Status(IStatus.WARNING, CloudFoundryServerUiPlugin.PLUGIN_ID,
									Messages.CloudFoundryEditorAction_WARNING_RESTART_APP);

						}
						else if (CloudErrorUtil.isNotFoundException(ce)) {
							status = display404Error(status);
						}
						else if (shouldLogException(ce)) {
							status = new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, ce.getMessage(), ce);
						}
						else {
							status = new Status(Status.CANCEL, CloudFoundryServerUiPlugin.PLUGIN_ID, ce.getMessage(),
									ce);
						}
						// fire Event only on error, as ICloudFoundryOperation
						// usually fires their own events when completed.
						// FIXNS: eventually this should be moved into the
						// ICloudFoundryOperation itself, such that errors can
						// be propagated
						// by non-editor operations
						ServerEventHandler.getDefault().fireServerEvent(
								new EditorCloudEvent(getEditorPage().getCloudServer(),
										CloudServerEvent.EVENT_CLOUD_OP_ERROR, status, area));
					}
				}
				return Status.OK_STATUS;
			}
		};

		return job;
	}

	/**
	 * Get Status for the 404 error. By default returns the same status
	 */
	protected IStatus display404Error(IStatus status) {
		return status;
	}

	protected void setErrorInPage(IStatus status) {
		setErrorInPage(status.getMessage());
	}

	protected void setErrorInPage(String message) {
		if (message == null) {
			editorPage.setMessage(null, IMessageProvider.NONE);
		}
		else {
			editorPage.setMessage(message, IMessageProvider.ERROR);
		}
	}

	protected void setMessageInPage(IStatus status) {
		String message = status.getMessage();
		int providerStatus = IMessageProvider.NONE;
		switch (status.getSeverity()) {
		case IStatus.INFO:
			providerStatus = IMessageProvider.INFORMATION;
			break;
		case IStatus.WARNING:
			providerStatus = IMessageProvider.WARNING;
			break;
		}

		editorPage.setMessage(message, providerStatus);
	}

	protected CloudFoundryServerBehaviour getBehaviour() {
		return getEditorPage().getCloudServer().getBehaviour();
	}

	public static class EditorCloudEvent extends CloudServerEvent {

		private static final long serialVersionUID = 1L;

		private final RefreshArea refreshArea;

		public EditorCloudEvent(CloudFoundryServer server, int type, IStatus status, RefreshArea refreshArea) {
			super(server, type, status);
			this.refreshArea = refreshArea;
		}

		public RefreshArea getRefreshArea() {
			return refreshArea;
		}
	}
}
