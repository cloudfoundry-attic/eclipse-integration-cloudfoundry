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

import org.cloudfoundry.ide.eclipse.internal.server.core.URLNameValidation;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommand;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.IStartCommandPartListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.StartCommandEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class JavaStartCommandPart extends AbstractStartCommandPart {
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

	public JavaStartCommandPart(IJavaProject javaProject, StartCommandPartFactory startCommandPartFactory,
			StartCommand startCommand, Composite parent, IStartCommandPartListener listener) {
		super(parent, listener);
		this.startCommandPartFactory = startCommandPartFactory;
		this.startCommand = startCommand;
		this.javaProject = javaProject;
	}

	/**
	 * 
	 * @return text control if it is created and not disposed. null otherwise
	 */
	public Text getTypeText() {
		return mainTypeText != null && !mainTypeText.isDisposed() ? mainTypeText : null;
	}

	/**
	 * 
	 * @return browse button control if it is created and not disposed. null
	 * otherwise
	 */
	public Button getBrowseButton() {
		return browseButton != null && !browseButton.isDisposed() ? browseButton : null;
	}

	protected Composite createComposite() {
		Composite javaStartArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(javaStartArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(javaStartArea);

		Composite mainTypeArea = startCommandPartFactory.create2ColumnComposite(javaStartArea);

		startCommandPartFactory.createdLabel(mainTypeArea, "Main Type:");

		Composite typeArea = startCommandPartFactory.create2ColumnComposite(mainTypeArea);

		mainTypeText = startCommandPartFactory.createdEditableText(typeArea);

		mainTypeText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateStartCommand();
			}

		});

		browseButton = new Button(typeArea, SWT.PUSH);
		browseButton.setText("Browse...");
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(browseButton);

		if (javaProject != null) {
			typeAdapter = new JavaTypeUIAdapter(this, javaProject, listener);
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
		boolean isInvalid = URLNameValidation.isEmpty(mainType);

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
		listener.handleChange(startCommand.toString(), !isInvalid);

	}

	public void updateStartCommand(StartCommandEvent event) {
		if (event.equals(StartCommandEvent.UPDATE)) {
			updateStartCommand();
		}
	}

}