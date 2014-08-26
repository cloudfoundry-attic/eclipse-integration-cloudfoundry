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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleResourceApplicationDelegate;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
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

	@Override
	public boolean shouldSetDefaultUrl(CloudFoundryApplicationModule appModule) {
		IJavaProject project = CloudFoundryProjectUtil
				.getJavaProject(appModule);
		return JavaCloudFoundryArchiver.isBootProject(project);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.core.internal.application.
	 * AbstractApplicationDelegate
	 * #getApplicationArchive(org.cloudfoundry.ide.eclipse.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer,
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
