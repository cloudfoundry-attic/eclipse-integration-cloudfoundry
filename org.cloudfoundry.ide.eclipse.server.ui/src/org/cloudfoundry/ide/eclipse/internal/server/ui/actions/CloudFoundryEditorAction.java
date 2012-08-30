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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
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

	public abstract IStatus performAction(IProgressMonitor monitor) throws CoreException;

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
						if (module != null && CaldecottTunnelHandler.isCaldecottApp(module.getName())) {
							return editorPage.refreshStates(module, RefreshArea.ALL, monitor);
						}
						else {
							return editorPage.refreshStates(module, area, monitor);
						}
					}
				}
				catch (CoreException e) {
					if (shouldLogException(e)) {
						StatusManager.getManager().handle(
								new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID,
										"Failed to perform server editor action", e), StatusManager.LOG);
					}
					return new Status(Status.CANCEL, CloudFoundryServerUiPlugin.PLUGIN_ID, e.getMessage(), e);
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
								if (CloudUtil.isNotFoundException((CoreException) exception)) {
									display404Error(status);
									return;
								}
								if (userAction && CloudUtil.isWrongCredentialsException((CoreException) exception)) {
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
							editorPage.setMessage(status.getMessage(), IMessageProvider.ERROR);
						}
						else {
							IModule currentModule = editorPage.getMasterDetailsBlock().getCurrentModule();
							if (currentModule != null) {
								ApplicationModule appModule = editorPage.getCloudServer().getApplication(currentModule);
								if (appModule != null && appModule.getErrorMessage() != null) {
									editorPage.setMessage(appModule.getErrorMessage(), IMessageProvider.ERROR);
									return;
								}
							}
							editorPage.setMessage(null, IMessageProvider.NONE);
						}
					}
				});
			}
		});
		return job;
	}

	protected void display404Error(IStatus status) {
		editorPage.setMessage(status.getMessage(), IMessageProvider.ERROR);
	}

	public void setUserAction(boolean userAction) {
		this.userAction = userAction;
	}

	protected CloudFoundryServerBehaviour getBehavior() {
		return getEditorPage().getCloudServer().getBehaviour();
	}
}
