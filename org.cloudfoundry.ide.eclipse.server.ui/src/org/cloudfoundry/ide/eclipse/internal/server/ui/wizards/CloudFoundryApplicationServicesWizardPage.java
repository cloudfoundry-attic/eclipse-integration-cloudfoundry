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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewColumn;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewerConfigurator;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewerSorter;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServicesTreeLabelProvider;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.TreeContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;

public class CloudFoundryApplicationServicesWizardPage extends WizardPage {

	// This page is optional and can be completed at any time
	private final boolean canFinish = true;

	private final String serverTypeId;

	private final CloudFoundryServer cloudServer;

	private CheckboxTableViewer servicesViewer;

	private static final String DESCRIPTION = "Bind or add new services";

	private static final CloudService[] NO_SERVICES = new CloudService[0];

	/**
	 * Services, either existing or new, that a user has checked for binding.
	 */
	private final Map<String, CloudService> selectedServicesToBind = new HashMap<String, CloudService>();

	/**
	 * This is a list of services to add to the CF server. This may not
	 * necessarily match all the services a user has selected to bind to an
	 * application, as a user may add a service, but uncheck it for binding.
	 */
	private final Set<CloudService> servicesToAdd = new HashSet<CloudService>();

	/**
	 * All services both existing and added, used to refresh the input of the
	 * viewer
	 */
	private final List<CloudService> allServices = new ArrayList<CloudService>();

	private final CloudFoundryApplicationModule module;

	private final ApplicationWizardDescriptor descriptor;

	public CloudFoundryApplicationServicesWizardPage(CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule module, ApplicationWizardDescriptor descriptor) {
		super("Services");
		this.cloudServer = cloudServer;
		this.serverTypeId = module.getServerTypeId();
		this.module = module;
		this.descriptor = descriptor;
		populatedServicesFromLastDeployment();
	}

	public boolean isPageComplete() {
		return canFinish;
	}

	public void createControl(Composite parent) {
		setTitle("Services selection");
		setDescription(DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		Composite toolBarArea = new Composite(tableArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(toolBarArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBarArea);

		Label label = new Label(toolBarArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
		label.setText("Select services to bind to the application:");

		Table table = new Table(tableArea, SWT.BORDER | SWT.SINGLE | SWT.CHECK);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar bar = toolBarManager.createControl(toolBarArea);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).grab(true, false).applyTo(bar);

		servicesViewer = new CheckboxTableViewer(table);

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

		new ServiceViewerConfigurator().enableAutomaticViewerResizing().configureViewer(servicesViewer);

		servicesViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				Object[] services = servicesViewer.getCheckedElements();
				if (services != null) {
					selectedServicesToBind.clear();
					for (Object obj : services) {
						CloudService service = (CloudService) obj;
						selectedServicesToBind.put(service.getName(), service);
					}
					setServicesToBind();
				}
			}
		});

		Action addServiceAction = new Action("Add Service", CloudFoundryImages.NEW_SERVICE) {

			public void run() {
				boolean deferAdditionOfService = true;
				CloudFoundryServiceWizard wizard = new CloudFoundryServiceWizard(cloudServer, deferAdditionOfService);
				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.OK) {
					CloudService addedService = wizard.getService();
					if (addedService != null) {
						addService(addedService);
					}
				}
			}

			public String getToolTipText() {
				return "Add a service to the server and automatically select it for the deployed application.";
			}
		};
		toolBarManager.add(addServiceAction);

		toolBarManager.update(true);

		setControl(tableArea);
		setInput();
	}

	/**
	 * Also automatically selects the added service to be bound to the
	 * application.
	 * @param service that was added and will also be automatically selected to
	 * be bound to the application.
	 */
	protected void addService(CloudService service) {
		// FIXNS: check if duplicate services or with the same name but
		// different type are
		// allowable
		servicesToAdd.add(service);
		allServices.add(service);
		selectedServicesToBind.put(service.getName(), service);
		setServicesToBind();
		setServicesAdded();
		setSelection();
	}

	protected void setInput() {
		List<CloudService> existingServices = null;

		try {
			existingServices = cloudServer.getBehaviour().getServices(new NullProgressMonitor());
		}
		catch (CoreException e) {
			setErrorText("Unable to obtain current list of messages due to: " + e.getLocalizedMessage());
		}

		if (existingServices == null) {
			servicesViewer.setInput(NO_SERVICES);
		}
		else {

			// All available services should be displayed
			allServices.clear();
			allServices.addAll(existingServices);
			servicesViewer.setInput(allServices.toArray(new CloudService[] {}));
			// Also add any actual services to the selected services ID map as
			// the map may have been prepopulated
			for (CloudService service : existingServices) {
				if (selectedServicesToBind.containsKey(service.getName())) {
					selectedServicesToBind.put(service.getName(), service);
				}
				setServicesToBind();
			}
			setSelection();
		}
	}

	protected void populatedServicesFromLastDeployment() {
		// Set the initial selection based on the past deployment history
		selectedServicesToBind.clear();
		List<String> serviceNames = descriptor.getDeploymentInfo().getServices();
		// Keep it light, there only populate the names as that may be the
		// only information
		// available, and heavier requests shouldn't be made at the time of
		// population. Rely on the page actually opening to populate the
		// actual services
		if (serviceNames != null) {
			for (String name : serviceNames) {
				selectedServicesToBind.put(name, null);
			}
		}
	}

	protected void setSelection() {
		servicesViewer.setInput(allServices.toArray(new CloudService[] {}));
		servicesViewer.setCheckedElements(selectedServicesToBind.values().toArray());
	}

	protected void setServicesToBind() {
		descriptor.getDeploymentInfo().setServices(new ArrayList<String>(selectedServicesToBind.keySet()));
	}

	protected void setServicesAdded() {
		descriptor.setCreatedCloudServices(new ArrayList<CloudService>(servicesToAdd));
	}

	public void setErrorText(String newMessage) {
		// Clear the message
		setMessage("");
		super.setErrorMessage(newMessage);
	}

	public void setMessageText(String newMessage) {
		setErrorMessage("");
		super.setMessage(newMessage);
	}

}
