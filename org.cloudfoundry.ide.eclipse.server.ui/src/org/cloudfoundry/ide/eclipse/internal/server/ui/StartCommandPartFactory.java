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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.URLNameValidation;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StandaloneDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.standalone.StartCommandType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
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
public class StartCommandPartFactory {

	private final StandaloneDescriptor standaloneDescriptor;

	public StartCommandPartFactory(StandaloneDescriptor standaloneDescriptor) {
		this.standaloneDescriptor = standaloneDescriptor;
	}

	public StartCommandPart createStartCommandTypePart(StartCommandType type, Composite parent,
			ICommandChangeListener listener) {
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

	protected StartCommandPart getDefaultStartCommandUIPart(Composite parent, final ICommandChangeListener listener) {
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

				updateStartCommand();
				return composite;
			}

			protected void updateStartCommand() {
				String value = standaloneStartText.getText();
				boolean isInvalid = URLNameValidation.isEmpty(value);
				listener.handleChange(value, !isInvalid);
			}

			public void update(IProgressMonitor monitor) {
				// Nothing to update
			}

		};
	}

	protected StartCommandPart getOtherStartArea(Composite parent, final ICommandChangeListener listener) {
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

	protected StartCommandPart getJavaStartArea(Composite parent, final ICommandChangeListener listener) {
		return new JavaStartCommandPart(parent, listener);

	}

	/**
	 * Sets a start command based on input in related start command UI controls.
	 * This listener has to be registered with the UI part that displays the
	 * start command controls.
	 * 
	 */
	public interface ICommandChangeListener {
		public void handleChange(String command, boolean isValid);
	}

	protected IJavaProject getJavaProject() {
		IProject project = standaloneDescriptor.getProject();
		if (project == null || !project.isAccessible()) {
			return null;
		}

		return CloudFoundryProjectUtil.getJavaProject(project);
	}

	abstract class AbstractStartCommandPart implements StartCommandPart {
		protected final Composite parent;

		protected final ICommandChangeListener listener;

		private Composite composite;

		protected AbstractStartCommandPart(Composite parent, ICommandChangeListener listener) {
			this.parent = parent;
			this.listener = listener;
		}

		public Composite getComposite() {
			if (composite == null) {
				composite = createComposite();
			}
			return composite;
		}

		abstract protected Composite createComposite();

	}

	public class JavaStartCommandPart extends AbstractStartCommandPart {
		private Text mainTypeText;

		private Text javaOptions;

		private Button browseButton;

		private JavaTypeUIAdapter typeAdapter;

		public JavaStartCommandPart(Composite parent, ICommandChangeListener listener) {
			super(parent, listener);
		}

		/**
		 * 
		 * @return text control if it is created and not disposed. null
		 * otherwise
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

			Composite mainTypeArea = create2ColumnComposite(javaStartArea);

			createdLabel(mainTypeArea, "Main Type:");

			Composite typeArea = create2ColumnComposite(mainTypeArea);

			mainTypeText = createdEditableText(typeArea);

			mainTypeText.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					updateStartCommand();
				}

			});

			browseButton = new Button(typeArea, SWT.PUSH);
			browseButton.setText("Browse...");
			GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(browseButton);

			// Set type browsing and content assist functionality in the
			// controls
			IJavaProject javaProject = getJavaProject();
			if (javaProject != null) {
				typeAdapter = new JavaTypeUIAdapter(this, javaProject, listener);
				typeAdapter.apply();
			}

			createdLabel(mainTypeArea, "Options:");

			javaOptions = createdEditableText(mainTypeArea);

			StartCommand.JavaStartCommand javaStartCommand = (StartCommand.JavaStartCommand) standaloneDescriptor
					.getStartCommand();

			String defaultArgs = javaStartCommand.getOptions();
			if (defaultArgs != null) {
				javaOptions.setText(defaultArgs);
			}

			javaOptions.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					updateStartCommand();
				}

			});

			updateStartCommand();

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

			listener.handleChange(startCommand.toString(), !isInvalid);

		}

		public void update(IProgressMonitor monitor) {
			if (typeAdapter != null) {
				JavaUIHelper helper = new JavaUIHelper(getJavaProject());
				IType[] types = helper.getMainMethodTypes(monitor);
				String qualifiedTypeName = types != null && types.length > 0 ? types[0].getFullyQualifiedName() : null;
				IPackageFragment defaultPackageFragment = helper.getDefaultPackageFragment();

				if (qualifiedTypeName != null) {
					typeAdapter.update(qualifiedTypeName, defaultPackageFragment, getJavaProject());
				}
			}

		}
	}
}
