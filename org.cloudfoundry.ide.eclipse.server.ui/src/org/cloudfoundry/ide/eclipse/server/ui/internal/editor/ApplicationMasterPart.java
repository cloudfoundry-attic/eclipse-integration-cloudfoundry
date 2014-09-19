/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.DeleteServicesAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.RefreshApplicationEditorAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.ReplaceCloudApplicationAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryServiceWizard;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudRoutesWizard;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.DragAndDropProjectProcessChoiceWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.ui.internal.ImageResource;
import org.eclipse.wst.server.ui.internal.ServerLabelProvider;
import org.eclipse.wst.server.ui.internal.view.servers.RemoveModuleAction;
import org.eclipse.wst.server.ui.internal.view.servers.ServersViewDropAdapter;
import org.eclipse.wst.server.ui.internal.wizard.ModifyModulesWizard;

/**
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
@SuppressWarnings("restriction")
public class ApplicationMasterPart extends SectionPart {

	private TableViewer applicationsViewer;

	private final CloudFoundryServer cloudServer;

	private IModule currentModule;

	private final CloudFoundryApplicationsEditorPage editorPage;

	private TableViewer servicesViewer;

	private FormToolkit toolkit;

	private boolean provideServices;

	private Section servicesSection;

	public ApplicationMasterPart(CloudFoundryApplicationsEditorPage editorPage, IManagedForm managedForm,
			Composite parent, CloudFoundryServer cloudServer) {
		super(parent, managedForm.getToolkit(), Section.TITLE_BAR | Section.DESCRIPTION);
		this.editorPage = editorPage;
		this.cloudServer = cloudServer;
		this.toolkit = managedForm.getToolkit();
		this.provideServices = CloudFoundryBrandingExtensionPoint.getProvideServices(editorPage.getServer()
				.getServerType().getId());
	}

	/**
	 * Creates the content of the master part inside the form part. This method
	 * is called when the master/details block is created.
	 */
	public void createContents() {
		createApplicationsSection();

		if (provideServices) {
			createServicesSection();
		}

		createRoutesDomainsSection();
	}

	public TableViewer getApplicationsViewer() {
		return applicationsViewer;
	}

	public IModule getCurrentModule() {
		return currentModule;
	}

	private void updateSections() {
		if (provideServices) {
			List<CloudService> services = editorPage.getServices();
			servicesViewer.setInput((services != null) ? services.toArray() : null);
			if (servicesSection != null && !servicesSection.isExpanded()) {
				// If collapsed, expand (e.g. section is collapsed and user adds
				// a new service)
				servicesSection.setExpanded(true);
			}
		}
	}

	public void refreshUI() {

		IStatus status = cloudServer.refreshCloudModules();

		applicationsViewer.setInput(cloudServer.getServerOriginal().getModules());
		// Update the sections regardless of any errors in the modules, as some
		// modules may have no errors
		updateSections();

		if (editorPage != null && !editorPage.isDisposed()) {
			if (!status.isOK()) {
				editorPage.setErrorMessage(status.getMessage());
			}
			else {
				editorPage.setErrorMessage(null);
			}
		}

	}

	private class ApplicationViewersDropAdapter extends ServersViewDropAdapter {

		public ApplicationViewersDropAdapter(Viewer viewer) {
			super(viewer);
		}

		@Override
		protected Object getCurrentTarget() {
			return editorPage.getServer().getOriginal();
		}

		@Override
		protected Object determineTarget(DropTargetEvent event) {
			return editorPage.getServer();
		}

		/**
		 * Schedule a job to execute the operation of application replacement.
		 * 
		 * @param selectedProj the dragged-and-dropped project whose contents will
		 *     be used to replace the contents of an existing cloud application
		 * @param targetModule the corresponding <code>CloudFoundryApplicationModule</code>
		 *     of the selected existing application.
		 */
		protected void scheduleApplicationReplacement(final IProject selectedProj,
			final CloudFoundryApplicationModule targetModule) {
			Job job = new Job("Replace cloud application") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					new ReplaceCloudApplicationAction(targetModule, editorPage) {
						@Override
						protected boolean selectReplaceProject() {
							selectProj = selectedProj;
							return true;
						}
					}.run();
					return Status.OK_STATUS;
				}

			};
			job.setPriority(Job.SHORT);
			job.schedule();
		}
		
		@Override
		public boolean performDrop(final Object data) {
			final String jobName = "Deploying application"; //$NON-NLS-1$
			UIJob job = new UIJob(jobName) {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {

					if (data instanceof IStructuredSelection) {
						Object modObj = ((IStructuredSelection) data).getFirstElement();
						IProject prj = null;
						if (modObj instanceof IProject) {
							prj = (IProject) modObj;
						}
						else if (modObj instanceof IJavaProject) {
							prj = ((IJavaProject) modObj).getProject();
						}

						if (prj != null) {

							final CloudFoundryServer cloudServer = (CloudFoundryServer) editorPage.getServer()
									.getOriginal().loadAdapter(CloudFoundryServer.class, monitor);

							if (cloudServer != null) {

								final String moduleName = prj.getName();
								
								// Now it should decide publish a new application or 
								// replace an existing application firstly.
								final IProject selectedProject = prj;
								
								final boolean[] result = new boolean[2];
								// result[0] shows whether the user cancel the operation,
								result[0] = false;
								// while result[1] shows the user choose OK (result[0] == true) 
								// and the logical whether needs to continue or not.
								result[1] = false;
								
								if (!cloudServer.getBehaviour().existBoundModule(selectedProject)) {
									Display.getDefault().syncExec(new Runnable() {

										public void run() {
											DragAndDropProjectProcessChoiceWizard choiceWizard = 
												new DragAndDropProjectProcessChoiceWizard(cloudServer);
											WizardDialog dialog = new WizardDialog(editorPage.getEditorSite().getShell(), choiceWizard);
											if (Window.OK == dialog.open()) {
												result[0] = true;
												if (choiceWizard.choosePublish()) {
													result[1] = true;
												} else {
													final boolean[] confirm = new boolean[1];
													CloudFoundryApplicationModule targetModule = choiceWizard.getReplacedApplication();
													final String replaceAppName = targetModule.getName();
													
													// The replacement can't be pulled back, so
													// give user a confirmation.
													Display.getDefault().syncExec(new Runnable() {
														public void run() {
															confirm[0] = MessageDialog.openConfirm(
																	editorPage.getSite().getShell(),
																	Messages.REPLACEMENT_CONFIRMATION_TITLE,
																	NLS.bind(
																			"Are you sure to use the contents of project {0} to replace the cloud application {1} ?",
																			moduleName,
																			replaceAppName));
														}
													});
													if (!confirm[0]) {
														result[0] = false;
													} else {
														scheduleApplicationReplacement(selectedProject, targetModule);
													}
												}
											}
										}

									});
									if (!result[0]) {
										return Status.CANCEL_STATUS;
									} else if (!result[1]) {
										// It indicates the user chooses to replace an existing application,
										// and the corresponding operation is executing in background job
										return Status.OK_STATUS;
									}
								}
								
								// Secondly, make sure there is no
								// CloundApplicationModule
								// with the same name of the selected project,
								// otherwise it will cause
								// the CloundApplicationModule with the same
								// name and its related remote
								// application in CF to be deleted.
								if (cloudServer.getBehaviour().existCloudApplicationModule(moduleName)) {
									result[0] = false;
									Display.getDefault().syncExec(new Runnable() {

										public void run() {
											result[0] = MessageDialog.openConfirm(
													editorPage.getSite().getShell(),
													Messages.REPLACEMENT_CONFIRMATION_TITLE,
													NLS.bind(
															"A cloud application with the name {0} already exists, {1}",
															moduleName,
															" do you want to replace its contents with the selected project which has the same name ?"));
										}

									});
									if (!result[0]) {
										return Status.CANCEL_STATUS;
									}
									
									scheduleApplicationReplacement(selectedProject, 
										cloudServer.getBehaviour().getCloudApplicationModule(moduleName));
									return Status.OK_STATUS;
								}

								// Make sure parent performs the drop first to
								// create the IModule. Unsupported modules will
								// not proceed result in IModule creation
								// therefore checks on the IProject are not
								// necessary
								ApplicationViewersDropAdapter.super.performDrop(data);

								// Now do a publish AFTER the IModule is
								// created. If no IModule is created (either
								// user cancels or project
								// is not supported), the publish operation will
								// do nothing.
								Job job = new Job(jobName) {

									@Override
									protected IStatus run(IProgressMonitor monitor) {
										cloudServer.getBehaviour().publishAdd(moduleName, monitor);
										return Status.OK_STATUS;
									}

								};
								job.setPriority(Job.INTERACTIVE);
								job.schedule();
								return Status.OK_STATUS;
							}
						}
					}
					return Status.CANCEL_STATUS;
				}
			};
			job.schedule();

			return true;
		}
	}

	private void createApplicationsSection() {
		Section section = getSection();
		section.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
		section.setText(Messages.COMMONTXT_APPLICATIONS);
		section.setDescription(Messages.ApplicationMasterPart_TEXT_APP_DESCRIP);

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(client);
		section.setClient(client);

		Composite headerComposite = toolkit.createComposite(section, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.marginTop = 0;
		rowLayout.marginBottom = 0;
		headerComposite.setLayout(rowLayout);
		headerComposite.setBackground(null);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		toolBarManager.createControl(headerComposite);

		applicationsViewer = new TableViewer(toolkit.createTable(client, SWT.NONE));
		applicationsViewer.setContentProvider(new TreeContentProvider());
		applicationsViewer.setLabelProvider(new ServerLabelProvider() {
			@Override
			public Image getImage(Object element) {
				Image image = super.getImage(element);

				if (element instanceof IModule) {
					IModule module = (IModule) element;
					CloudFoundryApplicationModule appModule = editorPage.getCloudServer()
							.getExistingCloudModule(module);
					if (appModule != null && appModule.getErrorMessage() != null) {
						return CloudFoundryImages.getImage(new DecorationOverlayIcon(image,
								CloudFoundryImages.OVERLAY_ERROR, IDecoration.BOTTOM_LEFT));
					}
				}

				return image;
			}

			@Override
			public String getText(Object element) {
				// This is the WTP module name (usually, it's the workspace
				// project name)
				String moduleName = super.getText(element);

				// However, the user has the option to specify a different name
				// when pushing an app, which is used as the cf app name. If
				// they are different, and the
				// corresponding workspace project is accessible, show both.
				// Otherwise, show the cf app name.

				if (element instanceof IModule) {

					IModule module = (IModule) element;

					// Find the corresponding Cloud Foundry-aware application
					// Module.
					CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule((IModule) element);

					if (appModule != null) {
						String cfAppName = appModule.getDeployedApplicationName();

						if (cfAppName != null) {

							// Be sure not to show a null WTP module name,
							// although
							// that should not be encountered
							if (moduleName != null
									&& !cfAppName.equals(moduleName)
									&& CloudFoundryProperties.isModuleProjectAccessible.testProperty(
											new IModule[] { module }, cloudServer)) {
								moduleName = cfAppName + " (" + moduleName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
							}
							else {
								moduleName = cfAppName;
							}
						}
					}
				}

				return moduleName;
			}

		});
		applicationsViewer.setInput(new CloudApplication[0]);
		applicationsViewer.setSorter(new CloudFoundryViewerSorter());

		applicationsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				IModule module = (IModule) selection.getFirstElement();

				if (currentModule != module) {
					currentModule = module;
					getManagedForm().fireSelectionChanged(ApplicationMasterPart.this, selection);
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).hint(250, SWT.DEFAULT).applyTo(applicationsViewer.getControl());

		int ops = DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		ApplicationViewersDropAdapter listener = new ApplicationViewersDropAdapter(applicationsViewer);
		applicationsViewer.addDropSupport(ops, transfers, listener);

		// create context menu
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				fillApplicationsContextMenu(manager);
			}
		});

		Menu menu = menuManager.createContextMenu(applicationsViewer.getControl());
		applicationsViewer.getControl().setMenu(menu);

		Action addRemoveApplicationAction = new Action(Messages.ApplicationMasterPart_TEXT_ADD_REMOVE,
				ImageResource.getImageDescriptor(ImageResource.IMG_ETOOL_MODIFY_MODULES)) {
			@Override
			public void run() {
				ModifyModulesWizard wizard = new ModifyModulesWizard(cloudServer.getServerOriginal());
				WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
				dialog.open();
			}
		};
		toolBarManager.add(addRemoveApplicationAction);

		// Fix for STS-2996. Moved from CloudFoundryApplicationsEditorPage
		toolBarManager.add(new RefreshApplicationEditorAction(editorPage));
		toolBarManager.update(true);
		section.setTextClient(headerComposite);

		getManagedForm().getToolkit().paintBordersFor(client);
	}

	private void createRoutesDomainsSection() {

		Section routeSection = toolkit.createSection(getSection().getParent(), Section.TITLE_BAR | Section.DESCRIPTION
				| Section.TWISTIE);
		routeSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(routeSection);
		routeSection.setText(Messages.ApplicationMasterPart_TEXT_ROUTES);
		routeSection.setExpanded(true);

		routeSection.clientVerticalSpacing = 0;

		Composite client = toolkit.createComposite(routeSection);
		client.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(client);
		routeSection.setClient(client);

		Button button = toolkit.createButton(client, Messages.ApplicationMasterPart_TEXT_REMOVE_BUTTON, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(button);

		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIJob uiJob = new UIJob(Messages.ApplicationMasterPart_JOB_REMOVE_ROUTE) {

					public IStatus runInUIThread(IProgressMonitor monitor) {
						CloudRoutesWizard wizard = new CloudRoutesWizard(cloudServer);

						WizardDialog dialog = new WizardDialog(editorPage.getEditorSite().getShell(), wizard);
						dialog.open();
						return Status.OK_STATUS;
					}

				};
				uiJob.setSystem(true);
				uiJob.setPriority(Job.INTERACTIVE);
				uiJob.schedule();
			}
		});

	}

	private void createServicesSection() {
		servicesSection = toolkit.createSection(getSection().getParent(), Section.TITLE_BAR | Section.DESCRIPTION
				| Section.TWISTIE);
		servicesSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(servicesSection);
		servicesSection.setText(Messages.COMMONTXT_SERVICES);
		servicesSection.setExpanded(true);
		servicesSection.setDescription(Messages.ApplicationMasterPart_TEXT_SERVICES_DESCRIP);
		// NOTE:Comment out as keeping section collapsed by default causes zero
		// height tables if no services are provided
		// servicesSection.addExpansionListener(new ExpansionAdapter() {
		//
		// @Override
		// public void expansionStateChanged(ExpansionEvent e) {
		// userExpanded = true;
		// }
		// });

		Composite client = toolkit.createComposite(servicesSection);
		client.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(client);
		servicesSection.setClient(client);

		Composite headerComposite = toolkit.createComposite(servicesSection, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.marginTop = 0;
		rowLayout.marginBottom = 0;
		headerComposite.setLayout(rowLayout);
		headerComposite.setBackground(null);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		toolBarManager.createControl(headerComposite);

		servicesViewer = new TableViewer(toolkit.createTable(client, SWT.MULTI));
		new ServiceViewerConfigurator().configureViewer(servicesViewer);

		servicesViewer.setContentProvider(new TreeContentProvider());
		servicesViewer.setLabelProvider(new ServicesTreeLabelProvider(servicesViewer) {

			protected Image getColumnImage(CloudService service, ServiceViewColumn column) {
				if (column == ServiceViewColumn.Tunnel) {
					TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
					if (handler.hasCaldecottTunnel(service.getName())) {
						return CloudFoundryImages.getImage(CloudFoundryImages.CONNECT);
					}
				}
				return null;
			}

		});
		servicesViewer.setSorter(new ServiceViewerSorter(servicesViewer) {

			@Override
			protected int compare(CloudService service1, CloudService service2, ServiceViewColumn sortColumn) {
				if (sortColumn == ServiceViewColumn.Tunnel) {
					TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
					if (handler.hasCaldecottTunnel(service1.getName())) {
						return -1;
					}
					else if (handler.hasCaldecottTunnel(service2.getName())) {
						return 1;
					}
					else {
						return 0;
					}
				}
				return super.compare(service1, service2, sortColumn);
			}

		});

		servicesViewer.setInput(new CloudService[0]);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(servicesViewer.getControl());

		Action addServiceAction = new Action(Messages.COMMONTXT_ADD_SERVICE, CloudFoundryImages.NEW_SERVICE) {
			@Override
			public void run() {
				IWizard wizard = new CloudFoundryServiceWizard(cloudServer);
				WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
				dialog.setBlockOnOpen(true);
				dialog.open();
			}
		};
		toolBarManager.add(addServiceAction);
		toolBarManager.update(true);
		servicesSection.setTextClient(headerComposite);

		// create context menu
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				fillServicesContextMenu(manager);
			}
		});

		Menu menu = menuManager.createContextMenu(servicesViewer.getControl());
		servicesViewer.getControl().setMenu(menu);
		editorPage.getSite().registerContextMenu(menuManager, servicesViewer);

		// Create drag source on the table
		int ops = DND.DROP_COPY;
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DragSourceAdapter listener = new DragSourceAdapter() {
			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) servicesViewer.getSelection();
				event.data = selection.getFirstElement();
				LocalSelectionTransfer.getTransfer().setSelection(selection);
			}

			@Override
			public void dragStart(DragSourceEvent event) {
				if (event.detail == DND.DROP_NONE || event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_COPY;
				}
				dragSetData(event);
			}

		};
		servicesViewer.addDragSupport(ops, transfers, listener);

		getManagedForm().getToolkit().paintBordersFor(client);

	}

	private void fillServicesContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) servicesViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		manager.add(new DeleteServicesAction(selection, cloudServer.getBehaviour(), editorPage));

		// FIXNS: Disable Caldecott feature in 1.5.1 until feature is supported
		// in the client-lib
		// List<IAction> caldecottAction = new
		// TunnelActionProvider(cloudServer).getTunnelActions(selection,
		// editorPage);
		// for (IAction action : caldecottAction) {
		// manager.add(action);
		// }
	}

	private void fillApplicationsContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) applicationsViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		IModule module = (IModule) selection.getFirstElement();
		if (module != null) {
			manager.add(new RemoveModuleAction(getSection().getShell(), editorPage.getServer().getOriginal(), module));
			if (module instanceof CloudFoundryApplicationModule) {
				CloudFoundryApplicationModule cloudModule = (CloudFoundryApplicationModule)module;
				manager.add(new ReplaceCloudApplicationAction(cloudModule, editorPage));
			}
		}
	}

}
