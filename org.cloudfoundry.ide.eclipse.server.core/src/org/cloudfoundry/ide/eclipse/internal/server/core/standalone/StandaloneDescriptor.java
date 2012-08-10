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
package org.cloudfoundry.ide.eclipse.internal.server.core.standalone;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.server.core.IModule;

/**
 * This descriptor determines the runtime, workspace project, an default start
 * command of a standalone application module.
 * 
 * <p/>
 * 
 * API is provided to determine if the given application is a valid, supported
 * standalone application. In particular, if a runtime type can be resolved by
 * the descriptor and the standalone application has either a "Standalone"
 * framework or standalone project facet , it is a valid standalone application
 */
public class StandaloneDescriptor {

	private final ApplicationModule appModule;

	private StandaloneRuntimeType type;

	public StandaloneDescriptor(ApplicationModule appModule) {
		this.appModule = appModule;
	}

	public boolean isSupportedStandalone() {
		return StandaloneUtil.isStandaloneApp(appModule) && getRuntimeType() != null;
	}

	public IProject getProject() {
		if (appModule != null) {
			IProject project = null;
			IModule mod = appModule.getLocalModule();
			if (mod != null) {
				project = mod.getProject();
			}
			return project;
		}
		return null;
	}

	protected ApplicationModule getApplicationModule() {
		return appModule;
	}

	public StandaloneRuntimeType getRuntimeType() {
		if (type == null) {
			IProject project = getProject();
			if (project != null) {
				IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(getProject());
				if (javaProject != null && javaProject.exists()) {
					type = StandaloneRuntimeType.Java;
				}
			}
		}

		return type;
	}

	/**
	 * Optional start command definition, if defined for this standalone type.
	 * It may be null if no runtime can be resolved for the given application,
	 * or the runtime has not defined a start command definition.
	 */
	public StartCommand getStartCommand() {
		StandaloneRuntimeType type = getRuntimeType();
		if (type != null) {
			switch (type) {
			case Java:
				return new JavaStartCommand(this);
			}
		}
		return null;
	}

}
