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

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.JavaRuntimeTypeHelper;
import org.cloudfoundry.ide.eclipse.internal.server.core.RuntimeType;
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
public class StandaloneHandler {

	private final ApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	private List<RuntimeType> appTypes;

	public StandaloneHandler(ApplicationModule appModule, CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	public boolean isSupportedStandalone() {
		return isStandaloneApp() && !getRuntimeTypes().isEmpty();
	}

	protected boolean isStandaloneApp() {
		IModule module = appModule.getLocalModule();
		boolean isStandalone = module != null
				&& CloudFoundryServer.ID_JAVA_STANDALONE_APP.equals(module.getModuleType().getId());
		// If standalone app cannot be determined by the local module, check the
		// staging
		if (!isStandalone) {
			Staging staging = getStaging();
			isStandalone = staging != null && CloudApplication.STANDALONE.equals(staging.getFramework());
		}
		return isStandalone;
	}

	public Staging getStaging() {
		if (appModule == null) {
			return null;
		}
		Staging staging = appModule.getStaging();
		if (staging == null) {
			CloudApplication cloudApp = appModule.getApplication();
			if (cloudApp != null) {
				staging = new StagingHandler(cloudApp.getStaging()).getStaging();
			}
		}
		return staging;
	}

	public boolean isValidStaging(Staging staging) {
		return staging != null && staging.getCommand() != null
				&& CloudApplication.STANDALONE.equals(staging.getFramework()) && staging.getRuntime() != null;
	}

	protected ApplicationModule getApplicationModule() {
		return appModule;
	}

	public List<RuntimeType> getRuntimeTypes() {
		if (appTypes == null) {
			IProject project = getProject();
			if (project != null) {
				IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(project);
				if (javaProject != null && javaProject.exists()) {
					appTypes = new JavaRuntimeTypeHelper(cloudServer).getRuntimeTypes();
				}
			}

			if (appTypes == null) {
				appTypes = Collections.emptyList();
			}
		}
		return appTypes;
	}

	protected IProject getProject() {
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

	/**
	 * Optional start command definition, if defined for this standalone type.
	 * It may be null if no runtime can be resolved for the given application,
	 * or the runtime has not defined a start command definition.
	 */
	public StartCommand getStartCommand() {
		// Find the first start command that matches any of the runtime types
		for (RuntimeType type : getRuntimeTypes()) {
			if (type != null) {
				switch (type) {
				case java:
				case java7:
					return new JavaStartCommand();
				}
			}
		}

		return null;
	}

}
