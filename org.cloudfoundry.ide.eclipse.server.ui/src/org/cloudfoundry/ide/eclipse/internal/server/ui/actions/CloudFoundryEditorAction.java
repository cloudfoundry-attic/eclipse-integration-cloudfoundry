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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ModifyOperation;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ApplicationMasterDetailsBlock;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryCredentialsWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;

/**
 * Abstract class implementing an app cloud action. Before the job starts, the
 * app cloud application editor page is set to show busy state, and after the
 * job finishes, the app cloud application editor is refreshed and set to normal
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public abstract class CloudFoundryEditorAction extends Action {

	private final CloudFoundryApplicationsEditorPage editorPage;

	private final ApplicationMasterDetailsBlock masterDetailsBlock;

	private boolean userAction;

	private final RefreshArea area;

	public enum RefreshArea {
		MASTER, DETAIL, ALL
	}

	public CloudFoundryEditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		this.editorPage = editorPage;
		this.area = area;
		this.masterDetailsBlock = editorPage.getMasterDetailsBlock();
		this.userAction = true;
	}

	public CloudFoundryApplicationsEditorPage getEditorPage() {
		return editorPage;
	}

	/**
	 * 
	 * @return area of the editor to be refreshed after the operation is
	 * complete.
	 */
	protected RefreshArea getArea() {
		return area;
	}

	public abstract String getJobName();

	/**
	 * Operation to execute. May be null.
	 * @return operation to execute.
	 * @throws CoreException
	 */
	protected abstract ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException;

	public boolean isUserAction() {
		return userAction;
	}

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
		Job job = new Job(getJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				try {
					ICloudFoundryOperation operation = getOperation(monitor);
					if (operation == null) {
						return CloudFoundryPlugin.getStatus("No editor operation to execute.", IStatus.WARNING);
					}
					operation.run(monitor);
				}
				catch (CoreException e) {
					CloudFoundryException cfe = e.getCause() instanceof CloudFoundryException ? (CloudFoundryException) e
							.getCause() : null;
					if (cfe instanceof NotFinishedStagingException) {
						status = new Status(IStatus.WARNING, CloudFoundryServerUiPlugin.PLUGIN_ID,
								"Please restart your application for any changes to take effect");

					}
					else if (shouldLogException(e)) {
						status = new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, e.getMessage(), e);
					}
					else {
						status = new Status(Status.CANCEL, CloudFoundryServerUiPlugin.PLUGIN_ID, e.getMessage(), e);
					}
				}
				return status;
			}
		};

		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				Display.getDefault().asyncExec(new Runnable() {

					public void run() {
						if (editorPage.isDisposed())
							return;
						if (masterDetailsBlock.getMasterPart().getManagedForm().getForm().isDisposed())
							return;

						masterDetailsBlock.refreshUI(area);

						IStatus status = event.getResult();
						Throwable exception = status.getException();
						if (!userAction) {
							return;
						}
						if (status.getSeverity() == IStatus.WARNING || status.getSeverity() == IStatus.INFO) {
							setMessageInPage(status);
							return;
						}
						else if (exception != null) {

							if (exception instanceof CoreException) {
								CoreException coreException = (CoreException) exception;

								if (CloudErrorUtil.isNotFoundException(coreException)) {
									display404Error(status);
									return;
								}
								else if (CloudErrorUtil.isWrongCredentialsException(coreException)) {
									CloudFoundryCredentialsWizard wizard = new CloudFoundryCredentialsWizard(editorPage
											.getCloudServer());
									WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(),
											wizard);
									if (dialog.open() == Dialog.OK) {
										CloudFoundryEditorAction.this.run();
										return;
									}
								}
							}

							StatusManager.getManager().handle(status, StatusManager.LOG);
							setErrorInPage(status);
						}
						else {
							IModule currentModule = editorPage.getMasterDetailsBlock().getCurrentModule();
							if (currentModule != null) {
								CloudFoundryApplicationModule appModule = editorPage.getCloudServer()
										.getExistingCloudModule(currentModule);
								if (appModule != null && appModule.getErrorMessage() != null) {
									setErrorInPage(appModule.getErrorMessage());
									return;
								}
							}
							setErrorInPage((String) null);
						}
					}
				});
			}
		});
		return job;
	}

	/**
	 * Default behaviour is to display the error from the status. Subclasses can
	 * override to show other messages.
	 */
	protected void display404Error(IStatus status) {
		setErrorInPage(status);
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

	public void setUserAction(boolean userAction) {
		this.userAction = userAction;
	}

	protected CloudFoundryServerBehaviour getBehavior() {
		return getEditorPage().getCloudServer().getBehaviour();
	}

	protected IModule getModule() {
		return editorPage.getMasterDetailsBlock().getCurrentModule();
	}

	/**
	 * Operation to modify server values from the editor. Should be used for operations
	 * th
	 * NOTE:Editor operations should only be created AFTER the editor page is
	 * accessible.
	 * 
	 */
	protected abstract class ModifyEditorOperation extends ModifyOperation {

		public ModifyEditorOperation() {
			super(editorPage.getCloudServer().getBehaviour());
		}

		@Override
		protected void refresh(IProgressMonitor monitor) throws CoreException {
			
			// Many operations affect app instances therefore updated the
			// instances after the operation
			editorPage.getCloudServer().getBehaviour().updateApplicationInstanceStats(getModule(), monitor);
			
			super.refresh(monitor);
		}
	}

}
