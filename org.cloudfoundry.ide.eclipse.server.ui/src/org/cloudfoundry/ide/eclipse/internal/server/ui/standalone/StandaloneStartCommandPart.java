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
package org.cloudfoundry.ide.eclipse.internal.server.ui.standalone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommandType;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.IStartCommandChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.IStartCommandPartListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.StartCommandEvent;
import org.eclipse.core.resources.IProject;
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
public class StandaloneStartCommandPart implements IStartCommandPartListener {

	private final StartCommand startCommand;

	private String standaloneStartCommand;

	private boolean isStartCommandValid = false;

	private Map<StartCommandType, StartCommandPart> startCommandAreas = new HashMap<StartCommandType, StartCommandPart>();

	private final IStartCommandChangeListener listener;

	private final IProject project;

	public StandaloneStartCommandPart(StartCommand startCommand, IStartCommandChangeListener listener, IProject project) {
		this.startCommand = startCommand;
		this.listener = listener;
		this.project = project;
	}

	public String getStandaloneStartCommand() {
		return standaloneStartCommand;
	}

	public Composite createPart(Composite parent) {
		createStandaloneSection(parent);
		return parent;
	}

	public boolean isStartCommandValid() {
		return isStartCommandValid;
	}

	protected void createStandaloneSection(Composite parent) {

		List<StartCommandType> commandTypes = startCommand.getStartCommandTypes();

		StartCommandPartFactory partFactory = new StartCommandPartFactory(startCommand, project);
		boolean createdControls = false;

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
		label.setText("Start Command:");

		Composite startCommandArea = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(startCommandArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(startCommandArea);

		if (!commandTypes.isEmpty()) {
			createdControls = createStartCommandArea(commandTypes, partFactory, startCommandArea);
		}

		// If no controls have been created, create a default start command
		// control.
		if (!createdControls) {
			partFactory.createStartCommandTypePart(StartCommandType.Other, startCommandArea, this);
		}
	}

	/**
	 * 
	 * @param commandTypes
	 * @param partFactory
	 * @param parent
	 * @return true if start command area was created. False otherwise.
	 */
	protected boolean createStartCommandArea(List<StartCommandType> commandTypes, StartCommandPartFactory partFactory,
			Composite parent) {

		if (commandTypes.isEmpty()) {
			return false;
		}

		int columnNumber = commandTypes.size();

		Composite buttonSelectionArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(columnNumber).applyTo(buttonSelectionArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonSelectionArea);

		StartCommandType defaultStartCommandType = startCommand.getDefaultStartCommandType();

		// Create radio buttons for each start command type, which
		// allows users to
		// toggle between the different start command
		for (StartCommandType commandType : commandTypes) {

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
						StartCommandType type = (StartCommandType) radio.getData();

						makeStartCommandControlsVisible(type);
						StartCommandPart part = startCommandAreas.get(type);
						if (part != null) {
							part.updateStartCommand(StartCommandEvent.UPDATE);
						}
					}
				}
			});
		}

		// Create the start command type UI whose visibility is
		// controlled by the radio button
		Composite startCompositeArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(startCompositeArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(startCompositeArea);

		for (StartCommandType commandType : commandTypes) {
			StartCommandPart commandPart = partFactory
					.createStartCommandTypePart(commandType, startCompositeArea, this);
			if (commandPart != null) {
				startCommandAreas.put(commandType, commandPart);
			}
		}

		// At this stage, at least one UI control has been created
		makeStartCommandControlsVisible(defaultStartCommandType);
		return true;
	}

	protected void makeStartCommandControlsVisible(StartCommandType typeToMakeVisible) {
		StartCommandPart part = startCommandAreas.get(typeToMakeVisible);
		Composite areaToMakeVisible = part != null ? part.getComposite() : null;

		if (areaToMakeVisible != null && !areaToMakeVisible.isDisposed()) {

			GridData data = (GridData) areaToMakeVisible.getLayoutData();
			GridDataFactory.createFrom(data).exclude(false).applyTo(areaToMakeVisible);
			areaToMakeVisible.setVisible(true);

			// Hide the other sections
			// If hiding, exclude from layout as to not take up space when it is
			// made invisible
			for (StartCommandType otherTypes : startCommandAreas.keySet()) {
				if (!otherTypes.equals(typeToMakeVisible)) {
					StartCommandPart otherArea = startCommandAreas.get(otherTypes);

					if (otherArea != null) {
						Composite otherAreaComposite = otherArea.getComposite();

						if (!otherAreaComposite.isDisposed()) {
							data = (GridData) otherAreaComposite.getLayoutData();
							GridDataFactory.createFrom(data).exclude(true).applyTo(otherAreaComposite);

							otherAreaComposite.setVisible(false);
						}
					}
				}
			}

			// Recalculate layout
			areaToMakeVisible.getParent().layout(true, true);
		}

	}

	public void handleChange(String command, boolean isValid) {
		standaloneStartCommand = command;
		isStartCommandValid = isValid;
		if (listener != null) {
			listener.handleEvent(StartCommandEvent.UPDATE);
		}
	}

}
