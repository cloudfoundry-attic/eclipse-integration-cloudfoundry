/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.DeleteServicesAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.RefreshApplicationEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryServiceWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
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

		@Override
		public boolean performDrop(final Object data) {
			UIJob job = new UIJob("Deploying application") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					ApplicationViewersDropAdapter.super.performDrop(data);

					return Status.OK_STATUS;
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
		section.setText("Applications");
		section.setDescription("Select a currently deployed application to see details.");

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
								moduleName = cfAppName + " (" + moduleName + ")";
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
		editorPage.getSite().registerContextMenu(menuManager, applicationsViewer);

		Action addRemoveApplicationAction = new Action("Add/Remove Applications",
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

	private void createServicesSection() {
		servicesSection = toolkit.createSection(getSection().getParent(), Section.TITLE_BAR | Section.DESCRIPTION
				| Section.TWISTIE);
		servicesSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(servicesSection);
		servicesSection.setText("Services");
		servicesSection.setExpanded(true);
		servicesSection.setDescription("Drag a service to the right hand side to bind it to an application.");
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
		servicesViewer.setSorter(new ServiceViewerSorter(servicesViewer, cloudServer.hasCloudSpace()) {

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

		Action addServiceAction = new Action("Add Service", CloudFoundryImages.NEW_SERVICE) {
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
		}
	}

}
