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

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback.DeploymentDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.RepublishModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;

/**
 * This handles automatic publishing of an app that has an accessible workspace
 * project by using the same descriptor information as set the first time the
 * manual publishing occurred. This avoids prompting a user again for the
 * descriptor information.
 * 
 * 
 */
public class RepublishApplicationHandler {

	private final ApplicationModule appModule;

	private final List<String> uris;

	private final CloudFoundryServer cloudServer;

	public RepublishApplicationHandler(ApplicationModule appModule, List<String> uris, CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.uris = uris;
		this.cloudServer = cloudServer;
	}

	protected CloudApplication getExistingCloudApplication(IProgressMonitor monitor) {

		CloudApplication existingApp = appModule.getApplication();
		if (existingApp == null) {
			try {
				existingApp = cloudServer.getBehaviour().getApplication(appModule.getApplicationId(), monitor);
			}
			catch (CoreException e) {
				// Ignore it
			}
		}
		return existingApp;
	}

	protected IProject getProject(ApplicationModule appModule) {
		IProject project = appModule.getLocalModule() != null ? appModule.getLocalModule().getProject() : null;
		if (project == null) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(appModule.getApplicationId());
		}
		return project;
	}

	public void republish(IProgressMonitor monitor) throws CoreException {

		IProject project = getProject(appModule);

		boolean republished = false;

		// Can only republish modules that have an accessible project
		if (project != null) {

			// Get the descriptor from the existing application module
			DeploymentDescriptor descriptor = getUpdatedDescriptor(monitor);

			if (descriptor != null) {

				IServer server = cloudServer.getServer();

				IModule[] modules = ServerUtil.getModules(project);

				if (modules != null && modules.length == 1) {
					IModule[] add = null;
					if (!ServerUtil.containsModule(server, modules[0], monitor)) {
						add = new IModule[] { modules[0] };
					}
					else {
						// Delete them first
						IServerWorkingCopy wc = server.createWorkingCopy();
						wc.modifyModules(null, modules, monitor);
						wc.save(true, null);
						// Create new ones
						modules = ServerUtil.getModules(project);
						if (modules != null && modules.length == 1) {
							add = new IModule[] { modules[0] };
						}
					}
					if (add != null && add.length > 0) {
						IServerWorkingCopy wc = server.createWorkingCopy();
						wc = server.createWorkingCopy();
						IStatus status = wc.canModifyModules(add, null, null);
						if (status.getSeverity() != IStatus.ERROR) {
							CloudFoundryPlugin.getModuleCache().getData(wc.getOriginal())
									.tagForAutomaticRepublish(new RepublishModule(add[0], descriptor));

							// publish the module
							wc.modifyModules(add, null, monitor);
							wc.save(true, null);
							republished = true;
						}
						else {
							throw new CoreException(status);
						}
					}
				}

			}
		}

		if (!republished) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus("Failed to republish module: "
					+ appModule.getApplicationId() + ". Please try manual republishing."));
		}

	}

	public DeploymentDescriptor getUpdatedDescriptor(IProgressMonitor monitor) throws CoreException {
		DeploymentDescriptor descriptor = new DeploymentDescriptor();

		ApplicationInfo appInfo = appModule.getLastApplicationInfo();
		if (appInfo == null) {
			appInfo = new ApplicationInfo(appModule.getApplicationId());
		}

		if (appInfo.getFramework() == null) {
			String framework = org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil.getFramework(appModule);
			appInfo.setFramework(framework);
		}

		descriptor.applicationInfo = appInfo;

		DeploymentInfo deploymentInfo = appModule.getLastDeploymentInfo();
		if (deploymentInfo == null) {
			deploymentInfo = new DeploymentInfo();
		}

		if (deploymentInfo.getDeploymentName() == null) {
			deploymentInfo.setDeploymentName(appModule.getApplicationId());
		}

		if (deploymentInfo.getMemory() <= 0) {
			deploymentInfo.setMemory(CloudUtil.DEFAULT_MEMORY);
		}

		descriptor.deploymentInfo = deploymentInfo;

		descriptor.staging = appModule.getStaging();
		descriptor.deploymentMode = ApplicationAction.START;
		descriptor.deploymentInfo.setUris(uris);

		return descriptor;

	}

}
