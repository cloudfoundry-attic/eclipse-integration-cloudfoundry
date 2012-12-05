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
import java.util.EventObject;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
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
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

public class ServiceTunnelCommandPart extends AbstractPart {

	protected enum ControlData {
		Add, Delete, Edit;
	}

	private TreeViewer serversViewer;

	private TableViewer serviceCommandsViewer;

	private Button addCommandButton;

	private Button deleteCommandButton;

	private Button editCommandButton;

	private Shell derivedShell;

	private List<ServicesServer> serversToUpdate;

	public ServiceTunnelCommandPart(List<ServicesServer> serversToUpdate) {
		this.serversToUpdate = serversToUpdate;
	}

	public Composite createControl(Composite parent) {

		derivedShell = parent.getShell();

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

		initUIState();
		return generalArea;
	}

	protected void createViewerArea(Composite parent) {
		Composite viewerArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(viewerArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewerArea);

		createServerArea(viewerArea);
		createServiceAppsArea(viewerArea);
	}

	protected void initUIState() {
		handleChange(null);
	}

	protected void createServerArea(Composite parent) {
		Composite serverComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(serverComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(serverComposite);

		Label serverLabel = new Label(serverComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel.setText("Select a service:");

		Tree serverTree = new Tree(serverComposite, SWT.BORDER | SWT.SINGLE);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(serverTree);

		serversViewer = new TreeViewer(serverTree);

		serversViewer.setContentProvider(new ServicesContentProvider());
		serversViewer.setLabelProvider(new ServicesLabelProvider());
		serversViewer.setSorter(new ServicesSorter());

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

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		serviceCommandsViewer = new TableViewer(table);

		serviceCommandsViewer.setContentProvider(new ServiceCommandsContentProvider());
		serviceCommandsViewer.setLabelProvider(new ServiceCommandLabelProvider());
		serviceCommandsViewer.setSorter(new ServiceCommandSorter());

		serviceCommandsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				handleChange(event);
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
				handleChange(event);
			}

		});

		deleteCommandButton = new Button(buttonArea, SWT.PUSH);

		deleteCommandButton.setData(ControlData.Delete);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(deleteCommandButton);
		deleteCommandButton.setText("Delete");

		deleteCommandButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event);
			}

		});

		editCommandButton = new Button(buttonArea, SWT.PUSH);

		editCommandButton.setData(ControlData.Edit);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(editCommandButton);
		editCommandButton.setText("Edit");

		editCommandButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {

				handleChange(event);
			}

		});

	}

	protected void setServerInput() {

		if (serversToUpdate == null) {
			serversToUpdate = new ArrayList<ServicesServer>();
		}

		serversViewer.setInput(serversToUpdate);

		setServiceCommandInput(null);
		setStatus(null);

	}

	protected void setServiceCommandInput(ServiceViewerWrapper wrapper) {

		if (wrapper != null && wrapper.getService() != null && wrapper.getService().getCommands() != null) {
			serviceCommandsViewer.setInput(wrapper.getService().getCommands());
		}
		else {
			serviceCommandsViewer.setInput(new ArrayList<ServicesServer>(0));
		}
	}

	protected CloudFoundryServer getSelectedServer() {
		ServicesServer server = getSelectedServicesServer();

		if (server != null) {
			return CloudServerUtil.getCloudServer(server.getServerName());
		}
		return null;
	}

	protected ServicesServer getSelectedServicesServer() {
		ISelection iSelection = serversViewer.getSelection();
		ServicesServer server = null;
		if (iSelection instanceof IStructuredSelection) {

			Object selectObj = ((IStructuredSelection) iSelection).getFirstElement();
			if (selectObj instanceof ServiceViewerWrapper) {
				server = ((ServiceViewerWrapper) selectObj).getServer();
			}
			else if (selectObj instanceof ServicesServer) {
				server = ((ServicesServer) selectObj);
			}

		}
		return server;
	}

	protected ServiceViewerWrapper getSelectedService() {
		ISelection iSelection = serversViewer.getSelection();
		if (iSelection instanceof IStructuredSelection) {
			Object selectObj = ((IStructuredSelection) iSelection).getFirstElement();
			if (selectObj instanceof ServiceViewerWrapper) {
				return (ServiceViewerWrapper) selectObj;
			}
		}
		return null;
	}

	public List<ServicesServer> getUpdatedServers() {
		return serversToUpdate;
	}

	protected ServiceCommand getSelectedCommand() {

		ISelection selection = serviceCommandsViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (ServiceCommand) ((IStructuredSelection) selection).getFirstElement();
		}
		return null;
	}

	protected Shell getShell() {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		if (shell == null) {
			shell = derivedShell;
		}
		return shell;
	}

	protected void addOrEditCommand() {

		ServiceCommand serviceCommand = getSelectedCommand();
		CloudFoundryServer cloudServer = getSelectedServer();
		ServiceViewerWrapper wrapper = getSelectedService();
		if (cloudServer != null) {
			ServiceCommandWizard wizard = new ServiceCommandWizard(cloudServer, serviceCommand);
			Shell shell = getShell();

			if (shell != null) {
				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				if (dialog.open() == Window.OK) {
					ServiceCommand newServiceCommand = wizard.getServiceCommand();

					if (newServiceCommand != null) {
						updateCommandViewerInput(serviceCommand, newServiceCommand, wrapper);
					}
				}
			}

		}

	}

	protected void deleteCommand() {
		ServiceCommand serviceCommand = getSelectedCommand();
		ServiceViewerWrapper wrapper = getSelectedService();
		if (serviceCommand != null && wrapper != null) {
			updateCommandViewerInput(serviceCommand, null, wrapper);
		}
	}

	/**
	 * 
	 * @param toDelete to remove from list of existing commands in the given
	 * wrapper
	 * @param toAdd to add to the list of existing commands in the given wrapper
	 * @param wrapper containing the old value, and that should contain the new
	 * value.
	 */
	protected void updateCommandViewerInput(ServiceCommand toDelete, ServiceCommand toAdd, ServiceViewerWrapper wrapper) {
		if (wrapper != null && (toDelete != null || toAdd != null)) {
			List<ServiceCommand> commands = wrapper.getService().getCommands();
			List<ServiceCommand> newCommands = new ArrayList<ServiceCommand>();
			if (commands != null) {
				for (ServiceCommand existingCommand : commands) {
					if (!existingCommand.equals(toDelete)) {
						newCommands.add(existingCommand);
					}
				}

			}
			if (toAdd != null) {
				newCommands.add(toAdd);
			}
			wrapper.getService().setCommands(newCommands);
			setServiceCommandInput(wrapper);
		}
	}

	protected void handleChange(EventObject eventSource) {
		if (eventSource != null) {
			Object source = eventSource.getSource();

			if (source instanceof Control) {
				Control control = (Control) source;
				Object dataObj = control.getData();
				if (dataObj instanceof ControlData) {
					ControlData controlData = (ControlData) dataObj;
					switch (controlData) {
					case Add:
						addOrEditCommand();
						break;
					case Delete:
						deleteCommand();
						break;
					case Edit:
						addOrEditCommand();
						break;

					}
				}
			}
			else if (source == serversViewer) {
				ServiceViewerWrapper selectedServiceWrapper = getSelectedService();
				setServiceCommandInput(selectedServiceWrapper);
			}
		}

		// Be sure to grab the latest selections AFTER any of the button
		// controls are handled above, as
		// the button operations may result in selection changes
		refreshButtons();
	}

	protected void refreshButtons() {
		ServiceCommand selectedCommand = getSelectedCommand();
		ServiceViewerWrapper serviceWrapper = getSelectedService();

		if (selectedCommand != null) {
			addCommandButton.setEnabled(false);
			deleteCommandButton.setEnabled(true);
			editCommandButton.setEnabled(true);
		}
		else if (serviceWrapper != null) {
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

	static class ServicesSorter extends ViewerSorter {

		public ServicesSorter() {

		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ServicesServer && e1 instanceof ServicesServer) {
				String name1 = ((ServicesServer) e1).getServerName();
				String name2 = ((ServicesServer) e2).getServerName();
				return name1.compareTo(name2);
			}
			else if (e1 instanceof ServiceViewerWrapper && e2 instanceof ServiceViewerWrapper) {
				String name1 = ((ServiceViewerWrapper) e1).getService().getServiceName();
				String name2 = ((ServiceViewerWrapper) e2).getService().getServiceName();
				return name1.compareTo(name2);
			}

			return super.compare(viewer, e1, e2);
		}

	}

	static class ServicesContentProvider implements ITreeContentProvider {
		private Object[] elements;

		public ServicesContentProvider() {
		}

		public void dispose() {
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ServicesServer) {
				ServicesServer server = (ServicesServer) parentElement;
				List<ServerService> services = server.getServices();
				if (services != null) {
					ServiceViewerWrapper[] wrapper = new ServiceViewerWrapper[services.size()];
					for (int i = 0; i < services.size() && i < wrapper.length; i++) {
						wrapper[i] = new ServiceViewerWrapper(server, services.get(i));
					}
					return wrapper;
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
			return getChildren(element) != null;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>) {
				elements = ((List<?>) newInput).toArray();
			}
		}
	}

	static class ServicesLabelProvider extends LabelProvider {

		public ServicesLabelProvider() {

		}

		public String getText(Object element) {
			if (element instanceof ServicesServer) {
				return ((ServicesServer) element).getServerName();
			}
			else if (element instanceof ServiceViewerWrapper) {
				return ((ServiceViewerWrapper) element).getService().getServiceName();
			}
			return super.getText(element);
		}

	}

	static class ServiceCommandSorter extends ViewerSorter {

		public ServiceCommandSorter() {

		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ServiceCommand && e1 instanceof ServiceCommand) {
				String name1 = ((ServiceCommand) e1).getExternalApplicationLaunchInfo().getDisplayName();
				String name2 = ((ServiceCommand) e2).getExternalApplicationLaunchInfo().getDisplayName();
				return name1.compareTo(name2);
			}

			return super.compare(viewer, e1, e2);
		}

	}

	static class ServiceCommandsContentProvider implements ITreeContentProvider {
		private Object[] elements;

		public ServiceCommandsContentProvider() {
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

	static class ServiceCommandLabelProvider extends LabelProvider {

		public ServiceCommandLabelProvider() {

		}

		public String getText(Object element) {
			if (element instanceof ServiceCommand) {
				ServiceCommand command = (ServiceCommand) element;
				return command.getExternalApplicationLaunchInfo().getDisplayName();
			}
			return super.getText(element);
		}

	}

	// Wrapper class around the service element that still retains
	// reference to the original service element, allowing modifications to it.
	static class ServiceViewerWrapper {

		private final ServerService service;

		private final ServicesServer server;

		public ServiceViewerWrapper(ServicesServer server, ServerService service) {
			this.server = server;
			this.service = service;
		}

		public ServerService getService() {
			return service;
		}

		public ServicesServer getServer() {
			return server;
		}

	}

}
