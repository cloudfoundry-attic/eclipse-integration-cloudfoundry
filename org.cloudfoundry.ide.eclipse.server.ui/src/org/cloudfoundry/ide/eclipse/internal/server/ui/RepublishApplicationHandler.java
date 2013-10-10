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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.RepublishModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.WaitWithProgressJob;
import org.eclipse.core.resources.IProject;
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

	private final CloudFoundryApplicationModule appModule;

	private final List<String> uris;

	private final CloudFoundryServer cloudServer;

	public RepublishApplicationHandler(CloudFoundryApplicationModule appModule, List<String> uris,
			CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.uris = uris;
		this.cloudServer = cloudServer;
	}

	protected CloudApplication getExistingCloudApplication(IProgressMonitor monitor) {

		CloudApplication existingApp = appModule.getApplication();
		if (existingApp == null) {
			try {
				existingApp = cloudServer.getBehaviour()
						.getApplication(appModule.getDeployedApplicationName(), monitor);
			}
			catch (CoreException e) {
				// Ignore it
			}
		}
		return existingApp;
	}

	public void republish(IProgressMonitor monitor) throws CoreException {

		IProject project = CloudUtil.getProject(appModule);

		boolean republished = false;

		// Can only republish modules that have an accessible project
		if (project != null) {

			// Get the descriptor from the existing application module
			DeploymentInfoWorkingCopy workingCopy = appModule.getDeploymentInfoWorkingCopy();
			workingCopy.setUris(uris);
			workingCopy.save();
			IServer server = cloudServer.getServer();

			final IModule[] modules = ServerUtil.getModules(project);

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
					cloudServer.getBehaviour().refreshModules(monitor);

					new WaitWithProgressJob(5, 1000) {

						@Override
						protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException {
							boolean found = cloudServer.getCloudModule(modules[0]) != null;
							if (found) {
								cloudServer.getBehaviour().refreshModules(monitor);
							}
							// If the app has been found, try again until it
							// is not found
							return !found;
						}

					}.run(monitor);

					// Create new ones
					IModule[] newModules = ServerUtil.getModules(project);
					if (newModules != null && newModules.length == 1) {
						add = new IModule[] { newModules[0] };
					}
				}
				if (add != null && add.length > 0) {
					IServerWorkingCopy wc = server.createWorkingCopy();
					wc = server.createWorkingCopy();
					IStatus status = wc.canModifyModules(add, null, null);
					if (status.getSeverity() != IStatus.ERROR) {
						CloudFoundryPlugin.getModuleCache().getData(wc.getOriginal())
								.tagForAutomaticRepublish(new RepublishModule(add[0], appModule.getDeploymentInfo()));

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

		if (!republished) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus("Failed to republish module: "
					+ appModule.getDeployedApplicationName() + ". Please try manual republishing."));
		}

	}

}
