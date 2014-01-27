/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ModuleResourceApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
			CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IModuleResource[] moduleResources,
			IProgressMonitor monitor) throws CoreException {
		return new JavaCloudFoundryArchiver(appModule, cloudServer)
				.getApplicationArchive(monitor);
	}

}
