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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommandType;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Creates the UI controls for a standalone start command, given a standalone
 * descriptor. UI Controls are generated per start command type, if multiple
 * start command types exist per start command definition.
 */
public class StandaloneUIDescriptor {

	private final StandaloneDescriptor standaloneDescriptor;

	public StandaloneUIDescriptor(StandaloneDescriptor standaloneDescriptor) {
		this.standaloneDescriptor = standaloneDescriptor;
	}

	/**
	 * Determines if for the given standalone descriptor, at least one of the
	 * corresponding start command types has related UI. Is this returns true,
	 * it means that the
	 * @param parent
	 * @param type
	 * @param listener
	 * @return
	 */
	public Composite createStartCommandControl(Composite parent, StartCommandType type, ICommandChangeListener listener) {
		CommandTypeUIPart part = getStartCommandTypePart(type);
		if (part != null) {
			return part.createComposite(parent, listener);
		}
		return null;
	}

	/**
	 * Returns a non-null UI Control for a default start command, in case the
	 * standalone descriptor does not provide a start command definition.
	 * @param parent
	 * @param listener
	 * @return
	 */
	public Composite createDefaultStartCommandControl(Composite parent, ICommandChangeListener listener) {
		return getDefaultStartCommandUIPart().createComposite(parent, listener);
	}

	protected CommandTypeUIPart getStartCommandTypePart(StartCommandType type) {
		CommandTypeUIPart commandTypePart = null;
		switch (type) {
		case Java:
			commandTypePart = getJavaStartArea();
			break;
		case Other:
			commandTypePart = getOtherStartArea();
			break;
		}
		return commandTypePart;
	}

	/**
	 * Returns true if the related standalone descriptor has a start command
	 * with a type that has UI controls. Implicitly this means that the
	 * standalone descriptor has a defined start command and at least one start
	 * command type.
	 * @return true if at least one start command type is defined in the given
	 * standalone descriptor, and it has an associated UI control. False
	 * otherwise
	 */
	public boolean hasUIControl() {
		StartCommand startCommand = standaloneDescriptor.getStartCommand();
		if (startCommand != null) {
			List<StartCommandType> types = startCommand.getStartCommandTypes();

			if (types != null) {
				for (StartCommandType type : types) {
					if (hasUIControl(type)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * @param commandType
	 * @return true is UI controls are defined for the given start command type.
	 * False otherwise.
	 */
	public boolean hasUIControl(StartCommandType commandType) {
		return getStartCommandTypePart(commandType) != null;
	}

	protected CommandTypeUIPart getDefaultStartCommandUIPart() {
		return new CommandTypeUIPart() {

			private Text standaloneStartText;

			public Composite createComposite(Composite parent, ICommandChangeListener listener) {
				Composite composite = new Composite(parent, SWT.NONE);

				GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).margins(new Point(0, 7))
						.applyTo(composite);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

				standaloneStartText = createdEditableText(composite);

				standaloneStartText.addModifyListener(new ModifyListener() {

					public void modifyText(ModifyEvent e) {
						// standaloneStartCommand =
						// standaloneStartText.getText();
						// update();
					}

				});

				return composite;
			}
		};
	}

	protected CommandTypeUIPart getOtherStartArea() {
		return getDefaultStartCommandUIPart();
	}

	protected Label createdLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(label);
		if (text != null) {
			label.setText(text);
		}
		return label;
	}

	protected Text createdEditableText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(text);

		text.setEditable(true);
		return text;
	}

	protected Composite create2ColumnComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(new Point(0, 0)).spacing(5, 2)
				.applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
		return composite;
	}

	protected CommandTypeUIPart getJavaStartArea() {
		return new CommandTypeUIPart() {

			private Text mainTypeText;

			private Text javaOptions;

			public Composite createComposite(Composite parent, ICommandChangeListener listener) {
				Composite javaStartArea = new Composite(parent, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(javaStartArea);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(javaStartArea);

				Composite mainTypeArea = create2ColumnComposite(javaStartArea);

				createdLabel(mainTypeArea, "Main Type:");

				Composite typeArea = create2ColumnComposite(mainTypeArea);

				mainTypeText = createdEditableText(typeArea);

				Button browseButton = new Button(typeArea, SWT.PUSH);
				browseButton.setText("Browse...");
				GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(browseButton);

				createdLabel(mainTypeArea, "Options:");

				javaOptions = createdEditableText(mainTypeArea);

				StartCommand.JavaStartCommand javaStartCommand = (StartCommand.JavaStartCommand) standaloneDescriptor
						.getStartCommand();

				String defaultArgs = javaStartCommand.getOptions();
				if (defaultArgs != null) {
					javaOptions.setText(defaultArgs);
				}

				return javaStartArea;
			}

		};

	}

	/**
	 * Sets a start command based on input in related start command UI controls.
	 * This listener has to be registered with the UI part that displays the
	 * start command controls.
	 * 
	 */
	public interface ICommandChangeListener {
		public void handleChange(String command);
	}

	/**
	 * UI part that creates the controls for a particular start command type.
	 * Listener can be registered that handles changes to the start command
	 * value based on UI control changes.
	 */
	public interface CommandTypeUIPart {
		public Composite createComposite(Composite parent, ICommandChangeListener listener);
	}
}
