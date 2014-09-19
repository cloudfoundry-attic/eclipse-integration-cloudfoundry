/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import java.util.EventObject;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CommandOptions;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.CommandTerminal;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServerServiceWithPredefinitions;
import org.cloudfoundry.ide.eclipse.server.core.internal.tunnel.ServiceCommand;
import org.cloudfoundry.ide.eclipse.server.ui.internal.EnvironmentVariablesPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UIPart;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
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
public class AddCommandDisplayPart extends UIPart {

	private Text options;

	private Text locationField;

	private Text displayName;

	private Text terminalLocation;

	protected String optionsVal;

	protected String executableLocationValue;

	protected String displayNameVal;

	protected String terminalLocationVal;

	protected List<EnvironmentVariable> envVariables;

	private Button findApplicationButton;

	private boolean applyTerminalToAllCommands = false;

	private Combo predefinedCommands;

	private final List<ServiceCommand> predefined;

	private ServerService service;

	private Shell shell;

	private ServiceCommand serviceCommand;

	private CommandTerminal defaultTerminal;

	private EnvironmentVariablesPart envVarPart;

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

		createAppLocationArea(main);

		createTerminalLocationArea(main);

		createOptionsArea(main);

		createEnvVariablesArea(main);

		createTunnelVariablesArea(main);

		readValues();

		return main;

	}

	protected Composite createGroupComposite(Composite parent, String groupName) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(groupName);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		Composite groupComp = new Composite(group, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(new Point(10, 10)).applyTo(groupComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupComp);

		return groupComp;
	}

	protected void createEnvVariablesArea(Composite parent) {

		parent = createGroupComposite(parent, Messages.COMMONTXT_ENV_VAR);

		envVarPart = new EnvironmentVariablesPart();
		envVarPart.addPartChangeListener(new IPartChangeListener() {

			public void handleChange(PartChangeEvent event) {
				envVariables = envVarPart.getVariables();
				// No need to validate as environment variables are optional
			}
		});
		envVarPart.createPart(parent);
	}

	protected void createTerminalLocationArea(Composite parent) {

		parent = createGroupComposite(parent, Messages.AddCommandDisplayPart_TITLE_EXTERNAL_CMDLINE);

		terminalLocation = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(terminalLocation);

		terminalLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		final Button terminalButton = new Button(parent, SWT.CHECK);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(terminalButton);
		terminalButton.setText(Messages.AddCommandDisplayPart_BUTTON_APPLY_CHANGE);

		terminalButton.setSelection(applyTerminalToAllCommands);

		terminalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				applyTerminalToAllCommands = terminalButton.getSelection();
			}

		});
	}

	protected void createAppLocationArea(Composite parent) {

		// parent = createGroupComposite(parent, "Application");

		/* Display name area */
		Label commandDisplayName = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(commandDisplayName);
		commandDisplayName.setText(Messages.AddCommandDisplayPart_LABEL_DISPLAY_NAME);

		displayName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(displayName);

		displayName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		Label fileSelectionLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(fileSelectionLabel);
		fileSelectionLabel.setText(Messages.AddCommandDisplayPart_LABEL_LOCATION_ENTER);

		locationField = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(locationField);

		Composite buttonArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(buttonArea);

		locationField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				handleChange(event);
			}
		});

		findApplicationButton = new Button(buttonArea, SWT.PUSH);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(findApplicationButton);
		findApplicationButton.setText(Messages.AddCommandDisplayPart_BUTTON_BROWSE);

		findApplicationButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				handleChange(event);
			}

		});
	}

	protected void createOptionsArea(Composite parent) {

		parent = createGroupComposite(parent, Messages.AddCommandDisplayPart_TITLE_APP_OPTION);
		Label optionsLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(optionsLabel);

		optionsLabel
				.setText(Messages.AddCommandDisplayPart_LABEL_SET_VAR);

		options = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
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

		GridDataFactory.fillDefaults().grab(true, false).hint(IDialogConstants.ENTRY_FIELD_WIDTH, 80).applyTo(options);

	}

	protected void createTunnelVariablesArea(Composite parent) {
		Text optionsDescription = new Text(parent, SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(IDialogConstants.ENTRY_FIELD_WIDTH, 120)
				.applyTo(optionsDescription);

		optionsDescription.setEditable(false);
		optionsDescription.setText(getOptionsDescription());

		optionsDescription.setBackground(parent.getBackground());
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
					.setText(Messages.AddCommandDisplayPart_LABEL_SET_PREDEF_CMD);

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
			predefinedCommands.add(Messages.AddCommandDisplayPart_TEXT_SELECT);

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
			displayName.setText(displayNameVal != null ? displayNameVal : ""); //$NON-NLS-1$

			optionsVal = predefinedCommand.getOptions() != null ? predefinedCommand.getOptions().getOptions() : null;
			options.setText(optionsVal != null ? optionsVal : ""); //$NON-NLS-1$

			executableLocationValue = predefinedCommand.getExternalApplication() != null ? predefinedCommand
					.getExternalApplication().getExecutableNameAndPath() : null;

			locationField.setText(executableLocationValue != null ? executableLocationValue : ""); //$NON-NLS-1$

			envVariables = predefinedCommand.getEnvironmentVariables();

			envVarPart.setInput(envVariables);

		}
		else {

			// Clear the values
			displayName.setText(""); //$NON-NLS-1$
			options.setText(""); //$NON-NLS-1$
			locationField.setText(""); //$NON-NLS-1$
			envVarPart.setInput(null);
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

			envVariables = serviceCommand.getEnvironmentVariables();
			envVarPart.setInput(envVariables);

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

	public List<EnvironmentVariable> getEnvironmentVariables() {
		return envVariables;
	}

	protected String getOptionsDescription() {
		return CommandOptions.getDefaultTunnelOptionsDescription();
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
			message = Messages.AddCommandDisplayPart_TEXT_NO_EXECUTABLE;
		}
		else if (ValueValidationUtil.isEmpty(displayNameVal)) {
			message = Messages.AddCommandDisplayPart_TEXT_NO_DISPLAY_NAME;
		}
		else {

			// Verify that another command doesn't already exist
			List<ServiceCommand> existingCommands = getService().getCommands();
			if (existingCommands != null) {
				for (ServiceCommand command : existingCommands) {
					String otherCommandName = command.getDisplayName();
					if ((command != serviceCommand) && otherCommandName.equals(displayNameVal)) {
						message = Messages.AddCommandDisplayPart_TEXT_NAME_EXIST;
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
		notifyStatusChange(status);
	}

}
