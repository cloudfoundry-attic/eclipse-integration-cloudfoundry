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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommandResolver;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServicesServer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

public class ServiceTunnelCommandPart {

	protected enum ControlData {
		Servers, Commands, Add, Delete, Edit;
	}

	private TreeViewer serversViewer;

	private TableViewer serviceCommandsViewer;

	private Button addCommandButton;

	private Button deleteCommandButton;

	private Button editCommandButton;

	public ServiceTunnelCommandPart() {

	}

	public Composite createControl(Composite parent) {

		Label serverLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel
				.setText("Manage commands to launch when creating a tunnel to a specific service in a Cloud Foundry server.");

		Composite generalArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);

		createViewerArea(generalArea);

		createButtonAreas(generalArea);

		setServerInput();
		return generalArea;
	}

	protected void createViewerArea(Composite parent) {
		Composite viewerArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(viewerArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewerArea);

		createServerArea(viewerArea);
		createServiceAppsArea(viewerArea);

	}

	protected void createServerArea(Composite parent) {
		Composite serverComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(serverComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(serverComposite);

		Label serverLabel = new Label(serverComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel.setText("Select a service:");

		Tree serverTree = new Tree(serverComposite, SWT.BORDER | SWT.SINGLE);

		serverTree.setData(ControlData.Servers);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(serverTree);

		serversViewer = new TreeViewer(serverTree);

		serversViewer.setContentProvider(new ServerServiceAppLabelContentProvider());
		serversViewer.setLabelProvider(new ServerServiceAppLabelProvider());
		serversViewer.setSorter(new ServerServiceAppSorter());

		serversViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				handleChange(event);
			}
		});
	}

	protected void createServiceAppsArea(Composite parent) {
		Composite serviceTableComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(serviceTableComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(serviceTableComposite);

		Label serviceLabel = new Label(serviceTableComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serviceLabel);
		serviceLabel.setText("Add, delete or edit a command:");

		createTableArea(serviceTableComposite);

	}

	protected void createTableArea(Composite parent) {

		Table table = new Table(parent, SWT.BORDER | SWT.SINGLE);

		table.setData(ControlData.Commands);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		serviceCommandsViewer = new TableViewer(table);

		serviceCommandsViewer.setContentProvider(new ServerServiceAppLabelContentProvider());
		serviceCommandsViewer.setLabelProvider(new ServerServiceAppLabelProvider());
		serviceCommandsViewer.setSorter(new ServerServiceAppSorter());

		serviceCommandsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				handleChange(event.getSource());
			}
		});

	}

	protected void createButtonAreas(Composite parent) {
		Composite buttonArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(buttonArea);

		Label filler = new Label(buttonArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(filler);
		filler.setText("");

		addCommandButton = new Button(buttonArea, SWT.PUSH);

		addCommandButton.setData(ControlData.Add);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(addCommandButton);
		addCommandButton.setText("Add");

		addCommandButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event.getSource());
			}

		});

		deleteCommandButton = new Button(buttonArea, SWT.PUSH);

		deleteCommandButton.setData(ControlData.Delete);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(deleteCommandButton);
		deleteCommandButton.setText("Delete");

		deleteCommandButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event.getSource());
			}

		});

		editCommandButton = new Button(buttonArea, SWT.PUSH);

		editCommandButton.setData(ControlData.Edit);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(editCommandButton);
		editCommandButton.setText("Edit");

		editCommandButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {

				handleChange(event.getSource());
			}

		});

	}

	protected void setServerInput() {

		List<ServicesServer> actualServers = new ServiceCommandResolver().getServerServiceCommands();
		if (actualServers != null) {
			serversViewer.setInput(actualServers);
		}
		else {
			serversViewer.setInput(new ArrayList<ServicesServer>(0));
		}

		setServiceCommandInput();

	}

	protected void setServiceCommandInput() {
		ServerService serviceCommands = getSelectedService();

		if (serviceCommands != null && serviceCommands.getLaunchCommands() != null) {
			serviceCommandsViewer.setInput(serviceCommands.getLaunchCommands());
		}
		else {
			serviceCommandsViewer.setInput(new ArrayList<ServicesServer>(0));
		}
	}

	protected ServerService getSelectedService() {
		ISelection iSelection = serversViewer.getSelection();
		if (iSelection instanceof IStructuredSelection) {
			Object selectObj = ((IStructuredSelection) iSelection).getFirstElement();
			if (selectObj instanceof ServerService) {
				return (ServerService) selectObj;
			}
		}
		return null;
	}

	protected ServiceCommand getSelectedCommand() {

		ISelection selection = serviceCommandsViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (ServiceCommand) ((IStructuredSelection) selection).getFirstElement();
		}
		return null;
	}

	protected void handleChange(Object eventSource) {

		ServiceCommand selectedCommand = getSelectedCommand();
		ServerService selectedService = getSelectedService();
		
		if (eventSource instanceof Control) {
			Control eventControl = (Control) eventSource;
			Object dataObj = eventControl.getData();
			if (dataObj instanceof ControlData) {
				ControlData controlData = (ControlData) dataObj;
				switch(controlData) {
				case Add:
					
					break;
				case Delete:
					
					break;
					
				case Edit:
					
					break;
				}
			}
		}


		if (selectedCommand != null) {
			addCommandButton.setEnabled(false);
			deleteCommandButton.setEnabled(true);
			editCommandButton.setEnabled(true);
		}
		else if (selectedService != null) {
			addCommandButton.setEnabled(true);
			deleteCommandButton.setEnabled(false);
			editCommandButton.setEnabled(false);
		}
		else {
			addCommandButton.setEnabled(false);
			deleteCommandButton.setEnabled(false);
			editCommandButton.setEnabled(false);
		}

	}

	static class ServerServiceAppSorter extends ViewerSorter {

		public ServerServiceAppSorter() {

		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ServicesServer && e1 instanceof ServicesServer) {
				String name1 = ((ServicesServer) e1).getServerName();
				String name2 = ((ServicesServer) e2).getServerName();
				return name1.compareTo(name2);
			}
			else if (e1 instanceof ServerService && e2 instanceof ServerService) {
				String name1 = ((ServerService) e1).getServiceName();
				String name2 = ((ServerService) e2).getServiceName();
				return name1.compareTo(name2);
			}

			return super.compare(viewer, e1, e2);
		}

	}

	static class ServerServiceAppLabelContentProvider implements ITreeContentProvider {
		private Object[] elements;

		public ServerServiceAppLabelContentProvider() {
		}

		public void dispose() {
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ServicesServer) {
				ServicesServer server = (ServicesServer) parentElement;
				List<ServerService> services = server.getServices();
				if (services != null) {
					return services.toArray();
				}
			}
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return elements;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>) {
				elements = ((List<?>) newInput).toArray();
			}
		}
	}

	static class ServerServiceAppLabelProvider extends LabelProvider {

		public ServerServiceAppLabelProvider() {

		}

		public String getText(Object element) {
			if (element instanceof ServicesServer) {
				return ((ServicesServer) element).getServerName();
			}
			else if (element instanceof ServerService) {
				return ((ServerService) element).getServiceName();
			}
			return super.getText(element);
		}

	}

	static class ServiceAppSorter extends ViewerSorter {

		public ServiceAppSorter() {

		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ServiceCommand && e1 instanceof ServiceCommand) {
				String name1 = ((ServiceCommand) e1).getExternalApplicationLaunchInfo().getExecutableName();
				String name2 = ((ServiceCommand) e2).getExternalApplicationLaunchInfo().getExecutableName();
				return name1.compareTo(name2);
			}

			return super.compare(viewer, e1, e2);
		}

	}

	static class ServiceAppContentProvider implements ITreeContentProvider {
		private Object[] elements;

		public ServiceAppContentProvider() {
		}

		public void dispose() {
		}

		public Object[] getChildren(Object parentElement) {
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return elements;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>) {
				elements = ((List<?>) newInput).toArray();
			}
		}
	}

	static class ServiceAppLabelProvider extends LabelProvider {

		public ServiceAppLabelProvider() {

		}

		public String getText(Object element) {
			if (element instanceof ServiceCommand) {
				ServiceCommand command = (ServiceCommand) element;
				return command.getExternalApplicationLaunchInfo().getExecutableName();
			}
			return super.getText(element);
		}

	}

}
