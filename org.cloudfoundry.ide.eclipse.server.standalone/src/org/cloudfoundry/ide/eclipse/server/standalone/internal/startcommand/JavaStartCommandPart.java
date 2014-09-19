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
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class JavaStartCommandPart extends StartCommandPart {
	/**
	 * 
	 */
	private final StartCommandPartFactory startCommandPartFactory;

	private Text mainTypeText;

	private Text javaOptions;

	private Button browseButton;

	private JavaTypeUIAdapter typeAdapter;

	private StartCommand startCommand;

	private final IJavaProject javaProject;

	public JavaStartCommandPart(IJavaProject javaProject,
			StartCommandPartFactory startCommandPartFactory,
			StartCommand startCommand, Composite parent) {
		super(parent);
		this.startCommandPartFactory = startCommandPartFactory;
		this.startCommand = startCommand;
		this.javaProject = javaProject;
	}

	/**
	 * 
	 * @return text control if it is created and not disposed. null otherwise
	 */
	public Text getTypeText() {
		return mainTypeText != null && !mainTypeText.isDisposed() ? mainTypeText
				: null;
	}

	/**
	 * 
	 * @return browse button control if it is created and not disposed. null
	 *         otherwise
	 */
	public Button getBrowseButton() {
		return browseButton != null && !browseButton.isDisposed() ? browseButton
				: null;
	}

	public Control createPart(Composite parent) {
		Composite javaStartArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0)
				.applyTo(javaStartArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(javaStartArea);

		Composite mainTypeArea = startCommandPartFactory
				.create2ColumnComposite(javaStartArea);

		startCommandPartFactory.createdLabel(mainTypeArea, "Main Type:"); //$NON-NLS-1$

		Composite typeArea = startCommandPartFactory
				.create2ColumnComposite(mainTypeArea);

		mainTypeText = startCommandPartFactory.createdEditableText(typeArea);

		mainTypeText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateStartCommand();
			}

		});

		browseButton = new Button(typeArea, SWT.PUSH);
		browseButton.setText("Browse..."); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(false, false)
				.align(SWT.BEGINNING, SWT.CENTER).applyTo(browseButton);

		if (javaProject != null) {
			typeAdapter = new JavaTypeUIAdapter(this, javaProject);
			typeAdapter.apply();
		}

		startCommandPartFactory.createdLabel(mainTypeArea, "Options:"); //$NON-NLS-1$

		javaOptions = startCommandPartFactory.createdEditableText(mainTypeArea);

		String defaultArgs = startCommand.getArgs();
		if (defaultArgs != null) {
			javaOptions.setText(defaultArgs);
		}

		javaOptions.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateStartCommand();
			}

		});

		return javaStartArea;
	}

	public void updateStartCommand() {

		String mainType = mainTypeText.getText();
		// Start command must have a main method type, or its not valid
		boolean isInvalid = ValueValidationUtil.isEmpty(mainType);

		String options = javaOptions.getText();

		StringBuilder startCommand = new StringBuilder("java"); //$NON-NLS-1$

		if (options != null) {
			startCommand.append(" "); //$NON-NLS-1$
			startCommand.append(options);
		}
		if (mainType != null) {
			startCommand.append(" "); //$NON-NLS-1$
			startCommand.append(mainType);
		}

		// Also update default start command part, which shows the full start
		// command text
		notifyStatusChange(
				startCommand.toString(),
				isInvalid ? CloudFoundryPlugin
						.getErrorStatus("Invalid start command.") //$NON-NLS-1$
						: Status.OK_STATUS);

	}

}