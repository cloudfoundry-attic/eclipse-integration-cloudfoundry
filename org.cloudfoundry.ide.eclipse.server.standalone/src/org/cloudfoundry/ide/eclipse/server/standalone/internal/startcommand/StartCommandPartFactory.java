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

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Creates the UI controls for a standalone start command, given a standalone
 * descriptor. UI Controls are generated per start command type, if multiple
 * start command types exist per start command definition.
 */
public class StartCommandPartFactory {

	private final StartCommand startCommand;

	private final IProject project;

	public StartCommandPartFactory(StartCommand startCommand, IProject project) {
		this.startCommand = startCommand;
		this.project = project;
	}

	public StartCommandPart createStartCommandTypePart(StartCommandType type,
			Composite parent) {
		StartCommandPart commandTypePart = null;
		switch (type) {
		case Java:
			commandTypePart = getJavaStartArea(parent);
			break;
		case Other:
			commandTypePart = getOtherStartArea(parent);
			break;
		}
		if (commandTypePart != null) {
			commandTypePart.getComposite();
		}
		return commandTypePart;
	}

	protected StartCommandPart getDefaultStartCommandUIPart(
			final Composite parent) {
		return new StartCommandPart(parent) {

			private Text standaloneStartText;

			public Control createPart(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);

				GridLayoutFactory.fillDefaults().numColumns(1)
						.equalWidth(false).margins(new Point(0, 7))
						.applyTo(composite);
				GridDataFactory.fillDefaults().grab(true, false)
						.applyTo(composite);

				standaloneStartText = createdEditableText(composite);

				standaloneStartText.addModifyListener(new ModifyListener() {

					public void modifyText(ModifyEvent e) {
						updateStartCommand();
					}

				});

				return composite;
			}

			public void updateStartCommand() {
				String value = standaloneStartText.getText();
				boolean isInvalid = ValueValidationUtil.isEmpty(value);
				notifyStatusChange(
						startCommand.toString(),
						isInvalid ? CloudFoundryPlugin
								.getErrorStatus("Invalid start command.") //$NON-NLS-1$
								: Status.OK_STATUS);
			}

		};
	}

	protected StartCommandPart getOtherStartArea(Composite parent) {
		return getDefaultStartCommandUIPart(parent);
	}

	protected Label createdLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false)
				.align(SWT.FILL, SWT.CENTER).applyTo(label);
		if (text != null) {
			label.setText(text);
		}
		return label;
	}

	protected Text createdEditableText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false)
				.align(SWT.FILL, SWT.CENTER).applyTo(text);

		text.setEditable(true);
		return text;
	}

	protected Composite create2ColumnComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.margins(new Point(0, 0)).spacing(5, 2).applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
		return composite;
	}

	protected StartCommandPart getJavaStartArea(Composite parent) {
		IJavaProject javaProject = project != null ? CloudFoundryProjectUtil
				.getJavaProject(project) : null;
		return new JavaStartCommandPart(javaProject, this, startCommand, parent);
	}

}
