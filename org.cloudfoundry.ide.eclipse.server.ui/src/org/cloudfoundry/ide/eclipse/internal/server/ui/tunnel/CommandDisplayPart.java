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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.io.StringWriter;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CommandDisplayPart {

	private Text options;

	private Text locationField;

	private Text displayName;

	private Button findApplicationButton;

	private Shell shell;

	private ServiceCommand serviceCommand;

	public CommandDisplayPart(ServiceCommand serviceCommand) {
		this.serviceCommand = serviceCommand;
	}

	public Control createPart(Composite parent) {

		if (shell == null) {
			shell = parent.getShell();
		}

		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label fileSelectionLabel = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(main);
		fileSelectionLabel.setText("Display Name:");

		displayName = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(displayName);

		Composite fileSelection = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fileSelection);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fileSelection);

		locationField = new Text(fileSelection, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(locationField);

		if (serviceCommand != null) {
			locationField.setText(serviceCommand.getExternalApplicationLaunchInfo().getExecutableName());
		}

		findApplicationButton = new Button(fileSelection, SWT.PUSH);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(findApplicationButton);
		findApplicationButton.setText("Browse...");

		findApplicationButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event);
			}

		});

		Label argsLabel = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(argsLabel);
		argsLabel.setText("Enter options:");

		options = new Text(main, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		options.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_RETURN && (event.stateMask & SWT.MODIFIER_MASK) != 0) {
					event.doit = true;
				}
			}
		});

		if (serviceCommand != null && serviceCommand.getOptions() != null) {
			options.setText(serviceCommand.getOptions().getOptions());
		}

		GridDataFactory.fillDefaults().grab(true, true).hint(30, IDialogConstants.ENTRY_FIELD_WIDTH).applyTo(options);

		Text optionsDescription = new Text(main, SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(30, IDialogConstants.ENTRY_FIELD_WIDTH)
				.applyTo(optionsDescription);

		optionsDescription.setEditable(false);
		optionsDescription.setText(getOptionsDescription());

		return main;

	}

	public String getDisplayName() {
		return displayName != null ? displayName.getText() : null;
	}

	public String getLocation() {
		return locationField != null ? locationField.getText() : null;
	}

	public String getOptions() {
		return options != null ? options.getText() : null;
	}

	protected String getOptionsDescription() {
		StringWriter writer = new StringWriter();
		writer.append("Use the following variables for options to be automatically filled:");
		writer.append("\n");
		writer.append("\n");
		writer.append("$Username");
		writer.append("\n");
		writer.append("$Password");
		writer.append("\n");
		writer.append("$Url");
		writer.append("\n");
		writer.append("$Databasename");
		writer.append("\n");
		writer.append("$Port");
		return writer.toString();

	}

	protected void handleFileLocationButtonSelected() {
		if (shell != null && locationField != null) {
			FileDialog fileDialog = new FileDialog(shell, SWT.NONE);
			fileDialog.setFileName(locationField.getText());
			String text = fileDialog.open();
			if (text != null) {
				locationField.setText(text);
			}
		}

	}

	protected void handleChange(SelectionEvent event) {
		Object eventSource = event.getSource();
		if (eventSource == findApplicationButton) {
			handleFileLocationButtonSelected();
		}

	}

}
