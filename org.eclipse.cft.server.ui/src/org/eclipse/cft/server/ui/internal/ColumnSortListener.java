/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ColumnSortListener extends SelectionAdapter {

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
			refresh();
		}
	}

	protected void refresh() {
		viewer.refresh();
	}
}