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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.URLNameValidation;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommandType;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Creates the UI controls for a standalone start command, given a standalone
 * descriptor. UI Controls are generated per start command type, if multiple
 * start command types exist per start command definition.
 */
public class StartCommandPartFactory {

	final StandaloneDescriptor standaloneDescriptor;

	public StartCommandPartFactory(StandaloneDescriptor standaloneDescriptor) {
		this.standaloneDescriptor = standaloneDescriptor;
	}

	public StartCommandPart createStartCommandTypePart(StartCommandType type, Composite parent,
			IStartCommandPartListener listener) {
		StartCommandPart commandTypePart = null;
		switch (type) {
		case Java:
			commandTypePart = getJavaStartArea(parent, listener);
			break;
		case Other:
			commandTypePart = getOtherStartArea(parent, listener);
			break;
		}
		if (commandTypePart != null) {
			commandTypePart.getComposite();
		}
		return commandTypePart;
	}

	protected StartCommandPart getDefaultStartCommandUIPart(Composite parent, final IStartCommandPartListener listener) {
		return new AbstractStartCommandPart(parent, listener) {

			private Text standaloneStartText;

			protected Composite createComposite() {
				Composite composite = new Composite(parent, SWT.NONE);

				GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).margins(new Point(0, 7))
						.applyTo(composite);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

				standaloneStartText = createdEditableText(composite);

				standaloneStartText.addModifyListener(new ModifyListener() {

					public void modifyText(ModifyEvent e) {
						updateStartCommand();
					}

				});

				return composite;
			}

			protected void updateStartCommand() {
				String value = standaloneStartText.getText();
				boolean isInvalid = URLNameValidation.isEmpty(value);
				listener.handleChange(value, !isInvalid);

			}

			public void updateStartCommand(StartCommandEvent event) {
				if (event.equals(StartCommandEvent.UPDATE)) {
					updateStartCommand();
				}
			}

		};
	}

	protected StartCommandPart getOtherStartArea(Composite parent, final IStartCommandPartListener listener) {
		return getDefaultStartCommandUIPart(parent, listener);
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

	protected StartCommandPart getJavaStartArea(Composite parent, final IStartCommandPartListener listener) {
		return new JavaStartCommandPart(this, parent, listener);

	}

	/**
	 * Listeners registered to start command parts that are notified when a
	 * start command value has been changed, and whether the start command is
	 * valid or not
	 * 
	 */
	public interface IStartCommandPartListener {

		public void handleChange(String command, boolean isValid);

	}

	/**
	 * 
	 * Listener that is notified of start command change events.
	 * 
	 */
	public interface IStartCommandChangeListener {

		public void handleEvent(StartCommandEvent event);

	}

	public static class StartCommandEvent {

		public static final StartCommandEvent UPDATE = new StartCommandEvent("Update");

		private final String type;

		public StartCommandEvent(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}

	protected IJavaProject getJavaProject() {
		IProject project = standaloneDescriptor.getProject();
		if (project == null || !project.isAccessible()) {
			return null;
		}

		return CloudFoundryProjectUtil.getJavaProject(project);
	}
}
