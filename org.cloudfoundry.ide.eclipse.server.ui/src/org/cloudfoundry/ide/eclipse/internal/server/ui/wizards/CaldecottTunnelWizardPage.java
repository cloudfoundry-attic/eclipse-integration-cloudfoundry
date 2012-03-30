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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

public class CaldecottTunnelWizardPage extends WizardPage {

	private final CloudFoundryServerBehaviour behaviour;

	private final Set<CaldecottTunnelDescriptor> removeDescriptors = new HashSet<CaldecottTunnelDescriptor>();

	public CaldecottTunnelWizardPage(CloudFoundryServerBehaviour behaviour) {
		super("Caldecott Service Tunnels");
		this.behaviour = behaviour;
		setTitle("Caldecott Service Tunnels");
		setDescription("Manage Caldecott Tunnels");
		try {
			ImageDescriptor banner = CloudFoundryImages.getWizardBanner(behaviour.getCloudFoundryServer().getServer()
					.getServerType().getId());
			if (banner != null) {
				setImageDescriptor(banner);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	enum ViewColumn {
		ServiceName(150), UserName(100), Password(200);
		private int width;

		private ViewColumn(int width) {
			this.width = width;
		}

		public int getWidth() {
			return width;
		}
	}

	private CheckboxTableViewer servicesViewer;

	public void createControl(Composite parent) {

		getShell().setText("Active Caldecott Tunnels");

		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		Composite toolBarArea = new Composite(tableArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(toolBarArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBarArea);

		Label label = new Label(toolBarArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
		label.setText("List of Caldecott tunnels:");

		Table table = new Table(tableArea, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
		table.setSize(new Point(400, 400));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar bar = toolBarManager.createControl(toolBarArea);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).grab(true, false).applyTo(bar);

		servicesViewer = new CheckboxTableViewer(table);

		servicesViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
				// TODO Auto-generated method stub

			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// TODO Auto-generated method stub

			}

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Collection) {
					return ((Collection<?>) inputElement).toArray(new Object[0]);
				}
				return null;
			}

		});

		servicesViewer.setLabelProvider(new TreeLabelProvider(servicesViewer));

		configureViewer(servicesViewer);

		servicesViewer.getTable().addControlListener(new ControlListener() {

			public void controlResized(ControlEvent e) {
				resizeTable();
			}

			public void controlMoved(ControlEvent e) {

			}
		});

		Action addServiceAction = new Action("Remove Connection", CloudFoundryImages.REMOVE) {

			public void run() {

				Collection<CaldecottTunnelDescriptor> descriptors = (Collection) servicesViewer.getInput();
				if (descriptors == null || descriptors.isEmpty()) {
					return;
				}

				Object[] services = servicesViewer.getCheckedElements();
				if (services != null) {
					removeDescriptors.clear();
					for (Object obj : services) {
						if (!removeDescriptors.contains(obj)) {
							removeDescriptors.add((CaldecottTunnelDescriptor) obj);
						}
					}
				}

				descriptors = new HashSet<CaldecottTunnelDescriptor>(descriptors);
				if (!removeDescriptors.isEmpty()) {
					for (Iterator<?> it = removeDescriptors.iterator(); it.hasNext();) {
						Object obj = it.next();

						if (obj instanceof CaldecottTunnelDescriptor) {
							descriptors.remove(obj);

						}
					}

					servicesViewer.setInput(descriptors);
					servicesViewer.refresh();
				}
			}

			public String getToolTipText() {
				return "Remove the selected connection(s)";
			}
		};
		toolBarManager.add(addServiceAction);

		toolBarManager.update(true);

		setControl(tableArea);
		setInput();

		resizeTable();

	}

	protected void resizeTable() {
		Composite tableComposite = servicesViewer.getTable().getParent();
		Rectangle tableCompositeArea = tableComposite.getClientArea();
		int width = tableCompositeArea.width;
		resizeTableColumns(width, servicesViewer.getTable());
	}

	public Set<CaldecottTunnelDescriptor> getDescriptorsToRemove() {
		return removeDescriptors;
	}

	protected void setInput() {

		Collection<CaldecottTunnelDescriptor> cache = null;
		try {
			cache = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptors(behaviour.getCloudFoundryServer());
		}
		catch (CoreException e) {

		}

		if (cache != null) {
			servicesViewer.setInput(cache);
		}

	}

	public void configureViewer(final TableViewer tableViewer) {

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		int columnIndex = 0;
		ViewColumn[] columns = ViewColumn.values();
		String[] columnProperties = new String[columns.length];

		for (ViewColumn column : columns) {
			columnProperties[columnIndex] = column.name();
			TableColumn tableColumn = new TableColumn(table, SWT.NONE, columnIndex++);
			tableColumn.setData(column);
			tableColumn.setText(column.name());
			tableColumn.setWidth(column.getWidth());

		}

		tableViewer.setColumnProperties(columnProperties);

	}

	protected void resizeTableColumns(int tableWidth, Table table) {
		TableColumn[] tableColumns = table.getColumns();

		if (tableColumns.length == 0) {
			return;
		}

		int total = 0;

		// resize only if there is empty space at the end of the table
		for (TableColumn column : tableColumns) {
			total += column.getWidth();
		}

		if (total < tableWidth) {
			// resize the last one
			TableColumn lastColumn = tableColumns[tableColumns.length - 1];
			int newWidth = (tableWidth - total) + lastColumn.getWidth();
			lastColumn.setWidth(newWidth);
		}

	}

	protected class TreeLabelProvider extends LabelProvider implements ITableLabelProvider {

		private final TableViewer viewer;

		public TreeLabelProvider(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof CaldecottTunnelDescriptor) {
				return CloudFoundryImages.getImage(CloudFoundryImages.OBJ_SERVICE);
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof CaldecottTunnelDescriptor) {
				CaldecottTunnelDescriptor tunnel = (CaldecottTunnelDescriptor) element;
				return tunnel.getServiceName();
			}
			return super.getText(element);
		}

		public Image getColumnImage(Object element, int columnIndex) {

			TableColumn column = viewer.getTable().getColumn(columnIndex);
			if (column != null && column.getData() == ViewColumn.ServiceName) {
				return getImage(element);
			}

			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String result = null;
			TableColumn column = viewer.getTable().getColumn(columnIndex);
			if (column != null) {
				ViewColumn serviceColumn = (ViewColumn) column.getData();
				if (serviceColumn != null) {
					switch (serviceColumn) {
					case ServiceName:
						result = getText(element);
						break;
					case UserName:
						result = ((CaldecottTunnelDescriptor) element).getUserName();
						break;
					case Password:
						result = ((CaldecottTunnelDescriptor) element).getPassword();
						break;
					}
				}
			}
			return result;
		}

	}

}
