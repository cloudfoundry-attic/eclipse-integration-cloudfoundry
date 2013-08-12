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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.TunnelBehaviour;
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

	public abstract String getJobName();

	public boolean isUserAction() {
		return userAction;
	}

	protected abstract IStatus performAction(IProgressMonitor monitor) throws CoreException;

	protected boolean shouldLogException(CoreException e) {
		return true;
	}

	@Override
	public void run() {

		Job job = getJob();
		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) editorPage.getEditorSite().getService(
				IWorkbenchSiteProgressService.class);
		if (service != null) {
			service.schedule(job, 0L, true);
		}
		else {
			job.schedule();
		}
	}

	protected IStatus refreshApplication(IModule module, RefreshArea area, IProgressMonitor monitor)
			throws CoreException {
		// Do not refresh instances stats
		return doRefreshApplication(module, area, false, monitor);
	}

	protected IStatus doRefreshApplication(IModule module, RefreshArea area, boolean refreshInstances,
			IProgressMonitor monitor) throws CoreException {
		// Since Caldecott related operations affect multiple
		// areas of the editor
		// refresh the entire editor when an operation is
		// related to Caldecott
		if (module != null && TunnelBehaviour.isCaldecottApp(module.getName())) {
			return editorPage.refreshModules(module, RefreshArea.ALL, refreshInstances, monitor);
		}
		else {
			return editorPage.refreshModules(module, area, refreshInstances, monitor);
		}
	}

	protected Job getJob() {
		Job job = new Job(getJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = null;
				try {
					IModule module = editorPage.getMasterDetailsBlock().getCurrentModule();
					status = performAction(monitor);
					if (status != null && status.isOK()) {
						return refreshApplication(module, area, monitor);
					}
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
								CloudFoundryApplicationModule appModule = editorPage.getCloudServer().getApplication(
										currentModule);
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
}
