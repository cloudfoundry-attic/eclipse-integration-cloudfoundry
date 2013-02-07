/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
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
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.EnvironmentVariable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class EnvironmentVariablesPart extends AbstractPart {

	private List<EnvironmentVariable> variables;

	private TableViewer envVariablesViewer;

	public void setInput(List<EnvironmentVariable> variables) {
		this.variables = variables != null ? variables : new ArrayList<EnvironmentVariable>();
		if (envVariablesViewer != null) {
			envVariablesViewer.setInput(variables);
		}
	}

	public List<EnvironmentVariable> getVariables() {
		return variables;
	}

	public Control createPart(Composite parent) {

		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		Label viewerLabel = new Label(tableArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(viewerLabel);
		viewerLabel.setText("Right click to edit environment variables:");

		Table table = new Table(tableArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		table.setSize(new Point(400, 400));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		envVariablesViewer = new TableViewer(table);

		envVariablesViewer.setContentProvider(new IStructuredContentProvider() {

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Collection) {
					return ((Collection<?>) inputElement).toArray(new Object[0]);
				}
				return null;
			}

			public void dispose() {

			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

			}

		});

		envVariablesViewer.setLabelProvider(new EnvVarLabelProvider(envVariablesViewer));

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

		envVariablesViewer.setColumnProperties(columnProperties);

		// Add actions to edit the variables
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				List<IAction> actions = getViewerActions();
				if (actions != null) {
					for (IAction action : actions) {
						manager.add(action);
					}
				}
			}
		});

		Menu menu = menuManager.createContextMenu(envVariablesViewer.getControl());
		envVariablesViewer.getControl().setMenu(menu);

		return tableArea;
	}

	protected enum ViewerAction {
		Add, Delete, Edit
	}

	protected List<IAction> getViewerActions() {
		List<IAction> actions = new ArrayList<IAction>();
		final List<EnvironmentVariable> vars = getViewerSelection();

		actions.add(new Action(ViewerAction.Add.name()) {

			public void run() {
				handleActionSelected(ViewerAction.Add);
			}

			@Override
			public boolean isEnabled() {
				return true;
			}
		});

		actions.add(new Action(ViewerAction.Delete.name()) {

			public void run() {
				handleActionSelected(ViewerAction.Delete);
			}

			@Override
			public boolean isEnabled() {
				return vars != null && vars.size() > 0;
			}
		});

		actions.add(new Action(ViewerAction.Edit.name()) {

			public void run() {
				handleActionSelected(ViewerAction.Edit);
			}

			@Override
			public boolean isEnabled() {
				return vars != null && vars.size() == 1;
			}
		});

		return actions;
	}

	protected void handleActionSelected(ViewerAction action) {
		if (action != null) {
			switch (action) {
			case Add:
				handleAdd();
				break;
			case Edit:
				handleEdit();
				break;
			case Delete:
				handleDelete();
				break;
			}
		}
	}

	protected void handleAdd() {
		Shell shell = getShell();
		if (shell != null) {
			VariableDialogue dialogue = new VariableDialogue(shell, null);
			if (dialogue.open() == Window.OK) {
				updateVariables(dialogue.getEnvironmentVariable(), null);
			}
		}
	}

	protected void handleEdit() {
		Shell shell = getShell();
		List<EnvironmentVariable> selection = getViewerSelection();
		if (shell != null && selection != null && !selection.isEmpty()) {
			EnvironmentVariable toEdit = selection.get(0);
			VariableDialogue dialogue = new VariableDialogue(shell, toEdit);
			if (dialogue.open() == Window.OK) {
				updateVariables(dialogue.getEnvironmentVariable(), toEdit);
			}
		}
	}

	protected void handleDelete() {
		List<EnvironmentVariable> selection = getViewerSelection();
		if (selection != null && !selection.isEmpty()) {
			for (EnvironmentVariable toDelete : selection) {
				updateVariables(null, toDelete);
			}
		}
	}

	protected void updateVariables(EnvironmentVariable add, EnvironmentVariable delete) {
		if (variables == null) {
			variables = new ArrayList<EnvironmentVariable>();
		}

		if (delete != null) {
			List<EnvironmentVariable> updatedList = new ArrayList<EnvironmentVariable>();

			for (EnvironmentVariable var : variables) {
				if (!var.equals(delete)) {
					updatedList.add(var);
				}
			}

			variables = updatedList;
		}
		
		if (add != null) {
			variables.add(add);
		}

		setInput(variables);
	}

	protected List<EnvironmentVariable> getViewerSelection() {
		IStructuredSelection selection = (IStructuredSelection) envVariablesViewer.getSelection();
		List<EnvironmentVariable> selectedVars = new ArrayList<EnvironmentVariable>();
		if (!selection.isEmpty()) {
			Object[] servicesObjs = selection.toArray();
			for (Object serviceObj : servicesObjs) {
				selectedVars.add((EnvironmentVariable) serviceObj);
			}
		}
		return selectedVars;
	}

	protected void setViewerSelection(EnvironmentVariable var) {
		if (var != null) {
			envVariablesViewer.setSelection(new StructuredSelection(var));
		}
	}

	enum ViewColumn {
		Variable(200), Value(200);
		private int width;

		private ViewColumn(int width) {
			this.width = width;
		}

		public int getWidth() {
			return width;
		}
	}

	protected class EnvVarLabelProvider extends LabelProvider implements ITableLabelProvider {

		private final TableViewer viewer;

		public EnvVarLabelProvider(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String result = null;
			TableColumn column = viewer.getTable().getColumn(columnIndex);
			if (column != null) {
				ViewColumn serviceColumn = (ViewColumn) column.getData();
				if (serviceColumn != null) {
					EnvironmentVariable var = (EnvironmentVariable) element;
					switch (serviceColumn) {
					case Variable:
						result = var.getVariable();
						break;
					case Value:
						result = var.getValue();
						break;
					}
				}
			}
			return result;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

	}

	protected class VariableDialogue extends Dialog {

		private EnvironmentVariable envVar;

		private Text name;

		private Text value;

		public VariableDialogue(Shell shell, EnvironmentVariable envVar) {
			super(shell);
			this.envVar = new EnvironmentVariable();

			if (envVar != null) {
				this.envVar.setValue(envVar.getValue());
			}
			if (envVar != null) {
				this.envVar.setVariable(envVar.getVariable());
			}
		}

		public EnvironmentVariable getEnvironmentVariable() {
			return envVar;
		}

		@Override
		protected Control createButtonBar(Composite parent) {
			Control control = super.createButtonBar(parent);
			validate();
			return control;
		}

		protected void setValues(Control control) {
			if (control == null || control.isDisposed()) {
				return;
			}
			if (control == name) {
				envVar.setVariable(name.getText());
			}
			else if (control == value) {
				envVar.setValue(value.getText());
			}

			validate();
		}

		protected void validate() {
			Button okButton = getButton(IDialogConstants.OK_ID);
			if (okButton != null && !okButton.isDisposed()) {

				boolean isValid = !ValueValidationUtil.isEmpty(envVar.getValue())
						&& !ValueValidationUtil.isEmpty(envVar.getVariable());

				okButton.setEnabled(isValid);
			}
		}

		protected Control createDialogArea(Composite parent) {
			getShell().setText("Enter variable name and value");

			Composite control = (Composite) super.createDialogArea(parent);

			Composite composite = new Composite(control, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(composite);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);

			Label nameLabel = new Label(composite, SWT.NONE);
			nameLabel.setText("Name:");
			GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT)
					.applyTo(nameLabel);

			name = new Text(composite, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT)
					.applyTo(name);

			if (envVar != null && envVar.getVariable() != null) {
				name.setText(envVar.getVariable());
			}

			name.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					setValues(name);
				}
			});

			Label valueLabel = new Label(composite, SWT.NONE);
			valueLabel.setText("Value:");
			GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT)
					.applyTo(valueLabel);

			value = new Text(composite, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT)
					.applyTo(value);

			if (envVar != null && envVar.getValue() != null) {
				value.setText(envVar.getValue());
			}

			value.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					setValues(value);
				}
			});

			validate();

			return control;
		}

	}

}
