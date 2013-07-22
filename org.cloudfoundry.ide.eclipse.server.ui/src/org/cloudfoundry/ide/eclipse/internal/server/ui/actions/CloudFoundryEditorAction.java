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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
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

	protected Job getJob() {
		Job job = new Job(getJobName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = null;
				try {
					IModule module = editorPage.getMasterDetailsBlock().getCurrentModule();
					status = performAction(monitor);
					if (status != null && status.isOK()) {
						// Since Caldecott related operations affect multiple
						// areas of the editor
						// refresh the entire editor when an operation is
						// related to Caldecott
						if (module != null && TunnelBehaviour.isCaldecottApp(module.getName())) {
							return editorPage.refreshStates(module, RefreshArea.ALL, monitor);
						}
						else {
							return editorPage.refreshStates(module, area, monitor);
						}
					}
				}
				catch (CoreException e) {
					IStatus errorStatus = null;
					if (shouldLogException(e)) {
						errorStatus = new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID,
								e.getMessage(), e);
						StatusManager.getManager().handle(
								errorStatus, StatusManager.LOG);
					} else {
						errorStatus = new Status(Status.CANCEL, CloudFoundryServerUiPlugin.PLUGIN_ID, e.getMessage(), e);
					}
					return errorStatus;
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
						if (exception != null) {
							if (exception instanceof CoreException) {
								if (CloudErrorUtil.isNotFoundException((CoreException) exception)) {
									display404Error(status);
									return;
								}
								if (userAction && CloudErrorUtil.isWrongCredentialsException((CoreException) exception)) {
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
							setErrorInPage(status);
						}
						else {
							IModule currentModule = editorPage.getMasterDetailsBlock().getCurrentModule();
							if (currentModule != null) {
								CloudFoundryApplicationModule appModule = editorPage.getCloudServer().getApplication(currentModule);
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

	public void setUserAction(boolean userAction) {
		this.userAction = userAction;
	}

	protected CloudFoundryServerBehaviour getBehavior() {
		return getEditorPage().getCloudServer().getBehaviour();
	}
}
