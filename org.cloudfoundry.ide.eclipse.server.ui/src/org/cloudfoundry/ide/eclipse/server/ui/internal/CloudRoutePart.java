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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Allows Cloud routes to be viewed and edited.
 */
public class CloudRoutePart extends UIPart {

	private CheckboxTableViewer viewer;

	private Button activeButton;

	private Button showDeletedButton;

	private Button removeButton;

	private static final String IN_USE = "x";

	private List<CloudRoute> toDelete = new ArrayList<CloudRoute>();

	private List<CloudRoute> routes = new ArrayList<CloudRoute>();

	protected enum RouteColumn {

		NAME("Name", 250), DOMAIN("Domain", 100), IN_USE("In Use", 30);

		private String name;

		private int width;

		private RouteColumn(String name, int width) {
			this.name = name;
			this.width = width;
		}

		public String getName() {
			return name;
		}

		public int getWidth() {
			return width;
		}
	}

	public CloudRoutePart() {

	}

	public TableViewer getViewer() {
		return viewer;
	}

	public List<CloudRoute> getRoutesToDelete() {
		return toDelete;
	}

	public void setInput(List<CloudRoute> routes) {

		if (routes == null || routes.isEmpty()) {
			notifyStatusChange(CloudFoundryPlugin.getErrorStatus("No routes available."));
			return;
		}

		this.routes = new ArrayList<CloudRoute>(routes);

		viewer.setInput(routes);

	}

	public Control createPart(Composite parent) {

		Composite generalArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);

		createTableButtonArea(generalArea);

		createFilterButtons(generalArea);

		return generalArea;

	}

	protected void createFilterButtons(Composite parent) {
		activeButton = new Button(parent, SWT.CHECK);
		activeButton.setText(Messages.ROUTES_SHOW_IN_USE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(activeButton);

		activeButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshInput();
			}

		});

		showDeletedButton = new Button(parent, SWT.CHECK);
		showDeletedButton.setText(Messages.ROUTES_SHOW_REMOVED);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(showDeletedButton);

		showDeletedButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshInput();
			}

		});
	}

	protected void createSelectionButtonArea(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttons);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(buttons);

		Button selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText(Messages.SELECT_ALL);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(selectAll);

		selectAll.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setCheckedElements(routes.toArray(new CloudRoute[0]));
			}

		});

		Button deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText(Messages.DESELECT_ALL);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(selectAll);

		deselectAll.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setCheckedElements(Collections.emptyList().toArray(new CloudRoute[0]));
			}

		});

		removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText(Messages.REMOVE);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(removeButton);

		removeButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				Object[] routes = viewer.getCheckedElements();
				if (routes != null) {
					remove(Arrays.asList(routes));
				}
				else {
					remove(Collections.EMPTY_LIST);
				}
			}

		});

	}

	protected void createTableButtonArea(Composite parent) {

		Composite buttonsAndViewer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonsAndViewer);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(buttonsAndViewer);

		Composite tableArea = new Composite(buttonsAndViewer, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		createSelectionButtonArea(buttonsAndViewer);

		final Table table = new Table(tableArea, SWT.BORDER | SWT.MULTI | SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		viewer = new CheckboxTableViewer(table);

		viewer.setContentProvider(new RoutesContentProvider());

		viewer.setLabelProvider(new TreeLabelProvider(viewer));

		viewer.setSorter(new ViewerSorter() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				TableColumn sortColumn = table.getSortColumn();
				if (sortColumn != null) {
					RouteColumn column = (RouteColumn) sortColumn.getData();
					int result = 0;
					int sortDirection = table.getSortDirection();
					if (column != null) {
						if (e1 instanceof CloudRoute && e2 instanceof CloudRoute) {
							CloudRoute rt1 = (CloudRoute) e1;
							CloudRoute rt2 = (CloudRoute) e2;

							switch (column) {
							case NAME:
								result = rt1.getName().toLowerCase().compareTo(rt2.getName().toLowerCase());
								break;
							case DOMAIN:
								result = rt1.getDomain().getName().compareTo(rt2.getDomain().getName());
								break;
							case IN_USE:
								if (rt1.inUse() && !rt2.inUse()) {
									result = -1;
								}
								else if (rt2.inUse() && !rt1.inUse()) {
									result = 1;
								}
								break;
							}

						}
					}
					return sortDirection == SWT.UP ? result : -result;
				}

				return super.compare(viewer, e1, e2);
			}

		});

		addColumns(viewer);

		new TableResizeHelper(viewer).enableResizing();

	}

	public void addColumns(final TableViewer tableViewer) {

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		int columnIndex = 0;

		List<String> columnProperties = new ArrayList<String>();
		TableColumn sortColumn = null;
		for (RouteColumn clm : RouteColumn.values()) {
			TableColumn tableColumn = new TableColumn(table, SWT.NONE, columnIndex++);
			tableColumn.setText(clm.getName());
			tableColumn.setWidth(clm.getWidth());
			tableColumn.setData(clm);
			columnProperties.add(clm.getName());
			tableColumn.addSelectionListener(new ColumnSortListener(tableViewer));

			if (sortColumn == null) {
				sortColumn = tableColumn;
			}
		}

		if (sortColumn != null) {
			table.setSortColumn(sortColumn);
			table.setSortDirection(SWT.UP);
		}
		tableViewer.setColumnProperties(columnProperties.toArray(new String[0]));

	}

	protected class TreeLabelProvider extends LabelProvider implements ITableLabelProvider {

		private final TableViewer viewer;

		public TreeLabelProvider(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public Image getImage(Object element) {

			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof CloudRoute) {
				return ((CloudRoute) element).getName();
			}
			return super.getText(element);
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String result = null;

			TableColumn column = viewer.getTable().getColumn(columnIndex);
			if (column != null) {
				switch (columnIndex) {
				case 0:
					result = getText(element);
					break;
				case 1:
					if (element instanceof CloudRoute) {
						result = ((CloudRoute) element).getDomain().getName();
					}
					break;
				case 2:
					if (element instanceof CloudRoute && ((CloudRoute) element).inUse()) {
						return IN_USE;
					}
					break;
				}

			}
			return result;
		}

	}

	/**
	 * 
	 * @param toRemove elements to remove. Passing a null or empty list will
	 * clear the list of items to remove.
	 */
	protected void remove(List<?> toRemove) {
		toDelete.clear();

		if (toRemove != null) {
			for (Object obj : toRemove) {
				if (obj instanceof CloudRoute) {
					CloudRoute route = (CloudRoute) obj;
					toDelete.add(route);
				}
			}
		}

		notifyChange(new PartChangeEvent(toDelete, Status.OK_STATUS, this));

		refreshInput();
	}

	protected void refreshInput() {
		setInput(routes);
		viewer.refresh(true);
	}

	protected class RoutesContentProvider implements IStructuredContentProvider {

		public void dispose() {

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Collection) {
				List<CloudRoute> filtered = new ArrayList<CloudRoute>();
				for (Object obj : (Collection<?>) inputElement) {
					CloudRoute route = (CloudRoute) obj;

					boolean showActive = activeButton.getSelection();
					if (route.inUse() && !showActive) {
						continue;
					}
					else {
						if (!showDeletedButton.getSelection() || toDelete.contains(route)) {
							filtered.add(route);
						}
					}

				}
				return filtered.toArray(new Object[0]);
			}
			return null;
		}
	}
}
