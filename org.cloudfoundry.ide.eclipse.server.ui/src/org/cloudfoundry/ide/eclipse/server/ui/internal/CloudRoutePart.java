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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudUIEvent;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Allows Cloud routes to be viewed and edited.
 */
public class CloudRoutePart extends UIPart {

	public static IEventSource<?> ROUTES_REMOVED = new CloudUIEvent(Messages.CloudRoutePart_TEXT_ROUTES_REMOVED);

	private CheckboxTableViewer viewer;

	private Button showInUseButton;

	private Button showRemovedRoutesButton;

	private Button removeButton;

	private static final String IN_USE = "x"; //$NON-NLS-1$

	private List<CloudRoute> routesToRemove = new ArrayList<CloudRoute>();

	private List<CloudRoute> allRoutes = new ArrayList<CloudRoute>();

	private final Color DISABLED = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

	protected enum RouteColumn {

		NAME(Messages.COMMONTXT_NAME, 250), DOMAIN(Messages.CloudRoutePart_TEXT_ROUTE_DOMAIN, 100), IN_USE(Messages.CloudRoutePart_TEXT_ROUTE_INUSE, 30);

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
		return routesToRemove;
	}

	public void setInput(List<CloudRoute> routes) {

		if (routes == null || routes.isEmpty()) {
			notifyStatusChange(CloudFoundryPlugin.getErrorStatus(Messages.CloudRoutePart_ERROR_NO_ROUTE_AVAIL));
			return;
		}

		this.allRoutes = new ArrayList<CloudRoute>(routes);

		viewer.setInput(routes);

		refreshAll();
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
		showInUseButton = new Button(parent, SWT.CHECK);
		showInUseButton.setText(Messages.ROUTES_SHOW_IN_USE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(showInUseButton);

		showInUseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshAll();
			}

		});

		showRemovedRoutesButton = new Button(parent, SWT.CHECK);
		showRemovedRoutesButton.setText(Messages.ROUTES_SHOW_REMOVED);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(showRemovedRoutesButton);

		showRemovedRoutesButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshAll();
				String removeButtonLabel = showRemovedRoutesButton.getSelection() ? Messages.CloudRoutePart_UNDO : Messages.COMMONTXT_REMOVE;
				removeButton.setText(removeButtonLabel);
			}

		});
	}

	protected List<CloudRoute> getAllUnused() {
		List<CloudRoute> unused = new ArrayList<CloudRoute>();
		for (CloudRoute route : allRoutes) {
			if (!route.inUse()) {
				unused.add(route);
			}
		}
		return unused;
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
				viewer.setCheckedElements(getAllUnused().toArray(new CloudRoute[0]));
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
		removeButton.setText(Messages.COMMONTXT_REMOVE);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(removeButton);

		removeButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// The remove button has dual purpose:
				// 1. if viewing list of available routes, any selection will
				// remove them
				// 2. if viewing list of removed routes, any selection will undo
				// the removed routes
				Object[] selectedRoutes = viewer.getCheckedElements();
				if (selectedRoutes == null) {
					selectedRoutes = new CloudRoute[0];
				}
				List<CloudRoute> toRemove = new ArrayList<CloudRoute>(routesToRemove);

				if (!showRemovedRoutesButton.getSelection()) {
					for (Object obj : selectedRoutes) {
						if (!toRemove.contains(obj)) {
							toRemove.add((CloudRoute) obj);
						}
					}
				}
				else {
					// Undo those that are checked by removing them from the
					// list of routes to delete
					for (Object obj : selectedRoutes) {
						toRemove.remove(obj);
					}
				}

				remove(toRemove);
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

		final Table table = new Table(tableArea, SWT.BORDER | SWT.CHECK);
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

		viewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object items, Object item) {
				if (item instanceof CloudRoute) {
					CloudRoute route = (CloudRoute) item;
					return route.inUse() ? showInUseButton.getSelection()
							: (showRemovedRoutesButton.getSelection() && routesToRemove.contains(route))
									|| (!showRemovedRoutesButton.getSelection() && !routesToRemove.contains(route));
				}
				return false;
			}
		});

		viewer.getTable().addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {

				Widget item = event.item;
				// Disable selecting routes that are in use
				if (event.detail == SWT.CHECK && item instanceof TableItem) {
					TableItem tableItem = (TableItem) item;

					Object element = tableItem.getData();

					if (element instanceof CloudRoute && ((CloudRoute) element).inUse()) {
						event.doit = false;
						event.detail = SWT.NONE;
						tableItem.setChecked(false);
					}
				}
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
			tableColumn.addSelectionListener(new ColumnSortListener(tableViewer) {

				protected void refresh() {
					refreshAll();
				}

			});

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
	 * @param updatedToRemove elements to remove. Passing a null or empty list
	 * will clear the list of items to remove.
	 */
	protected void remove(List<?> updatedToRemove) {

		// If no change, then do nothing to avoid firing change events
		if (routesToRemove.equals(updatedToRemove)) {
			return;
		}

		routesToRemove.clear();

		if (updatedToRemove != null) {
			for (Object obj : updatedToRemove) {
				if (obj instanceof CloudRoute) {
					CloudRoute route = (CloudRoute) obj;
					if (!routesToRemove.contains(route)) {
						routesToRemove.add(route);
					}
				}
			}
		}

		notifyChange(new PartChangeEvent(routesToRemove, Status.OK_STATUS, ROUTES_REMOVED));

		refreshAll();

	}

	protected void refreshAll() {
		viewer.refresh(true);

		if (showInUseButton.getSelection()) {
			TableItem[] items = viewer.getTable().getItems();
			for (TableItem item : items) {
				CloudRoute route = (CloudRoute) item.getData();
				if (route.inUse()) {
					item.setForeground(DISABLED);
				}
			}
		}
	}

	protected class RoutesContentProvider implements IStructuredContentProvider {

		public void dispose() {

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Collection) {
				return ((Collection<?>) inputElement).toArray(new Object[0]);
			}
			return null;
		}
	}
}
