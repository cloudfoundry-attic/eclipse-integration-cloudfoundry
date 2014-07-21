/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.CloudFoundryServiceWizard;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

public class EnvironmentVariablesPart extends UIPart {

	private List<EnvironmentVariable> variables;

	private TableViewer envVariablesViewer;
	
	private Action editEnvVarAction;

	private Action removeEnvVarAction;
	
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
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);
	    
		Table table = new Table(tableArea, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().hint(new Point(SWT.DEFAULT, 80)).grab(true, true).applyTo(table);
		envVariablesViewer = new TableViewer(table);
		Listener actionEnabler =  new Listener() {
			 
			@Override
			 public void handleEvent(Event event) {
			     removeEnvVarAction.setEnabled(isDeleteEnabled());
			     editEnvVarAction.setEnabled(isEditEnabled());
			  }
			 }; 
			
		table.addListener(SWT.Selection, actionEnabler);
		table.addListener(SWT.FocusOut, actionEnabler);
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

		new TableResizeHelper(envVariablesViewer).enableResizing();

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

		return tableArea;
	}

	protected enum ViewerAction {
		Add, Delete, Edit
	}

	private boolean isEditEnabled() {
		final List<EnvironmentVariable> vars = getViewerSelection();
		boolean isEnabled =  vars != null && vars.size() ==1;
		return isEnabled;
	}
	
	private boolean isDeleteEnabled() {
		final List<EnvironmentVariable> vars = getViewerSelection();
		boolean isEnabled =  vars != null && vars.size() > 0;
		return isEnabled;	
	}
	
	protected void handleAdd() {
		Shell shell = CloudUiUtil.getShell();
		if (shell != null) {
			VariableDialogue dialogue = new VariableDialogue(shell, null);
			if (dialogue.open() == Window.OK) {
				updateVariables(dialogue.getEnvironmentVariable(), null);
			}
		}
	}

	protected void handleEdit() {
		Shell shell = CloudUiUtil.getShell();
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
			GridDataFactory.fillDefaults().grab(false, false).applyTo(composite);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);

			Label nameLabel = new Label(composite, SWT.NONE);
			nameLabel.setText("Name:");
			GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT)
					.applyTo(nameLabel);

			name = new Text(composite, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT)
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