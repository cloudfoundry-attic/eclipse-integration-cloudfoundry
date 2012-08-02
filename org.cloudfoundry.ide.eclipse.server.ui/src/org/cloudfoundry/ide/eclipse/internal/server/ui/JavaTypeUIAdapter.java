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

import org.cloudfoundry.ide.eclipse.internal.server.ui.StartCommandPartFactory.ICommandChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.StartCommandPartFactory.JavaStartCommandPart;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.CompletionContextRequestor;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Given a Java start command part, it applies type browsing to a browse button
 * in the part as well as Java type content assist to the part's text control.
 * Updates can be performed on this part such that the content assist context
 * can be updated, in addition to the values of the text control and type
 * context used for opening the browse dialogue when the browse button is
 * pressed.
 * 
 */
public class JavaTypeUIAdapter {

	private final Button browseButton;

	private final Text typeText;

	private final IJavaProject javaProject;

	private final String title;

	private JavaTypeCompletionProcessor processor;

	private final JavaStartCommandPart javaStartCommandPart;

	public JavaTypeUIAdapter(JavaStartCommandPart javaStartCommandPart, IJavaProject javaProject, String title,
			ICommandChangeListener listener) {

		this.browseButton = javaStartCommandPart.getBrowseButton();
		this.typeText = javaStartCommandPart.getTypeText();
		this.javaProject = javaProject;
		this.title = title;
		this.javaStartCommandPart = javaStartCommandPart;

	}

	public JavaTypeUIAdapter(JavaStartCommandPart javaStartCommandPart, IJavaProject javaProject,
			ICommandChangeListener listener) {
		this(javaStartCommandPart, javaProject, "Browse Type with main method", listener);
	}

	public void apply() {
		Button browseButton = javaStartCommandPart.getBrowseButton();
		if (browseButton != null) {
			browseButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					handleTypeBrowsing();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					handleTypeBrowsing();
				}

			});
		}

		applyContentAssist();

	}

	protected void applyContentAssist() {
		Text text = javaStartCommandPart.getTypeText();
		if (text == null) {
			return;
		}
		processor = createContentAssistProcessor();
		ControlContentAssistHelper.createTextContentAssistant(text, processor);
	}

	protected Shell getShell() {
		Control control = javaStartCommandPart.getTypeText();
		Shell shell = control != null ? control.getShell()
				: javaStartCommandPart.getBrowseButton() != null ? javaStartCommandPart.getBrowseButton().getShell()
						: null;
		return shell;
	}

	public void handleTypeBrowsing() {
		Text textControl = javaStartCommandPart.getTypeText();
		if (textControl == null) {
			return;
		}
		String pattern = textControl.getText();

		Shell shell = getShell();

		int javaSearchType = IJavaSearchConstants.CLASS;

		IJavaElement[] elements = new IJavaElement[] { javaProject };
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);

		FilteredTypesSelectionDialog dialog = new FilteredTypesSelectionDialog(shell, false, null, scope,
				javaSearchType);
		dialog.setTitle(title);
		dialog.setInitialPattern(pattern);

		if (dialog.open() == Window.OK) {
			IType type = (IType) dialog.getFirstResult();
			if (type != null) {
				String qualifiedName = type.getFullyQualifiedName();
				if (qualifiedName != null) {
					textControl.setText(qualifiedName);
				}
			}
		}

		javaStartCommandPart.updateStartCommand();
	}

	public void update(String qualifiedTypeName, final IPackageFragment defaultPackageFragment, IJavaProject javaProject) {
		Text text = javaStartCommandPart.getTypeText();
		if (text != null && qualifiedTypeName != null) {
			text.setText(qualifiedTypeName);
		}

		if (defaultPackageFragment != null) {
			processor.setCompletionContextRequestor(new CompletionContextRequestor() {
				public StubTypeContext getStubTypeContext() {
					return TypeContextChecker.createSuperClassStubTypeContext(
							JavaTypeCompletionProcessor.DUMMY_CLASS_NAME, null, defaultPackageFragment);
				}
			});
		}
	}

	protected JavaTypeCompletionProcessor createContentAssistProcessor() {
		return new JavaTypeCompletionProcessor(false, false, true);
	}

}