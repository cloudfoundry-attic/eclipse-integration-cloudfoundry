/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 *     HuaweiTech - Initial implementation and Project Selection
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.ModuleCache;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.model.WorkbenchViewerComparator;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;

public class MapToProjectOperation implements ICloudFoundryOperation {

	private final CloudFoundryApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	private Shell shell;

	public MapToProjectOperation(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, Shell shell) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
		this.shell = shell;
	}

	public void run(IProgressMonitor monitor) throws CoreException {

		final IProject[] selectedProject = new IProject[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				selectedProject[0] = selectReplaceProject(cloudServer, appModule);
			}
		});

		if (selectedProject[0] != null) {
			map(selectedProject[0], monitor);
		}
	}

	public void map(IProject project, IProgressMonitor monitor) throws CoreException {
		if (project == null || !project.isAccessible()) {
			throw CloudErrorUtil.toCoreException("No accessible project specified. Unable to link the cloud module with the project.");//$NON-NLS-1$
		}
		if (appModule == null) {
			throw CloudErrorUtil
					.toCoreException("No Cloud module specified. Unable to link cloud module with the project: " + project.getName()); //$NON-NLS-1$
		}

		ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
		ModuleCache.ServerData data = moduleCache.getData(cloudServer.getServerOriginal());

		// if it is being deployed, do not perform remap
		if (data.isUndeployed(appModule.getLocalModule())) {
			throw CloudErrorUtil
					.toCoreException("Unable to unlink the module. It is currently being published. Please wait until the publish operation is complete before relinking the project."); //$NON-NLS-1$
		}

		data.tagForReplace(appModule);
		try {
			doMap(project, monitor);
		}
		finally {
			data.untagForReplace(appModule);
		}
	}

	protected void doMap(IProject project, IProgressMonitor monitor) throws CoreException {
		IServer server = cloudServer.getServer();

		IServerWorkingCopy wc = server.createWorkingCopy();

		final IModule[] modules = ServerUtil.getModules(project);

		if (modules == null || modules.length == 0) {
			throw CloudErrorUtil
					.toCoreException("Unable to create module for " + project.getName() + ". Failed to link the project with " + appModule.getDeployedApplicationName()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (ServerUtil.containsModule(server, modules[0], monitor)) {
			throw CloudErrorUtil
					.toCoreException("Unable to create module for " + project.getName() + ". Module already exists. Failed to link the project with " + appModule.getDeployedApplicationName()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		IModule[] add = new IModule[] { modules[0] };

		ServerUtil.modifyModules(wc, add, new IModule[] { appModule.getLocalModule() }, monitor);
		wc.save(true, monitor);

		CloudFoundryServerBehaviour behaviour = cloudServer.getBehaviour();
		if (behaviour != null) {
			behaviour.cleanModuleStates(add, monitor);
		}

		CloudFoundryApplicationModule updatedModule = cloudServer.getExistingCloudModule(appModule
				.getDeployedApplicationName());

		if (updatedModule != null) {
			cloudServer.getBehaviour().operations().refreshApplication(updatedModule.getLocalModule()).run(monitor);
		}

		
		ServerEventHandler.getDefault().fireServerRefreshed(cloudServer);
	}

	/**
	 * Chooses the project to replace current selected cloud application.
	 * 
	 * @return true if the replace project is selected, false indicates the user
	 * aborts the selection operation
	 */
	protected IProject selectReplaceProject(final CloudFoundryServer cloudServer,
			final CloudFoundryApplicationModule appModule) {

		final String appToBeMappedName = appModule.getDeployedApplicationName();
		ViewerFilter viewerFilter = new ViewerFilter() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {
				IProject potentialProject = null;
				if (element instanceof IProject) {
					potentialProject = (IProject) element;
				}

				if (potentialProject == null || !potentialProject.isAccessible()) {
					return false;
				}
				
				// If a module is set as nonfaceted, we do not need to check it.
				if(!CloudFoundryServer.isNonfacetedModule(appModule)) {
				
					// .. otherwise, all projects should be faceted.
					IFacetedProject facetedProject;
					try {
						facetedProject = ProjectFacetsManager.create(potentialProject);
						
						if(facetedProject == null) {
							// Unfaceted projects are not supported, so return false.
							return false;
						}					
					} catch (CoreException e) {
						// If an error occurs when attempting to convert an individual project, then
						// it likely is not supported (for example, not faceted), so return false.
						return false;
					}
				}
				
				// Allow mapping a project with the same name as the app. This
				// case needs to be handled first before checking for other
				// potentially unrelated
				// modules in the server
				// that have the same name as the project (see below), which as of 1.8.1 is
				// not allowed.
				if (appToBeMappedName.equals(potentialProject.getName())) {
					return true;
				}

				IModule[] allModules = cloudServer.getServerOriginal().getModules();
				if (allModules != null) {
					for (IModule mod : allModules) {
						// Filter out projects that already are mapped to
						// modules
						if (mod.getProject() != null && mod.getProject().equals(potentialProject)) {
							return false;
						}
						// As of 1.8.1, mapping projects that have the same name
						// as other (possibly unrelated modules) is
						// not supported as WTP may create duplicate modules
						// with the same name.
						else if (mod.getName().equals(potentialProject.getName())) {
							return false;
						}
					}
				}
				return true;
			}

		};

		ITreeContentProvider contentProvider = new ITreeContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object ob1, Object ob2) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean hasChildren(Object ob1) {
				return false;
			}

			@Override
			public Object getParent(Object ob1) {
				return null;
			}

			@Override
			public Object[] getElements(Object elements) {
				if (elements instanceof IProject[]) {
					return (IProject[]) elements;
				}
				return null;
			}

			@Override
			public Object[] getChildren(Object parent) {
				return null;
			}
		};

		ILabelProvider labelProvider = new WorkbenchLabelProvider();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(shell, labelProvider, contentProvider) {
			@Override
			protected Label createMessageArea(Composite composite) {
				Label label = new Label(composite, SWT.WRAP);
				GridData gd = new GridData();
				gd.horizontalAlignment = SWT.FILL;
				// The default ElementTreeSelectionDialog label doesn't wrap.
				// Since our description text is long, we need to do this adjustment.
				gd.widthHint = composite.getShell().getSize().x / 5;
				label.setLayoutData(gd);
				String msg = getMessage();
				if (msg != null) {
					label.setText(msg);
				}
				label.setFont(composite.getFont());
				return label;
			}
		};
		dialog.setComparator(new WorkbenchViewerComparator());

		dialog.setTitle(Messages.MapToProjectOperation_PROJECT_SELECTION_DIALOGUE_TITLE);
		dialog.setMessage(NLS.bind(Messages.MapToProjectOperation_PROJECT_SELECTION_DIALOGUE_MESSAGE,
				appModule.getDeployedApplicationName()));
		dialog.addFilter(viewerFilter);
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		dialog.setInitialSelection(null);
		dialog.setHelpAvailable(false);

		IProject selectProj = null;
		if (dialog.open() == Window.OK) {
			Object selectObj = dialog.getFirstResult();
			if (selectObj instanceof IProject) {
				selectProj = (IProject) selectObj;
			}
			final boolean[] result = new boolean[1];

			final IProject selProj = selectProj;
			if (selProj != null) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						result[0] = MessageDialog.openConfirm(shell,
								Messages.MapToProjectOperation_PROJECT_SELECTION_CONFIRM_DIALOGUE_TITLE, NLS.bind(
										Messages.MapToProjectOperation_PROJECT_SELECTION_CONFIRM_DIALOGUE_MESSAGE,
										selProj.getName(), appModule.getDeployedApplicationName()));
					}
				});
			}

			if (result[0]) {
				return selectProj;
			}
		}
		return null;
	}

	
}
