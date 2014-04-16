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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.ui.progress.UIJob;

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

	private final IJavaProject javaProject;

	private final String title;

	private JavaTypeCompletionProcessor processor;

	private final JavaStartCommandPart javaStartCommandPart;

	public JavaTypeUIAdapter(JavaStartCommandPart javaStartCommandPart,
			IJavaProject javaProject, String title) {

		this.javaProject = javaProject;
		this.title = title;
		this.javaStartCommandPart = javaStartCommandPart;

	}

	public JavaTypeUIAdapter(JavaStartCommandPart javaStartCommandPart,
			IJavaProject javaProject) {
		this(javaStartCommandPart, javaProject, "Browse Type with main method");
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
		final JavaTypeResolver helper = new JavaTypeResolver(javaProject);
		final IPackageFragment defaultPackageFragment = getDefaultPackageFragment(javaProject);

		if (defaultPackageFragment != null) {
			processor
					.setCompletionContextRequestor(new CompletionContextRequestor() {
						public StubTypeContext getStubTypeContext() {
							return TypeContextChecker
									.createSuperClassStubTypeContext(
											JavaTypeCompletionProcessor.DUMMY_CLASS_NAME,
											null, defaultPackageFragment);
						}
					});
		}
		UIJob job = new UIJob("Initialising Java Content Assist") {

			public IStatus runInUIThread(IProgressMonitor monitor) {

				IType type = helper.getMainTypesFromSource(monitor);
				String qualifiedTypeName = type != null ? type
						.getFullyQualifiedName() : null;
				Text text = javaStartCommandPart.getTypeText();
				if (qualifiedTypeName != null && text != null) {
					text.setText(qualifiedTypeName);
				}
				return Status.OK_STATUS;
			}

		};
		job.setPriority(Job.INTERACTIVE);
		job.setSystem(true);
		job.schedule();

	}

	protected Shell getShell() {
		Control control = javaStartCommandPart.getTypeText();
		Shell shell = control != null ? control.getShell()
				: javaStartCommandPart.getBrowseButton() != null ? javaStartCommandPart
						.getBrowseButton().getShell() : null;
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

		FilteredTypesSelectionDialog dialog = new FilteredTypesSelectionDialog(
				shell, false, null, scope, javaSearchType);
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

	protected JavaTypeCompletionProcessor createContentAssistProcessor() {
		return new JavaTypeCompletionProcessor(false, false, true);
	}
	
	public IPackageFragment getDefaultPackageFragment(IJavaProject javaProject) {

		if (javaProject == null) {
			return null;
		}

		List<IPackageFragmentRoot> packFragRoots = new ArrayList<IPackageFragmentRoot>();
		try {

			IClasspathEntry[] entries = javaProject.getRawClasspath();

			for (IClasspathEntry entry : entries) {

				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPackageFragmentRoot[] roots = javaProject.findPackageFragmentRoots(entry);
					if (roots != null) {
						for (IPackageFragmentRoot rt : roots) {
							if (!packFragRoots.contains(rt)) {
								packFragRoots.add(rt);
							}
						}
					}
				}
			}

		}
		catch (JavaModelException e) {
			CloudFoundryPlugin.log(e);
		}

		IPackageFragment fragment = null;
		for (IPackageFragmentRoot root : packFragRoots) {
			try {
				IJavaElement[] members = root.getChildren();
				if (members != null) {
					for (IJavaElement element : members) {
						if (element instanceof IPackageFragment) {
							IPackageFragment frag = (IPackageFragment) element;
							if (frag.isDefaultPackage()) {
								fragment = frag;
								break;
							}
						}
					}
				}
				if (fragment != null) {
					break;
				}
			}
			catch (JavaModelException e) {
				CloudFoundryPlugin.log(e);
			}
		}
		return fragment;
	}

}