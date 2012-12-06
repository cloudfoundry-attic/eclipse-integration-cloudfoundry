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
import java.util.EventObject;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

public class CommandDisplayPart extends AbstractPart {

	private Text options;

	private Text locationField;

	private Text displayName;

	private String optionsVal;

	private String locationVal;

	private String displayNameVal;

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

		Composite fileSelection = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(fileSelection);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fileSelection);
		
		
		/* Display name area */
		Composite displayComp = new Composite(fileSelection, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(displayComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(displayComp);

		Label commandDisplayName = new Label(displayComp, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(commandDisplayName);
		commandDisplayName.setText("Display Name:");

		displayName = new Text(displayComp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 0).applyTo(displayName);

	
		
		Label fileSelectionLabel = new Label(fileSelection, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(fileSelectionLabel);
		fileSelectionLabel.setText("Enter or browse location of command executable:");

		/* Executable location */
		Composite locationComp = new Composite(fileSelection, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(locationComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(locationComp);
		
		locationField = new Text(locationComp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(locationField);
	

		findApplicationButton = new Button(locationComp, SWT.PUSH);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(findApplicationButton);
		findApplicationButton.setText("Browse...");

		findApplicationButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event);
			}

		});

		displayName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		locationField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		Label argsLabel = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(argsLabel);
		argsLabel.setText("Enter options:");

		options = new Text(main, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		options.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_RETURN && (event.stateMask & SWT.MODIFIER_MASK) != 0) {
					event.doit = true;
				}
			}
		});

		GridDataFactory.fillDefaults().grab(true, true).hint(30, IDialogConstants.ENTRY_FIELD_WIDTH).applyTo(options);

		Text optionsDescription = new Text(main, SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(30, IDialogConstants.ENTRY_FIELD_WIDTH)
				.applyTo(optionsDescription);

		optionsDescription.setEditable(false);
		optionsDescription.setText(getOptionsDescription());

		setInitialValues();

		// Initial Validation.
		validate(true);

		return main;

	}

	protected void setInitialValues() {
		if (serviceCommand != null) {
			locationVal = serviceCommand.getExternalApplicationLaunchInfo().getExecutableName();
			if (locationVal != null) {
				locationField.setText(locationVal);
			}
			displayNameVal = serviceCommand.getExternalApplicationLaunchInfo().getDisplayName();

			if (displayNameVal != null) {
				displayName.setText(displayNameVal);
			}

			if (serviceCommand.getOptions() != null) {
				optionsVal = serviceCommand.getOptions().getOptions();
				options.setText(optionsVal);
			}
		}
	}

	public String getDisplayName() {
		return displayNameVal;
	}

	public String getLocation() {
		return locationVal;
	}

	public String getOptions() {
		return optionsVal;
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

	protected void handleChange(EventObject event) {
		Object eventSource = event.getSource();
		if (eventSource == findApplicationButton) {
			handleFileLocationButtonSelected();
		}
		locationVal = locationField.getText();
		displayNameVal = displayName.getText();
		optionsVal = options.getText();

		validate(false);
	}

	protected void validate(boolean initialValidate) {
		String message = null;
		if (ValueValidationUtil.isEmpty(locationVal)) {
			message = "No command executable location specified.";
		}
		else if (ValueValidationUtil.isEmpty(displayNameVal)) {
			message = "No command display name specified.";
		}

		IStatus status = null;
		if (message != null) {
			// If it is an intial validation, generate an error status so
			// listeners can update controls accordingly (e.g.,
			// if invoking this part in a wizard, the "Finish" button is
			// disabled), but set the message to null as to not
			// display the error to the user until a user enters data
			status = CloudFoundryPlugin.getErrorStatus(initialValidate ? null : message);
		}
		else {
			status = Status.OK_STATUS;
		}
		setStatus(status);
	}

}
