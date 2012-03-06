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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewerConfigurator;
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

	private static final String DESCRIPTION = "Add, delete or create new services";

	private static final CloudService[] NO_SERVICES = new CloudService[0];

	private final Map<String, CloudService> selectedServices = new HashMap<String, CloudService>();

	private final Set<CloudService> addedServices = new HashSet<CloudService>();

	private final ApplicationModule module;

	protected CloudFoundryApplicationServicesWizardPage(CloudFoundryServer cloudServer, ApplicationModule module) {
		super("Services");
		this.cloudServer = cloudServer;
		this.serverTypeId = module.getServerTypeId();
		this.module = module;
		populatedServicesFromLastDeployment();
	}

	public boolean isPageComplete() {
		return canFinish;
	}

	/**
	 * Returns a copy of the selected services to be added to a deployed app
	 * @return may be empty if no services selected, but never null
	 */
	public List<String> getSelectedServicesID() {
		return new ArrayList<String>(selectedServices.keySet());
	}

	/**
	 * Returns a copy of the newly services to be added to the server
	 * @return may be empty if nothing new added, but never null
	 */
	public List<CloudService> getAddedServices() {
		return new ArrayList<CloudService>(addedServices);
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
		label.setText("Check services to add to the deployed application:");

		Table table = new Table(tableArea, SWT.BORDER | SWT.SINGLE | SWT.CHECK);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar bar = toolBarManager.createControl(toolBarArea);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).grab(true, false).applyTo(bar);

		servicesViewer = new CheckboxTableViewer(table);

		new ServiceViewerConfigurator().enableAutomaticViewerResizing().configureViewer(servicesViewer);

		servicesViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				Object[] services = servicesViewer.getCheckedElements();
				if (services != null) {
					selectedServices.clear();
					for (Object obj : services) {
						CloudService service = (CloudService) obj;
						selectedServices.put(service.getName(), service);
					}
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
	 * Also automatically selects the added service
	 * @param service
	 */
	protected void addService(CloudService service) {
		addedServices.add(service);
		selectedServices.put(service.getName(), service);
		servicesViewer.add(service);
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
			servicesViewer.setInput(existingServices.toArray());
			// Also add any actual services to the selected services ID map as
			// the map may have been prepopulated
			for (CloudService service : existingServices) {
				if (selectedServices.containsKey(service.getName())) {
					selectedServices.put(service.getName(), service);
				}
			}
			setSelection();

		}
	}

	protected void populatedServicesFromLastDeployment() {
		// Set the initial selection based on the past deployment history
		selectedServices.clear();
		DeploymentInfo lastDeploymentInfo = module.getLastDeploymentInfo();
		if (lastDeploymentInfo != null) {
			List<String> serviceNames = lastDeploymentInfo.getServices();
			// Keep it light, there only populate the names as that may be the
			// only information
			// available, and heavier requests shouldn't be made at the time of
			// population. Rely on the page actually opening to populate the
			// actual services
			if (serviceNames != null) {
				for (String name : serviceNames) {
					selectedServices.put(name, null);
				}

			}
		}
	}

	protected void setSelection() {
		servicesViewer.setCheckedElements(selectedServices.values().toArray());
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
