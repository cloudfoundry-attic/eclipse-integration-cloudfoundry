/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.EnvironmentVariable;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.AbstractWaitWithProgressJob;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Performs a connection to a given server and module. Handles network timeouts,
 * including retrying if connections failed.
 */
public class DebugProvider implements IDebugProvider {

	private static DebugProvider defaultProvider;

	private static final String JAVA_OPTS = "JAVA_OPTS"; //$NON-NLS-1$

	@Override
	public DebugConnectionDescriptor getDebugConnectionDescriptor(final CloudFoundryApplicationModule appModule,
			final CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException {
		
		final int attempts = 50;
		final int totalChildWork = 10;
		SubMonitor subMonitor = SubMonitor.convert(monitor, attempts*totalChildWork);

		String fileContent = new AbstractWaitWithProgressJob<String>(attempts, 5000, true) {

			protected String runInWait(IProgressMonitor monitor) throws CoreException {
				if (monitor.isCanceled()) {
					return null;
				}
				SubMonitor subMonitor = SubMonitor.convert(monitor);
				return cloudServer.getBehaviour().getFile(appModule.getDeployedApplicationName(), 0,
						"app/.profile.d/ngrok.txt", subMonitor.newChild(totalChildWork)); //$NON-NLS-1$
			}

		}.run(subMonitor);

		// NS: Note - replace with JSON parsing, as the ngrok output contains
		// JSON with the port
		if (fileContent != null && fileContent.indexOf("Tunnel established at tcp://ngrok.com:") > -1) { //$NON-NLS-1$
			String pattern = "Tunnel established at tcp://ngrok.com:"; //$NON-NLS-1$
			int start = fileContent.indexOf(pattern);
			String sub = fileContent.substring(start);
			int end = sub.indexOf('\n');
			sub = sub.substring(pattern.length(), end);
			int port = Integer.parseInt(sub.trim());

			if (port > 0) {
				return new DebugConnectionDescriptor("ngrok.com", port); //$NON-NLS-1$
			}
		}
		return null;
	}

	@Override
	public boolean canLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		return containsDebugOption(getDebugEnvironment(appModule.getDeploymentInfo()));
	}

	@Override
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(appModule);
		return javaProject != null && javaProject.exists() && containsDebugFiles(javaProject);
	}

	public boolean isCloudSpaceDebugEnabled() {
		return false;
	}

	@Override
	public String getLaunchConfigurationID() {
		return CloudFoundryDebuggingLaunchConfigDelegate.LAUNCH_CONFIGURATION_ID;
	}

	protected EnvironmentVariable getDebugEnvironment(ApplicationDeploymentInfo info) {
		List<EnvironmentVariable> vars = info.getEnvVariables();
		for (EnvironmentVariable var : vars) {
			if (JAVA_OPTS.equals(var.getVariable())) {
				return var;
			}
		}
		return null;
	}

	@Override
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		List<EnvironmentVariable> vars = info.getEnvVariables();
		EnvironmentVariable javaOpts = getDebugEnvironment(info);

		if (!containsDebugOption(javaOpts)) {
			if (javaOpts == null) {
				javaOpts = new EnvironmentVariable();
				javaOpts.setVariable(JAVA_OPTS);
				vars.add(javaOpts);
			}

			String value = javaOpts.getValue();
			String debugOpts = "-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n"; //$NON-NLS-1$
			if (value == null) {
				value = debugOpts;
			}
			else {
				value = value + ' ' + debugOpts;
			}

			javaOpts.setValue(value);

			cloudServer
					.getBehaviour()
					.operations()
					.environmentVariablesUpdate(appModule.getLocalModule(), appModule.getDeployedApplicationName(),
							vars).run(monitor);

		}
		return true;

	}

	protected boolean containsDebugOption(EnvironmentVariable var) {
		return var != null && var.getValue() != null && JAVA_OPTS.equals(var.getVariable())
				&& (var.getValue().contains("-Xdebug") || var.getValue().contains("-Xrunjdwp")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static DebugProvider getCurrent(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		if (defaultProvider == null) {
			defaultProvider = new DebugProvider();
		}
		return defaultProvider;
	}

	/**
	 * Returns either test sources, or non-test sources, based on a flag
	 * setting. If nothing is found, returns empty list.
	 */
	protected boolean containsDebugFiles(IJavaProject project) {
		try {

			IClasspathEntry[] entries = project.getResolvedClasspath(true);

			if (entries != null) {
				for (IClasspathEntry entry : entries) {
					if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath projectPath = project.getPath();
						IPath relativePath = entry.getPath().makeRelativeTo(projectPath);
						IFolder folder = project.getProject().getFolder(relativePath);
						if (containsResource(folder, ".profile.d")) {//$NON-NLS-1$
							return true;
						}
					}
				}
			}
		}
		catch (JavaModelException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (CoreException ce) {
			CloudFoundryPlugin.logError(ce);
		}
		return false;
	}

	protected boolean containsResource(IContainer container, String pattern) throws CoreException {
		if (container != null && container.exists()) {
			if (container.getName().contains(pattern)) {
				return true;
			}
			for (IResource child : container.members()) {
				if (child instanceof IContainer) {
					IContainer childContainer = (IContainer) child;
					if (containsResource(childContainer, pattern)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
