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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class EnvironmentVariablesPart extends UIPart {

	private List<EnvironmentVariable> variables;

	private TableViewer envVariablesViewer;

	private Button editEnvVarButton;

	private Button removeEnvVarButton;

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
		Composite commonArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(commonArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(commonArea);

		Composite tableParent = new Composite(commonArea, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableParent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableParent);

		Table table = new Table(tableParent, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		envVariablesViewer = new TableViewer(table);
		Listener actionEnabler = new Listener() {
			@Override
			public void handleEvent(Event event) {
				setEnabledDisabled();
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

		addEditButtons(commonArea);
		return commonArea;
	}

	protected void setEnabledDisabled() {
		removeEnvVarButton.setEnabled(isDeleteEnabled());
		editEnvVarButton.setEnabled(isEditEnabled());
	}

	private void addEditButtons(Composite parent) {

		Composite buttonArea = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(1).spacing(SWT.DEFAULT, 4).applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonArea);

		Button newEnvVarButton = new Button(buttonArea, SWT.NONE);
		newEnvVarButton.setText(Messages.EnvironmentVariablesPart_TEXT_NEW_ENV_VAR);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(newEnvVarButton);
		newEnvVarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		editEnvVarButton = new Button(buttonArea, SWT.NONE);
		editEnvVarButton.setText(Messages.COMMONTXT_EDIT);
		editEnvVarButton.setEnabled(false);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(editEnvVarButton);
		editEnvVarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});

		removeEnvVarButton = new Button(buttonArea, SWT.NONE);
		removeEnvVarButton.setText(Messages.COMMONTXT_REMOVE);
		removeEnvVarButton.setEnabled(false);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(removeEnvVarButton);
		removeEnvVarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDelete();
			}
		});
	}

	private boolean isEditEnabled() {
		final List<EnvironmentVariable> vars = getViewerSelection();
		boolean isEnabled = vars != null && vars.size() == 1;
		return isEnabled;
	}

	private boolean isDeleteEnabled() {
		final List<EnvironmentVariable> vars = getViewerSelection();
		boolean isEnabled = vars != null && vars.size() > 0;
		return isEnabled;
	}

	protected void handleAdd() {
		boolean variableChanged = false;
		Shell shell = CloudUiUtil.getShell();
		if (shell != null) {
			VariableDialogue dialogue = new VariableDialogue(shell, null);
			if (dialogue.open() == Window.OK) {
				variableChanged = updateVariables(dialogue.getEnvironmentVariable(), null);
			}
		}

		if (variableChanged) {
			notifyStatusChange(Status.OK_STATUS);
		}
	}

	protected void handleEdit() {
		boolean variableChanged = false;
		Shell shell = CloudUiUtil.getShell();
		List<EnvironmentVariable> selection = getViewerSelection();
		if (shell != null && selection != null && !selection.isEmpty()) {
			EnvironmentVariable toEdit = selection.get(0);
			VariableDialogue dialogue = new VariableDialogue(shell, toEdit);
			if (dialogue.open() == Window.OK) {
				variableChanged = updateVariables(dialogue.getEnvironmentVariable(), toEdit);
			}
		}

		if (variableChanged) {
			notifyStatusChange(Status.OK_STATUS);
			setEnabledDisabled();
		}
	}

	protected void handleDelete() {
		boolean variableChanged = false;
		List<EnvironmentVariable> selection = getViewerSelection();
		if (selection != null && !selection.isEmpty()) {
			for (EnvironmentVariable toDelete : selection) {
				variableChanged = variableChanged || updateVariables(null, toDelete);
			}
		}

		if (variableChanged) {
			notifyStatusChange(Status.OK_STATUS);
			setEnabledDisabled();
		}
	}

	protected boolean updateVariables(EnvironmentVariable add, EnvironmentVariable delete) {
		boolean variableChanged = false;
		if (variables == null) {
			variables = new ArrayList<EnvironmentVariable>();
		}

		if (delete != null) {
			List<EnvironmentVariable> updatedList = new ArrayList<EnvironmentVariable>();

			for (EnvironmentVariable var : variables) {
				if (var.equals(delete)) {
					variableChanged = true;
				}
				else {
					updatedList.add(var);
				}
			}

			variables = updatedList;
		}

		if (add != null) {
			variables.add(add);
			variableChanged = true;
		}

		setInput(variables);
		return variableChanged;
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
			getShell().setText(Messages.EnvironmentVariablesPart_TEXT_DIALOG_TITLE);

			Composite control = (Composite) super.createDialogArea(parent);

			Composite composite = new Composite(control, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(composite);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);

			Label nameLabel = new Label(composite, SWT.NONE);
			nameLabel.setText(Messages.COMMONTXT_NAME_WITH_COLON);
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
			valueLabel.setText(Messages.EnvironmentVariablesPart_TEXT_VALUE_LABEL);
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