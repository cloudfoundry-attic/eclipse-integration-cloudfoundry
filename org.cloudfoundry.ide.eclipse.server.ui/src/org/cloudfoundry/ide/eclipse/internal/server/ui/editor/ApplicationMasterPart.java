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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.List;

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.DeleteServicesAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction.RefreshArea;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryServiceWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
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

	private boolean userExpanded;

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
			if (servicesSection != null && !userExpanded) {
				servicesSection.setExpanded((services == null ? 0 : services.size()) > 0);
				GridDataFactory.fillDefaults().grab(true, servicesSection.isExpanded())
						.hint(SWT.DEFAULT, servicesSection.isExpanded() ? SWT.DEFAULT : 0).applyTo(servicesSection);
				GridDataFactory.fillDefaults().grab(true, servicesSection.isExpanded())
						.hint(SWT.DEFAULT, servicesSection.isExpanded() ? SWT.DEFAULT : 0)
						.applyTo(servicesViewer.getControl());
				servicesSection.getParent().layout();
			}
			// servicesViewer.refresh(true);
		}
	}

	public void refreshUI() {
		applicationsViewer.setInput(cloudServer.getServerOriginal().getModules());
		// applicationsViewer.refresh(true);

		updateSections();
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
			Job job = new Job("Deploying application") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					boolean result = ApplicationViewersDropAdapter.super.performDrop(data);
					if (result) {
						editorPage.refresh(RefreshArea.MASTER, true);
					}

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
		section.setDescription("List of currently deployed applications.");

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
		applicationsViewer.setContentProvider(new ApplicationsMasterPartContentProvider());
		applicationsViewer.setLabelProvider(new ServerLabelProvider() {
			@Override
			public Image getImage(Object element) {
				Image image = super.getImage(element);

				if (element instanceof IModule) {
					IModule module = (IModule) element;
					ApplicationModule appModule = editorPage.getCloudServer().getApplication(module);
					if (appModule.getErrorMessage() != null) {
						return CloudFoundryImages.getImage(new DecorationOverlayIcon(image,
								CloudFoundryImages.OVERLAY_ERROR, IDecoration.BOTTOM_LEFT));
					}
				}

				return image;
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
		servicesSection.setDescription("Drag a service to the right hand side to associate it with an application.");

		servicesSection.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				userExpanded = true;
			}
		});

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

		userExpanded = false;
	}

	private void fillServicesContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) servicesViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		manager.add(new DeleteServicesAction(selection, cloudServer.getBehaviour(), editorPage));
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
