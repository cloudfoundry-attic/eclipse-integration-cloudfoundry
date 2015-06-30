/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.ui.SelectMainTypeWizard;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * Helper methods for UI components that require Java type searching, given a
 * valid java project.
 */
public class JavaTypeResolver {

	private final IJavaProject project;

	private String serverID;

	public JavaTypeResolver(IJavaProject project, String serverID) {
		this.project = project;
		this.serverID = serverID;
	}

	protected IJavaProject getJavaProject() {
		return project;
	}

	public IType[] getMainTypes(IProgressMonitor monitor) {
		IJavaProject javaProject = getJavaProject();

		if (javaProject != null) {
			// Returns main method types
			boolean includeSubtypes = true;
			MainMethodSearchEngine engine = new MainMethodSearchEngine();
			int constraints = IJavaSearchScope.SOURCES;
			constraints |= IJavaSearchScope.APPLICATION_LIBRARIES;
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
					new IJavaElement[] { javaProject }, constraints);
			return engine.searchMainMethods(monitor, scope, includeSubtypes);
		}
		return new IType[] {};

	}

	public IType getMainTypesFromSource(IProgressMonitor monitor) {
		if (project != null) {
			IType[] types = getMainTypes(monitor);
			// Enable when dependency to
			// org.springsource.ide.eclipse.commons.core is
			// added. This should be the common way to obtain main types
			// MainTypeFinder.guessMainTypes(project, monitor);

			if (types != null && types.length > 0) {

				final List<IType> typesFromSource = new ArrayList<IType>();

				for (IType type : types) {
					if (!type.isBinary() && !typesFromSource.contains(type)) {
						typesFromSource.add(type);
					}
				}

				if (typesFromSource.size() == 1) {
					return typesFromSource.get(0);
				} else {
					// Prompt user to select a main type

					final IType[] selectedType = new IType[1];
					Display.getDefault().syncExec(new Runnable() {

						@Override
						public void run() {

							final Shell shell = CloudUiUtil.getShell();

							if (shell != null && !shell.isDisposed()) {
								SelectMainTypeWizard wizard = new SelectMainTypeWizard(
										serverID, typesFromSource);
								WizardDialog dialog = new WizardDialog(shell,
										wizard);
								if (dialog.open() == Window.OK) {
									selectedType[0] = wizard
											.getSelectedMainType();
								}

							} else {
								CloudFoundryPlugin
										.getCallback()
										.handleError(
												CloudFoundryPlugin.getErrorStatus(NLS
														.bind(Messages.SelectMainTypeWizardPage_NO_SHELL,
																project.getProject()
																		.getName())));
								selectedType[0] = typesFromSource.get(0);
							}
						}
					});
					return selectedType[0];
				}
			}
		}
		return null;
	}
}
