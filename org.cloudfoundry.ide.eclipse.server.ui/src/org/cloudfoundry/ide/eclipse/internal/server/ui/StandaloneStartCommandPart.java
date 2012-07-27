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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommandType;
import org.cloudfoundry.ide.eclipse.internal.server.ui.StandaloneUIDescriptor.ICommandChangeListener;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Creates the UI for specifying a start command for a standalone application.
 * If a descriptor for the standalone application contains a start command
 * definition, the values of the start command definition will be used to
 * populate the UI controls.
 * <p/>
 * If a start command is associated with multiple start command types, controls
 * for each start command type will be created, only if there is a corresponding
 * UI descriptor for the start command type. Users can toggle between the
 * different start command type UI.
 * <p/>
 * If no start command definition is found for the standalone descriptor, a
 * default start command UI will be created containing an editable text where a
 * full start command value can be specified.
 */
public class StandaloneStartCommandPart implements ICommandChangeListener {

	private final StandaloneDescriptor descriptor;

	private String standaloneStartCommand;

	private Map<StartCommandType, Composite> startCommandAreas = new HashMap<StartCommandType, Composite>();

	public StandaloneStartCommandPart(StandaloneDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public String getStandaloneStartCommand() {
		return standaloneStartCommand;
	}

	public Composite createPart(Composite parent) {
		createStandaloneSection(parent);
		return parent;
	}

	protected StartCommandType getDefaultStartCommandType(StandaloneDescriptor descriptor) {
		if (descriptor != null) {
			StartCommand startCommand = descriptor.getStartCommand();
			if (startCommand != null) {
				return startCommand.getDefaultStartCommandType();
			}
		}
		return null;
	}

	protected void createStandaloneSection(Composite parent) {

		List<StartCommandType> commandTypes = getStartCommandTypes();

		StandaloneUIDescriptor uiDescriptor = new StandaloneUIDescriptor(descriptor);
		boolean createdControls = false;

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
		label.setText("Start Command:");

		Composite startCommandArea = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(startCommandArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(startCommandArea);

		if (!commandTypes.isEmpty() && uiDescriptor.hasUIControl()) {
			createdControls = createStartCommandArea(commandTypes, uiDescriptor, startCommandArea);
		}

		// If no controls have been created, create a default start command
		// control.
		if (!createdControls) {
			uiDescriptor.createDefaultStartCommandControl(startCommandArea, this);
		}
	}

	/**
	 * 
	 * @param commandTypes
	 * @param uiDescriptor
	 * @param parent
	 * @return true if start command area was created. False otherwise.
	 */
	protected boolean createStartCommandArea(List<StartCommandType> commandTypes, StandaloneUIDescriptor uiDescriptor,
			Composite parent) {

		List<StartCommandType> createdUITypes = new ArrayList<StartCommandType>();

		for (StartCommandType commandType : commandTypes) {
			if (uiDescriptor.hasUIControl(commandType)) {
				createdUITypes.add(commandType);
			}
		}

		if (createdUITypes.isEmpty()) {
			return false;
		}

		int columnNumber = createdUITypes.size();

		Composite buttonSelectionArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(columnNumber).applyTo(buttonSelectionArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonSelectionArea);

		StartCommandType defaultStartCommandType = getDefaultStartCommandType(descriptor);

		// Create radio buttons for each start command type, which
		// allows users to
		// toggle between the different start command
		for (StartCommandType commandType : createdUITypes) {

			// If no default start command type was specified, make
			// the first one encountered
			// the default start command type
			if (defaultStartCommandType == null) {
				defaultStartCommandType = commandType;
			}
			final Button radio = new Button(buttonSelectionArea, SWT.RADIO);
			radio.setText(commandType.name());
			radio.setToolTipText(commandType.getDescription());
			radio.setData(commandType);

			boolean isSelected = commandType.equals(defaultStartCommandType);
			radio.setSelection(isSelected);

			radio.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					if (radio.getSelection()) {
						makeStartCommandControlsVisible((StartCommandType) radio.getData());
					}
				}
			});
		}

		// Create the start command type UI whose visibility is
		// controlled by the radio button
		Composite startCompositeArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(startCompositeArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(startCompositeArea);

		for (StartCommandType commandType : createdUITypes) {
			Composite commandComposite = uiDescriptor.createStartCommandControl(startCompositeArea, commandType, this);
			startCommandAreas.put(commandType, commandComposite);
		}

		// At this stage, at least one UI control has been created
		makeStartCommandControlsVisible(defaultStartCommandType);
		return true;
	}

	/**
	 * @return non-null list of start command types, or empty if none are
	 * defined for the given standalone descriptor
	 */
	protected List<StartCommandType> getStartCommandTypes() {

		if (descriptor.getStartCommand() == null) {
			return Collections.emptyList();
		}

		List<StartCommandType> commandTypes = descriptor.getStartCommand().getStartCommandTypes();

		if (commandTypes == null) {
			return Collections.emptyList();
		}

		return commandTypes;
	}

	protected void makeStartCommandControlsVisible(StartCommandType typeToMakeVisible) {
		Composite areaToMakeVisible = startCommandAreas.get(typeToMakeVisible);

		if (areaToMakeVisible != null && !areaToMakeVisible.isDisposed()) {

			GridData data = (GridData) areaToMakeVisible.getLayoutData();
			GridDataFactory.createFrom(data).exclude(false).applyTo(areaToMakeVisible);
			areaToMakeVisible.setVisible(true);

			// Hide the other sections
			// If hiding, exclude from layout as to not take up space when it is
			// made invisible
			for (StartCommandType otherTypes : startCommandAreas.keySet()) {
				if (!otherTypes.equals(typeToMakeVisible)) {
					Composite otherArea = startCommandAreas.get(otherTypes);

					if (!otherArea.isDisposed()) {
						data = (GridData) otherArea.getLayoutData();
						GridDataFactory.createFrom(data).exclude(true).applyTo(otherArea);

						otherArea.setVisible(false);
					}
				}
			}

			// Recalculate layout
			areaToMakeVisible.getParent().layout(true, true);
		}

	}

	public void handleChange(String command) {
		standaloneStartCommand = command;
	}

}
