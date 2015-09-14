/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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
 *     Steven Hung, IBM - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Logger;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.ServiceToApplicationsBindingPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.ServiceToApplicationsBindingPart.ApplicationToService;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.AddServicesToApplicationAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.ModifyServicesForApplicationAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.RemoveServicesFromApplicationAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.ServicesHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class ServiceToApplicationsBindingWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private ServiceToApplicationsBindingWizardPage bindServiceToApplicationPage;

	private ServicesHandler servicesHandler;

	private final CloudFoundryApplicationsEditorPage editorPage;

	public ServiceToApplicationsBindingWizard(ServicesHandler servicesHandler, CloudFoundryServer server,
			CloudFoundryApplicationsEditorPage editorPage) {
		this.cloudServer = server;
		this.servicesHandler = servicesHandler;
		this.editorPage = editorPage;

		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		bindServiceToApplicationPage = new ServiceToApplicationsBindingWizardPage(servicesHandler, cloudServer,
				editorPage);
		bindServiceToApplicationPage.setWizard(this);
		addPage(bindServiceToApplicationPage);
	}

	@Override
	public boolean performFinish() {
		bindServiceToApplicationPage.performFinish();
		return true;
	}

	static class ServiceToApplicationsBindingWizardPage extends PartsWizardPage {
		private ServiceToApplicationsBindingPart serviceToApplicationsBindingPart;

		private final CloudFoundryServer server;

		ServicesHandler servicesHandler;

		CloudFoundryApplicationsEditorPage editorPage;

		public ServiceToApplicationsBindingWizardPage(ServicesHandler servicesHandler, CloudFoundryServer server,
				CloudFoundryApplicationsEditorPage editorPage) {
			super(Messages.MANAGE_SERVICES_TO_APPLICATIONS_TITLE, Messages.MANAGE_SERVICES_TO_APPLICATIONS_TITLE,
					CloudFoundryImages.getWizardBanner(server.getServer().getServerType().getId()));

			setDescription(NLS.bind(Messages.MANAGE_SERVICES_TO_APPLICATIONS_DESCRIPTION, servicesHandler.toString()));
			this.server = server;
			this.servicesHandler = servicesHandler;
			this.editorPage = editorPage;
		}

		public void performWhenPageVisible() {
			// When the page is visible, populate the page
			runAsynchWithWizardProgress(new ICoreRunnable() {
				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					if (server != null) {
						CloudFoundryServerBehaviour behaviour = server.getBehaviour();
						if (behaviour != null) {
							monitor.beginTask(Messages.MANAGE_SERVICES_TO_APPLICATIONS_GET_APPLICATION_NAMES,
									IProgressMonitor.UNKNOWN);
							final List<CloudApplication> allApps = behaviour.getApplications(monitor);

							monitor.done();
							Display.getDefault().syncExec(new Runnable() {
								public void run() {
									serviceToApplicationsBindingPart.setInput(allApps);
								}
							});
						}
					}
				}
				// The message used below cannot be seen from the UI. To ensure
				// translation is correct, re-use the wizard's title
			}, Messages.MANAGE_SERVICES_TO_APPLICATIONS_TITLE);

		}

		public boolean isPageComplete() {
			// Finish can always be pressed, regardless of what is selected or
			// not selected
			return true;
		}

		protected void performFinish() {
			try {
				CloudUiUtil.runForked(new ICoreRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						monitor.setTaskName(Messages.MANAGE_SERVICES_TO_APPLICATIONS_FINISH);
						CloudFoundryServerBehaviour behaviour = server.getBehaviour();
						if (serviceToApplicationsBindingPart != null && server != null && behaviour != null) {
							List<ApplicationToService> applicationsToProcess = serviceToApplicationsBindingPart
									.getApplicationToService();

							CloudService cloudService = null;

							try {
								// Find the Cloud Service that was selected
								List<CloudService> cloudServiceList = behaviour.getServices(monitor);
								int lenCloudService = cloudServiceList.size();

								String serviceName = servicesHandler.toString();

								for (int j = 0; j < lenCloudService; j++) {
									CloudService currService = cloudServiceList.get(j);
									if (currService != null) {
										String currServiceName = currService.getName();
										if (currServiceName != null && currServiceName.equals(serviceName)) {
											cloudService = currService;
											break;
										}
									}
								}
								StructuredSelection structuredSelection = new StructuredSelection(cloudService);

								int len = applicationsToProcess.size();
								for (int i = 0; i < len; i++) {
									ApplicationToService curr = applicationsToProcess.get(i);

									// Detect if the service was modified for
									// that application
									//
									// Call AddServicesToApplicationAction and
									// RemoveServicesFromApplicationAction,
									// which will deal with the binding and
									// unbinding of the service. In addition
									// these actions refresh the Application and
									// Services editor after the update
									boolean isBoundToServiceAfter = curr.getBoundToServiceAfter();
									if (isBoundToServiceAfter != curr.getBoundToServiceBefore()) {
										CloudApplication cloudApp = applicationsToProcess.get(i).getCloudApplication();
										CloudFoundryApplicationModule module = server.getExistingCloudModule(cloudApp
												.getName());
										if (isBoundToServiceAfter) {
											ModifyServicesForApplicationAction bindService = new AddServicesToApplicationAction(
													structuredSelection, module, server.getBehaviour(), editorPage);
											bindService.run();
										}
										else {
											ModifyServicesForApplicationAction unbindService = new RemoveServicesFromApplicationAction(
													structuredSelection, module, server.getBehaviour(), editorPage);
											unbindService.run();
										}
									}
								}

							}
							catch (CoreException e) {
								if (Logger.ERROR) {
									Logger.println(
											Logger.ERROR_LEVEL,
											this,
											"performFinish", "Error when processing applications to bind or unbind with the service", e); //$NON-NLS-1$ //$NON-NLS-2$
								}

								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										MessageDialog.openError(Display.getDefault().getActiveShell(),
												Messages.MANAGE_SERVICES_TO_APPLICATIONS_FINISH_ERROR_TITLE,
												Messages.MANAGE_SERVICES_TO_APPLICATIONS_FINISH_ERROR_DESCRIPTION);
									}
								});

							}
						}
					}
				}, getWizard().getContainer());
			}
			catch (OperationCanceledException e1) {
				if (Logger.ERROR) {
					Logger.println(
							Logger.ERROR_LEVEL,
							this,
							"performFinish", "Error when processing applications to bind or unbind with the service", e1); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			catch (CoreException e1) {
				if (Logger.ERROR) {
					Logger.println(
							Logger.ERROR_LEVEL,
							this,
							"performFinish", "Error when processing applications to bind or unbind with the service", e1); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		public void createControl(Composite parent) {
			serviceToApplicationsBindingPart = new ServiceToApplicationsBindingPart(servicesHandler);
			serviceToApplicationsBindingPart.addPartChangeListener(this);

			Control control = serviceToApplicationsBindingPart.createPart(parent);
			setControl(control);
		}

		@Override
		public void handleChange(PartChangeEvent event) {
			// Do nothing
		}
	}
}