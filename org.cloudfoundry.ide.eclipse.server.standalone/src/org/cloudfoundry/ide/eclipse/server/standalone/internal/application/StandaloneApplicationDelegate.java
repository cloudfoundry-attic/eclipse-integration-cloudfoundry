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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ManifestParser;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ModuleResourceApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ModuleResourceApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Determines if a give module is a Java standalone application. Also provides
 * an archiving mechanism that is specific to Java standalone applications.
 * 
 */
public class StandaloneApplicationDelegate extends
		ModuleResourceApplicationDelegate {

	public StandaloneApplicationDelegate() {

	}

	public boolean requiresURL() {
		// URLs are optional for Java standalone applications
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.core.application.
	 * IApplicationDelegate
	 * #getApplicationArchive(org.cloudfoundry.ide.eclipse.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer,
	 * org.eclipse.wst.server.core.model.IModuleResource[])
	 */
	public ApplicationArchive getApplicationArchive(
			CloudFoundryApplicationModule module,
			CloudFoundryServer cloudServer, IModuleResource[] moduleResources)
			throws CoreException {
		String archiveURL = module.getDeploymentInfo().getArchive();

		// FIXNS:
		// Workaround to the fact that path manifest property does not get
		// persisted in the server for the application,
		// therefore if the deploymentinfo does not have it, parse it from the
		// manifest, if one can be found for the application
		// in a local project. The reason this is done here as opposed to when
		// deployment info is updated for an application
		// is that the path only gets used when pushing the application (either
		// initial push, or through start/update restart)
		// so it will keep manifest reading I/O only to these cases, rather than
		// on deployment info update, which occurs on
		// every refresh.
		if (archiveURL == null) {
			archiveURL = new ManifestParser(module, cloudServer)
					.getApplicationProperty(
							module.getDeployedApplicationName(),
							ManifestParser.RELATIVE_APP_PATH);
		}
		
		ApplicationArchive appArchive = null;
		if (archiveURL != null) {
			// For now assume urls are project relative
			IProject project = CloudUtil.getProject(module);

			if (project != null) {
				IFile file = project.getFile(archiveURL);
				if (file.exists()) {
					File actualFile = file.getLocation().toFile();
					if (actualFile != null && actualFile.exists()) {
						try {
							appArchive = new ZipApplicationArchive(new ZipFile(
									actualFile));
						} catch (IOException ioe) {
							CloudFoundryPlugin.logError(ioe);
						}
					}
				}
			}
		}

		if (appArchive == null) {
			appArchive = new ModuleResourceApplicationArchive(
					module.getLocalModule(), Arrays.asList(moduleResources));
		}
		return appArchive;
	}

}
