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
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TunnelDisplayPart {

	private TableViewer servicesViewer;

	private final Shell shell;

	private final CloudFoundryServer cloudServer;

	private final List<String> servicesWithTunnels;

	public TunnelDisplayPart(Shell shell, CloudFoundryServer cloudServer, List<String> servicesWithTunnels) {
		this.shell = shell;
		this.cloudServer = cloudServer;
		this.servicesWithTunnels = servicesWithTunnels;
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

	public TableViewer getViewer() {
		return servicesViewer;
	}

	protected void resizeTable() {
		Composite tableComposite = servicesViewer.getTable().getParent();
		Rectangle tableCompositeArea = tableComposite.getClientArea();
		int width = tableCompositeArea.width;
		resizeTableColumns(width, servicesViewer.getTable());
	}

	public Control createControl(Composite parent) {

		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		Table table = new Table(tableArea, SWT.BORDER | SWT.MULTI);
		table.setSize(new Point(400, 400));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		servicesViewer = new TableViewer(table);

		servicesViewer.setContentProvider(new IStructuredContentProvider() {

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

		addTableActions();

		setInput();

		resizeTable();

		return tableArea;

	}

	protected void setInput() {

		Collection<CaldecottTunnelDescriptor> cache = null;

		if (servicesWithTunnels != null && !servicesWithTunnels.isEmpty()) {
			cache = new ArrayList<CaldecottTunnelDescriptor>();
			CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
			for (String serviceName : servicesWithTunnels) {
				CaldecottTunnelDescriptor descriptor = handler.getCaldecottTunnel(serviceName);
				if (descriptor != null) {
					cache.add(descriptor);
				}
			}
		}
		else {
			cache = CloudFoundryPlugin.getCaldecottTunnelCache().getDescriptors(cloudServer);
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

	protected List<CaldecottTunnelDescriptor> getSelectedCaldecotTunnelDescriptors() {
		IStructuredSelection selection = (IStructuredSelection) servicesViewer.getSelection();
		List<CaldecottTunnelDescriptor> descriptors = new ArrayList<CaldecottTunnelDescriptor>();
		if (!selection.isEmpty()) {
			Object[] servicesObjs = selection.toArray();
			for (Object serviceObj : servicesObjs) {
				descriptors.add((CaldecottTunnelDescriptor) serviceObj);

			}
		}
		return descriptors;
	}

	protected void addTableActions() {
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				List<CaldecottTunnelDescriptor> descriptors = getSelectedCaldecotTunnelDescriptors();
				List<IAction> actions = getViewerActions(descriptors);
				if (actions != null) {
					for (IAction action : actions) {
						manager.add(action);
					}
				}

			}
		});

		Menu menu = menuManager.createContextMenu(getViewer().getControl());
		getViewer().getControl().setMenu(menu);

	}

	protected List<IAction> getViewerActions(List<CaldecottTunnelDescriptor> descriptors) {
		List<IAction> actions = new ArrayList<IAction>();

		if (descriptors.size() == 1) {
			actions.add(new CopyPassword());
			actions.add(new CopyUserName());
		}

		return actions;
	}

	protected abstract class CopyTunnelInformation extends Action {

		public CopyTunnelInformation(String actionName, ImageDescriptor actionImage) {
			super(actionName, actionImage);
		}

		public void run() {
			Clipboard clipBoard = new Clipboard(shell.getDisplay());
			CaldecottTunnelDescriptor descriptor = getSelectedTunnelDescriptor();
			if (descriptor != null) {
				String value = getTunnelInformation(descriptor);
				clipBoard.setContents(new Object[] { value }, new TextTransfer[] { TextTransfer.getInstance() });
			}
		}

		protected CaldecottTunnelDescriptor getSelectedTunnelDescriptor() {

			List<CaldecottTunnelDescriptor> descriptors = getSelectedCaldecotTunnelDescriptors();

			return !descriptors.isEmpty() ? descriptors.get(0) : null;
		}

		abstract public String getToolTipText();

		abstract String getTunnelInformation(CaldecottTunnelDescriptor descriptor);

	}

	protected class CopyUserName extends CopyTunnelInformation {

		public CopyUserName() {
			super("Copy username", CloudFoundryImages.EDIT);
		}

		@Override
		public String getToolTipText() {
			return "Copy username";
		}

		@Override
		String getTunnelInformation(CaldecottTunnelDescriptor descriptor) {
			return descriptor.getUserName();
		}

	}

	protected class CopyPassword extends CopyTunnelInformation {

		public CopyPassword() {
			super("Copy password", CloudFoundryImages.EDIT);
		}

		@Override
		public String getToolTipText() {
			return "Copy password";
		}

		@Override
		String getTunnelInformation(CaldecottTunnelDescriptor descriptor) {
			return descriptor.getPassword();
		}

	}

}
