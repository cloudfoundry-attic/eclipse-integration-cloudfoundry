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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
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

	private List<StandaloneRuntimeType> appTypes;

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

	/**
	 * Always returns a non-null list. May be empty
	 * @return
	 */
	public List<StandaloneRuntimeType> getRuntimeTypes() {
		if (appTypes == null) {
			IProject project = getProject();
			if (project != null) {
				IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(getProject());
				if (javaProject != null && javaProject.exists()) {
					List<CloudInfo.Runtime> actualTypes = cloudServer.getBehaviour().getRuntimes();
					appTypes = new ArrayList<StandaloneRuntimeType>(getJavaTypes(actualTypes));
				}
			}

			if (appTypes == null) {
				appTypes = Collections.emptyList();
			}
		}

		return appTypes;
	}

	/**
	 * Returns the Java runtimes supported by the given cloud server based on
	 * the local Java runtime definitions. If no Java runtimes are found,
	 * returns empty list
	 * @param actualRuntimes
	 * @return Java runtimes supported by cloud server, or empty list. Never
	 * null.
	 */
	protected List<StandaloneRuntimeType> getJavaTypes(List<CloudInfo.Runtime> actualRuntimes) {
		if (actualRuntimes == null) {
			return Collections.emptyList();
		}
		Set<String> runtimeIds = new HashSet<String>();
		for (CloudInfo.Runtime actualRuntime : actualRuntimes) {
			runtimeIds.add(actualRuntime.getName());
		}
		StandaloneRuntimeType[] expectedTypes = { StandaloneRuntimeType.java, StandaloneRuntimeType.java7 };
		List<StandaloneRuntimeType> foundRuntimes = new ArrayList<StandaloneRuntimeType>();

		// Check whether the expected types still are found in the server
		// runtime list. Only show runtime types that
		// match those that are actually supported in the server
		for (StandaloneRuntimeType expectedType : expectedTypes) {
			if (runtimeIds.contains(expectedType.name())) {
				foundRuntimes.add(expectedType);
			}
		}

		return foundRuntimes;

	}

	/**
	 * Optional start command definition, if defined for this standalone type.
	 * It may be null if no runtime can be resolved for the given application,
	 * or the runtime has not defined a start command definition.
	 */
	public StartCommand getStartCommand() {
		// Find the first start command that matches any of the runtime types
		for (StandaloneRuntimeType type : getRuntimeTypes()) {
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
