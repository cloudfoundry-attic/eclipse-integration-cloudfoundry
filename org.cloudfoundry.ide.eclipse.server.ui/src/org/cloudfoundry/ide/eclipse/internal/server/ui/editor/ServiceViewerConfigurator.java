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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

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

	public ServiceViewerConfigurator() {
		this.addAutomaticViewerResizing = false;

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
		ServiceViewColumnDescriptor descriptor = ServiceViewColumn.getServiceViewColumnDescriptor();

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
