/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
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
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandTerminal;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerServiceWithPredefinitions;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.TunnelOptions;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Adds a new command definition for a given service. Required values are a
 * command display name and the location of a command executable.
 * 
 * A check is performed to see if either one of the have valid, non-empty
 * values. In addition, when adding a new command, if a command already exists
 * for the given service with the same display name, and error is shown.
 * 
 * Display names must be unique.
 * 
 * Another command can be used to pre-populate the UI.
 * 
 */
public class AddCommandDisplayPart extends AbstractPart {

	private Text options;

	private Text locationField;

	private Text displayName;

	private Text terminalLocation;

	protected String optionsVal;

	protected String executableLocationValue;

	protected String displayNameVal;

	protected String terminalLocationVal;

	private Button findApplicationButton;

	private boolean applyTerminalToAllCommands = false;

	private Combo predefinedCommands;

	private final List<ServiceCommand> predefined;

	private ServerService service;

	private Shell shell;

	private ServiceCommand serviceCommand;

	private CommandTerminal defaultTerminal;

	/**
	 * 
	 * @param service
	 * @param serviceCommand . This is what is used to populate the wizard
	 */
	public AddCommandDisplayPart(ServerService service, ServiceCommand serviceCommand) {
		this(service, serviceCommand, null);

	}

	public AddCommandDisplayPart(ServerService service, CommandTerminal defaultTerminal) {
		this(service, null, defaultTerminal);
	}

	protected AddCommandDisplayPart(ServerService service, ServiceCommand serviceCommand,
			CommandTerminal defaultTerminal) {
		this.predefined = service instanceof ServerServiceWithPredefinitions ? ((ServerServiceWithPredefinitions) service)
				.getPredefinedCommands() : null;
		this.service = service;
		this.serviceCommand = serviceCommand;
		this.defaultTerminal = defaultTerminal;
	}

	public Control createPart(Composite parent) {

		if (shell == null) {
			shell = parent.getShell();
		}

		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		createPredefinedArea(main);

		/* Display name area */
		Label commandDisplayName = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(commandDisplayName);
		commandDisplayName.setText("Display Name:");

		displayName = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(displayName);

		displayName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		Label terminalLocationLabel = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(terminalLocationLabel);
		terminalLocationLabel.setText("External Command Line Terminal:");

		terminalLocation = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(terminalLocation);

		terminalLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		final Button terminalButton = new Button(main, SWT.CHECK);

		int padding = 20;
		GridDataFactory.fillDefaults().grab(false, false).indent(padding, SWT.DEFAULT).applyTo(terminalButton);
		terminalButton.setText("Apply terminal changes to all commands.");

		terminalButton.setSelection(applyTerminalToAllCommands);

		terminalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				applyTerminalToAllCommands = terminalButton.getSelection();
			}

		});

		Label fileSelectionLabel = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(fileSelectionLabel);
		fileSelectionLabel.setText("Enter or browse location of command executable:");

		locationField = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(locationField);

		Composite buttonArea = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(buttonArea);

		locationField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		findApplicationButton = new Button(buttonArea, SWT.PUSH);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(findApplicationButton);
		findApplicationButton.setText("Browse...");

		findApplicationButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event);
			}

		});

		Text argsLabel = new Text(main, SWT.MULTI);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(argsLabel);

		argsLabel
				.setText("Enter options below. \nUse ${variablename} for option values that should be prompted when the command is executed.");

		argsLabel.setBackground(main.getBackground());
		options = new Text(main, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		options.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_RETURN && (event.stateMask & SWT.MODIFIER_MASK) != 0) {
					event.doit = true;
				}
			}
		});

		options.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		GridDataFactory.fillDefaults().grab(true, true).hint(30, IDialogConstants.ENTRY_FIELD_WIDTH).applyTo(options);

		Text optionsDescription = new Text(main, SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(30, IDialogConstants.ENTRY_FIELD_WIDTH)
				.applyTo(optionsDescription);

		optionsDescription.setEditable(false);
		optionsDescription.setText(getOptionsDescription());

		optionsDescription.setBackground(main.getBackground());

		readValues();

		return main;

	}

	public boolean applyTerminalToAllCommands() {
		return applyTerminalToAllCommands;
	}

	protected void createPredefinedArea(Composite parent) {
		// See if any predefined commands are available are available
		if (predefined != null && !predefined.isEmpty()) {

			Label templates = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(templates);
			templates
					.setText("Select a pre-defined command. Note that the executable location may need to be changed.");

			Composite predefinedArea = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(predefinedArea);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(predefinedArea);

			predefinedCommands = new Combo(predefinedArea, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(predefinedCommands);
			predefinedCommands.setEnabled(true);
			predefinedCommands.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					int selectionIndex = predefinedCommands.getSelectionIndex();
					if (selectionIndex != -1) {

						// Account for the additional entry in the combo that
						// has no mapped entry in the predefined list
						int predefIndex = selectionIndex - 1;

						ServiceCommand value = predefIndex >= 0 && predefIndex < predefined.size() ? predefined
								.get(predefIndex) : null;
						setPredefinedCommand(value);
					}
				}
			});

			// Add a clear value to allow users to clear the predefined command
			predefinedCommands.add("Select:");

			for (ServiceCommand option : predefined) {
				predefinedCommands.add(option.getDisplayName());
			}

			predefinedCommands.select(0);

		}
	}

	protected void setPredefinedCommand(ServiceCommand predefinedCommand) {

		// If no predefined command is set, clear values
		if (predefinedCommand != null) {
			displayNameVal = predefinedCommand.getDisplayName();
			if (displayNameVal != null) {
				displayName.setText(displayNameVal);
			}

			optionsVal = predefinedCommand.getOptions() != null ? predefinedCommand.getOptions().getOptions() : null;
			if (optionsVal != null) {
				options.setText(optionsVal);
			}
			executableLocationValue = predefinedCommand.getExternalApplication() != null ? predefinedCommand
					.getExternalApplication().getExecutableNameAndPath() : null;
			if (executableLocationValue != null) {
				locationField.setText(executableLocationValue);
			}

		}
		else {

			// Clear the values
			displayName.setText("");
			options.setText("");
			locationField.setText("");
		}

		validate(true);
	}

	/**
	 * Sets the control values based on the set service command. The set service
	 * command can be either an existing command that is being edited or a
	 * template that requires further input from the user.
	 */
	protected void readValues() {
		if (serviceCommand != null) {
			executableLocationValue = serviceCommand.getExternalApplication() != null ? serviceCommand
					.getExternalApplication().getExecutableNameAndPath() : null;

			if (executableLocationValue != null) {
				locationField.setText(executableLocationValue);
			}

			displayNameVal = serviceCommand.getDisplayName();

			if (displayNameVal != null) {
				displayName.setText(displayNameVal);
			}

			optionsVal = serviceCommand.getOptions() != null ? serviceCommand.getOptions().getOptions() : null;

			if (optionsVal != null) {
				options.setText(optionsVal);
			}

			if (serviceCommand.getCommandTerminal() != null) {
				terminalLocationVal = serviceCommand.getCommandTerminal().getTerminal();

				if (terminalLocationVal != null) {
					terminalLocation.setText(terminalLocationVal);
				}
			}

		}
		else if (defaultTerminal != null) {
			terminalLocationVal = defaultTerminal.getTerminal();

			if (terminalLocationVal != null) {
				terminalLocation.setText(terminalLocationVal);
			}
		}
		validate(false);
	}

	public void updateValues(Control eventControl) {

		if (eventControl == null || eventControl.isDisposed()) {
			return;
		}

		if (eventControl == locationField) {
			executableLocationValue = locationField.getText();
		}
		else if (eventControl == displayName) {
			displayNameVal = displayName.getText();
		}
		else if (eventControl == options) {
			optionsVal = options.getText();
		}
		else if (eventControl == terminalLocation) {
			terminalLocationVal = terminalLocation.getText();
		}
		else if (eventControl == findApplicationButton) {
			handleFileLocationButtonSelected();
		}

	}

	public String getExecutableLocation() {
		return executableLocationValue;
	}

	public String getDisplayName() {
		return displayNameVal;
	}

	public String getOptions() {
		return optionsVal;
	}

	public String getTerminal() {
		return terminalLocationVal;
	}

	protected ServerService getService() {
		return service;
	}

	protected String getOptionsDescription() {
		StringWriter writer = new StringWriter();
		writer.append("Use the following variables for service tunnel options to be filled automatically:");
		writer.append("\n");
		writer.append("\n");
		writer.append("${");
		writer.append(TunnelOptions.user.name());
		writer.append("}");
		writer.append("\n");
		writer.append("${");
		writer.append(TunnelOptions.password.name());
		writer.append("}");
		writer.append("\n");
		writer.append("${");
		writer.append(TunnelOptions.url.name());
		writer.append("}");
		writer.append("\n");
		writer.append("${");
		writer.append(TunnelOptions.databasename.name());
		writer.append("}");
		writer.append("\n");
		writer.append("${");
		writer.append(TunnelOptions.port.name());
		writer.append("}");
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

		if (eventSource instanceof Control) {
			Control eventControl = (Control) eventSource;

			updateValues(eventControl);

			validate(true);
		}

	}

	/**
	 * Return a non-null message, If and only if there is an error. No errors
	 * should return null.
	 */
	protected String getValidationMessage() {
		String message = null;
		if (ValueValidationUtil.isEmpty(executableLocationValue)) {
			message = "No command executable location specified.";
		}
		else if (ValueValidationUtil.isEmpty(displayNameVal)) {
			message = "No command display name specified.";
		}
		else {

			// Verify that another command doesn't already exist
			List<ServiceCommand> existingCommands = getService().getCommands();
			if (existingCommands != null) {
				for (ServiceCommand command : existingCommands) {
					String otherCommandName = command.getDisplayName();
					if ((command != serviceCommand) && otherCommandName.equals(displayNameVal)) {
						message = "Another command with the same display name already exists. Please select another display name.";
						break;
					}
				}
			}

		}
		return message;
	}

	protected void validate(boolean setErrorMessage) {
		String message = getValidationMessage();

		IStatus status = null;
		if (message != null) {
			// If it is an intial validation, generate an error status so
			// listeners can update controls accordingly (e.g.,
			// if invoking this part in a wizard, the "Finish" button is
			// disabled), but set the message to null as to not
			// display the error to the user until a user enters data
			status = CloudFoundryPlugin.getErrorStatus(setErrorMessage ? message : null);
		}
		else {
			status = Status.OK_STATUS;
		}
		setStatus(status);
	}

}
