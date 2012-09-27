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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewColumn.ServiceViewColumnDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Configures the services viewer providers, sorters and columns. Must be called
 * before setting the viewer input.
 * 
 */
public class ServiceViewerConfigurator {

	private boolean addAutomaticViewerResizing;

	private final CloudFoundryServer cloudServer;

	public ServiceViewerConfigurator(CloudFoundryServer cloudServer) {
		this.addAutomaticViewerResizing = false;
		this.cloudServer = cloudServer;
	}

	public ServiceViewerConfigurator enableAutomaticViewerResizing() {
		addAutomaticViewerResizing = true;
		return this;
	}

	/**
	 * This must be called before setting the viewer input
	 * @param tableViewer
	 */
	public void configureViewer(final TableViewer tableViewer) {

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		int columnIndex = 0;
		ServiceViewColumnDescriptor descriptor = ServiceViewColumn.getServiceViewColumnDescriptor(cloudServer);

		ServiceViewColumn[] columns = descriptor != null ? descriptor.getServiceViewColumn() : null;

		if (columns == null) {
			return;
		}

		String[] columnProperties = new String[columns.length];
		TableColumn sortColumn = null;
		for (ServiceViewColumn column : columns) {
			columnProperties[columnIndex] = column.name();
			TableColumn tableColumn = new TableColumn(table, SWT.NONE, columnIndex++);
			tableColumn.setData(column);
			tableColumn.setText(column.name());
			tableColumn.setWidth(column.getWidth());
			tableColumn.addSelectionListener(new ColumnSortListener(tableViewer));

			if (column == ServiceViewColumn.Name) {
				sortColumn = tableColumn;
			}

		}

		// Add a control listener to resize the columns such that there is no
		// empty space
		// after the last column
		if (addAutomaticViewerResizing) {
			table.getParent().addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					Composite tableComposite = tableViewer.getTable().getParent();
					Rectangle tableCompositeArea = tableComposite.getClientArea();
					int width = tableCompositeArea.width;
					resizeTableColumns(width, table);
				}
			});
		}

		tableViewer.setColumnProperties(columnProperties);

		if (sortColumn != null) {
			table.setSortColumn(sortColumn);
			table.setSortDirection(SWT.UP);
		}
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

	protected class ColumnSortListener extends SelectionAdapter {

		private final TableViewer viewer;

		public ColumnSortListener(TableViewer viewer) {
			this.viewer = viewer;
		}

		public void widgetSelected(SelectionEvent e) {
			if (e.widget instanceof TableColumn) {
				TableColumn selected = (TableColumn) e.widget;
				Table table = viewer.getTable();
				TableColumn current = table.getSortColumn();

				int newDirection = SWT.UP;
				// If selecting a different column, keep the ascending
				// direction as default. Only switch
				// directions if the same column has been selected.
				if (current == selected) {
					newDirection = table.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
				}
				else {
					table.setSortColumn(selected);
				}
				table.setSortDirection(newDirection);
				viewer.refresh();
			}
		}
	}

}
