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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.startcommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
 * full start command value can be specified
 */
/*
 * IMPLEMENTATION NOTE: The Standalone command part is actually a container for
 * multiple subparts, each subpart corresponding to a start command type. At any
 * given time, only one subpart is ever visible. As a listener like a wizard
 * page may be registered to listen for start command changes, the standalone
 * container part redirects any events from the subparts as having originated
 * from the container. The reason for this is to prevent the listener from
 * keeping track of errors that originated from non-visible subparts. For
 * example, if a visible subpart had errors, and a user switches to another
 * subpart that does not have errors, the errors from the now-hidden initial
 * subpart should not matter anymore. Therefore, the listener should not be
 * listening to changes in all sub-parts, but only the sub-part that is actually
 * visible. Thus the reason why the container is in charge of managing events
 * coming from the subparts.
 */
public class StandaloneStartCommandPart extends UIPart implements
		IPartChangeListener {

	private final StartCommand startCommand;

	private Map<StartCommandType, StartCommandPart> startCommandAreas = new HashMap<StartCommandType, StartCommandPart>();

	private final IProject project;

	public StandaloneStartCommandPart(StartCommand startCommand,
			IProject project) {
		this.startCommand = startCommand;

		this.project = project;
	}

	public Composite createPart(Composite parent) {
		createStandaloneSection(parent);
		return parent;
	}

	protected void createStandaloneSection(Composite parent) {

		List<StartCommandType> commandTypes = startCommand
				.getStartCommandTypes();

		StartCommandPartFactory partFactory = new StartCommandPartFactory(
				startCommand, project);
		boolean createdControls = false;

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
		label.setText("Start Command:");

		Composite startCommandArea = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0)
				.applyTo(startCommandArea);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(startCommandArea);

		if (!commandTypes.isEmpty()) {
			createdControls = createStartCommandArea(commandTypes, partFactory,
					startCommandArea);
		}

		// If no controls have been created, create a default start command
		// control.
		if (!createdControls) {
			partFactory.createStartCommandTypePart(StartCommandType.Other,
					startCommandArea);
		}
	}

	/**
	 * 
	 * @param commandTypes
	 * @param partFactory
	 * @param parent
	 * @return true if start command area was created. False otherwise.
	 */
	protected boolean createStartCommandArea(
			List<StartCommandType> commandTypes,
			StartCommandPartFactory partFactory, Composite parent) {

		if (commandTypes.isEmpty()) {
			return false;
		}

		int columnNumber = commandTypes.size();

		Composite buttonSelectionArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(columnNumber)
				.applyTo(buttonSelectionArea);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(buttonSelectionArea);

		StartCommandType defaultStartCommandType = startCommand
				.getDefaultStartCommandType();

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
						StartCommandType type = (StartCommandType) radio
								.getData();

						makeStartCommandControlsVisible(type);
						StartCommandPart part = startCommandAreas.get(type);
						if (part != null) {
							part.updateStartCommand();
						}
					}
				}
			});
		}

		// Create the start command type UI whose visibility is
		// controlled by the radio button
		Composite startCompositeArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0)
				.applyTo(startCompositeArea);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(startCompositeArea);

		for (StartCommandType commandType : commandTypes) {
			StartCommandPart commandPart = partFactory
					.createStartCommandTypePart(commandType, startCompositeArea);
			if (commandPart != null) {

				// Since the standalone part is a container of multiple
				// subparts, with only one subpart
				// ever visible at any given time, make sure the listener of any
				// part changes
				// (e.g, a wizard page) only receives ONE event originating from
				// the
				// container part, not from the individual
				// subparts. This is to avoid the listener from keeping track of
				// errors from non-visible subparts, as errors from non-visible
				// parts should
				// not prevent the completion of an operation, like deploying an
				// application.
				commandPart.addPartChangeListener(this);
				startCommandAreas.put(commandType, commandPart);
			}
		}

		// At this stage, at least one UI control has been created
		makeStartCommandControlsVisible(defaultStartCommandType);
		return true;
	}

	protected void makeStartCommandControlsVisible(
			StartCommandType typeToMakeVisible) {
		StartCommandPart part = startCommandAreas.get(typeToMakeVisible);
		Control areaToMakeVisible = part != null ? part.getComposite() : null;

		if (areaToMakeVisible != null && !areaToMakeVisible.isDisposed()) {

			GridData data = (GridData) areaToMakeVisible.getLayoutData();
			GridDataFactory.createFrom(data).exclude(false)
					.applyTo(areaToMakeVisible);
			areaToMakeVisible.setVisible(true);

			// Hide the other sections
			// If hiding, exclude from layout as to not take up space when it is
			// made invisible
			for (StartCommandType otherTypes : startCommandAreas.keySet()) {
				if (!otherTypes.equals(typeToMakeVisible)) {
					StartCommandPart otherArea = startCommandAreas
							.get(otherTypes);

					if (otherArea != null) {
						Control otherAreaComposite = otherArea.getComposite();

						if (!otherAreaComposite.isDisposed()) {
							data = (GridData) otherAreaComposite
									.getLayoutData();
							GridDataFactory.createFrom(data).exclude(true)
									.applyTo(otherAreaComposite);

							otherAreaComposite.setVisible(false);
						}
					}
				}
			}

			// Recalculate layout
			areaToMakeVisible.getParent().layout(true, true);
		}

	}

	public void handleChange(PartChangeEvent event) {
		// The events received here are coming from the subparts. Redirect the
		// event to the actual listener (e.g. wizard page) as an event
		// originating from the container part, rather than the subpart.
		notifyStatusChange(event.getData(), event.getStatus());

	}
}
