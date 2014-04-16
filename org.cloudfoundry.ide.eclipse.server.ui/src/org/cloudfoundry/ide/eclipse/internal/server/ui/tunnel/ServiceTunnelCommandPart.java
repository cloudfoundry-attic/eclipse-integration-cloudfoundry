/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandTerminal;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ITunnelServiceCommands;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommandManager;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceInfo;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ServiceCommandWizard;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
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

/**
 * Allows editing of commands per service
 *
 */
public class ServiceTunnelCommandPart extends UIPart {

	protected enum ControlData {
		Add, Delete, Edit;
	}

	private TableViewer serviceViewer;

	private TableViewer serviceCommandsViewer;

	private Button addCommandButton;

	private Button deleteCommandButton;

	private Button editCommandButton;

	private Shell derivedShell;

	private ITunnelServiceCommands serviceCommands;

	private final ServiceInfo serviceContext;

	private List<ServerService> services;

	public ServiceTunnelCommandPart(ITunnelServiceCommands serviceCommands, ServiceInfo serviceContext) {
		this.serviceCommands = serviceCommands;
		this.serviceContext = serviceContext;
		services = (serviceCommands != null && serviceCommands.getServices() != null) ? new ArrayList<ServerService>(
				serviceCommands.getServices()) : new ArrayList<ServerService>();
	}

	public Composite createPart(Composite parent) {

		derivedShell = parent.getShell();

		Composite generalArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);

		Label serverLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(serverLabel);
		serverLabel
				.setText("Manage commands to launch when creating a tunnel to a specific service in a Cloud Foundry server.");

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

		createServiceArea(viewerArea);
		createServiceAppsArea(viewerArea);
	}

	protected void initUIState() {
		handleChange(null);
	}

	protected void createServiceArea(Composite parent) {
		Composite serverComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(serverComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(serverComposite);

		Label serverLabel = new Label(serverComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel.setText("Select a service:");

		Table table = new Table(serverComposite, SWT.BORDER | SWT.SINGLE);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		serviceViewer = new TableViewer(table);

		serviceViewer.setContentProvider(new ServiceCommandsContentProvider());
		serviceViewer.setLabelProvider(new ServicesLabelProvider());
		serviceViewer.setSorter(new ServicesSorter());
		serviceViewer.addSelectionChangedListener(new ISelectionChangedListener() {

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

		if (services != null && !services.isEmpty()) {
			serviceViewer.setInput(services);
			ServerService initialSelection = services.get(0);
			if (serviceContext != null) {
				for (ServerService serv : services) {
					if (serv.getServiceInfo().equals(serviceContext)) {
						initialSelection = serv;
						break;
					}
				}
			}
			serviceViewer.setSelection(new StructuredSelection(initialSelection), true);
			setServiceCommandInput(initialSelection, null);
		}

		notifyStatusChange(null);
	}

	protected void setServiceCommandInput(ServerService service, ServiceCommand commandToSelect) {

		if (service != null && service.getCommands() != null) {
			serviceCommandsViewer.setInput(service.getCommands());
			if (commandToSelect != null) {
				serviceCommandsViewer.setSelection(new StructuredSelection(commandToSelect), true);
			}
		}
		else {
			serviceCommandsViewer.setInput(new ArrayList<ServerService>(0));
		}
	}

	protected ServerService getSelectedService() {
		ISelection iSelection = serviceViewer.getSelection();
		if (iSelection instanceof IStructuredSelection) {
			Object selectObj = ((IStructuredSelection) iSelection).getFirstElement();
			if (selectObj instanceof ServerService) {
				return (ServerService) selectObj;
			}
		}
		return null;
	}

	public ITunnelServiceCommands getUpdatedCommands() {

		// Set the updated commands
		serviceCommands.setServices(services);
		return serviceCommands;
	}

	protected ServiceCommand getSelectedCommand() {

		ISelection selection = serviceCommandsViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (ServiceCommand) ((IStructuredSelection) selection).getFirstElement();
		}
		return null;
	}

	protected Shell getShell() {
		Shell shell = CloudUiUtil.getShell();
		if (shell == null) {
			shell = derivedShell;
		}
		return shell;
	}

	/**
	 * If add is true, it will ignore any currently selected command, and simply
	 * add a new command. Otherwise it will attempt to edit the currently
	 * selected command, if one exists. If add is false, and there are no
	 * selected command, it will add a new command
	 * @param add true if adding a new command even if there is a currently
	 * selected command. False will ONLY edit an existing command, if one is
	 * currently selected. Otherwise it will add a new command.
	 */
	protected void addOrEditCommand(boolean add) {

		ServerService service = getSelectedService();
		if (service != null) {
			ServiceCommand serviceCommandToEdit = !add ? getSelectedCommand() : null;

			ServiceCommandWizard wizard = new ServiceCommandWizard(serviceCommands, service, serviceCommandToEdit);
			Shell shell = getShell();

			if (shell != null) {
				WizardDialog dialog = new WizardDialog(shell, wizard);
				if (dialog.open() == Window.OK) {

					ServiceCommand editedCommand = wizard.getEditedServiceCommand();

					if (editedCommand != null) {
						// Add the new one
						boolean added = new ServiceCommandManager(serviceCommands).addCommand(service.getServiceInfo(),
								editedCommand);

						if (!added) {
							notifyStatusChange(CloudFoundryPlugin.getErrorStatus("Failed to add command: "
									+ editedCommand.getDisplayName()));
							return;
						}
						else {
							// Delete the old command
							if (serviceCommandToEdit != null) {
								deleteCommand(serviceCommandToEdit);
							}

							CommandTerminal updatedTerminal = wizard.applyTerminalToAllCommands() ? editedCommand
									.getCommandTerminal() : null;

							setServiceCommandInput(service, editedCommand);
							applyTerminalToAllCommands(updatedTerminal);
						}

					}

				}
			}

		}
	}

	protected void deleteCommand(ServiceCommand toDelete) {
		ServerService serverService = getSelectedService();
		if (toDelete != null && serverService != null) {
			List<ServiceCommand> commands = serverService.getCommands();
			List<ServiceCommand> newCommands = new ArrayList<ServiceCommand>();
			if (commands != null) {
				for (ServiceCommand existingCommand : commands) {
					if (!existingCommand.equals(toDelete)) {
						newCommands.add(existingCommand);
					}
				}

			}
			serverService.setCommands(newCommands);

			setServiceCommandInput(serverService, null);

		}
	}

	protected void applyTerminalToAllCommands(CommandTerminal terminalToUpdate) {
		if (terminalToUpdate != null) {
			if (services != null) {
				for (ServerService service : services) {
					List<ServiceCommand> commands = service.getCommands();
					if (commands != null) {
						for (ServiceCommand cmd : commands) {
							cmd.setCommandTerminal(terminalToUpdate);
						}
					}
				}
			}
			serviceCommands.setDefaultTerminal(terminalToUpdate);
		}
	}

	protected void handleChange(EventObject eventSource) {
		// Clear errors first
		notifyStatusChange(Status.OK_STATUS);
		if (eventSource != null) {
			Object source = eventSource.getSource();

			if (source instanceof Control) {
				Control control = (Control) source;
				Object dataObj = control.getData();
				if (dataObj instanceof ControlData) {
					ControlData controlData = (ControlData) dataObj;
					switch (controlData) {
					case Add:
						addOrEditCommand(true);
						break;
					case Delete:
						ServiceCommand toDelete = getSelectedCommand();
						deleteCommand(toDelete);
						break;
					case Edit:
						addOrEditCommand(false);
						break;

					}
				}
			}
			else if (source == serviceViewer) {
				ServerService serverService = getSelectedService();
				setServiceCommandInput(serverService, null);
			}
		}

		// Be sure to grab the latest selections AFTER any of the button
		// controls are handled above, as
		// the button operations may result in selection changes
		refreshButtons();
	}

	protected void refreshButtons() {
		ServiceCommand selectedCommand = getSelectedCommand();
		ServerService serviceWrapper = getSelectedService();

		if (selectedCommand != null) {
			addCommandButton.setEnabled(true);
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
			if (e1 instanceof ServerService && e2 instanceof ServerService) {
				String name1 = ((ServerService) e1).getServiceInfo().getVendor();
				String name2 = ((ServerService) e2).getServiceInfo().getVendor();
				return name1.compareTo(name2);
			}

			return super.compare(viewer, e1, e2);
		}

	}

	static class ServicesLabelProvider extends LabelProvider {

		public ServicesLabelProvider() {

		}

		public String getText(Object element) {
			if (element instanceof ServerService) {
				return ((ServerService) element).getServiceInfo().getVendor();
			}

			return super.getText(element);
		}

	}

	static class ServiceCommandSorter extends ViewerSorter {

		public ServiceCommandSorter() {

		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ServiceCommand && e1 instanceof ServiceCommand) {
				String name1 = ((ServiceCommand) e1).getDisplayName();
				String name2 = ((ServiceCommand) e2).getDisplayName();
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
				return command.getDisplayName();
			}
			return super.getText(element);
		}

	}
}
