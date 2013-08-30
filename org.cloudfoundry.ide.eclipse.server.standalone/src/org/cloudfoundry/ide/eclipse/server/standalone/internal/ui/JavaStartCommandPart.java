/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.application.StartCommand;
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

		startCommandPartFactory.createdLabel(mainTypeArea, "Main Type:");

		Composite typeArea = startCommandPartFactory
				.create2ColumnComposite(mainTypeArea);

		mainTypeText = startCommandPartFactory.createdEditableText(typeArea);

		mainTypeText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateStartCommand();
			}

		});

		browseButton = new Button(typeArea, SWT.PUSH);
		browseButton.setText("Browse...");
		GridDataFactory.fillDefaults().grab(false, false)
				.align(SWT.BEGINNING, SWT.CENTER).applyTo(browseButton);

		if (javaProject != null) {
			typeAdapter = new JavaTypeUIAdapter(this, javaProject);
			typeAdapter.apply();
		}

		startCommandPartFactory.createdLabel(mainTypeArea, "Options:");

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

		StringBuilder startCommand = new StringBuilder("java");

		if (options != null) {
			startCommand.append(" ");
			startCommand.append(options);
		}
		if (mainType != null) {
			startCommand.append(" ");
			startCommand.append(mainType);
		}

		// Also update default start command part, which shows the full start
		// command text
		notifyStatusChange(
				startCommand.toString(),
				isInvalid ? CloudFoundryPlugin
						.getErrorStatus("Invalid start command.")
						: Status.OK_STATUS);

	}

}