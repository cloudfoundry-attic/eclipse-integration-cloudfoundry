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
package org.eclipse.cft.server.core.internal.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.AbstractWaitWithProgressJob;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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

	private final static Pattern NGROK_OUTPUT_FILE = Pattern.compile(".*ngrok\\.txt"); //$NON-NLS-1$

	@Override
	public DebugConnectionDescriptor getDebugConnectionDescriptor(final CloudFoundryApplicationModule appModule,
			final CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {

		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		IFile ngrokFile = getFile(appModule.getLocalModule().getProject(), ".profile.d", "ngrok.sh"); //$NON-NLS-1$ //$NON-NLS-2$ 

		String remoteNgrokOutputFile = null;

		if (ngrokFile != null && ngrokFile.getRawLocation() != null) {
			try {

				Reader reader = new FileReader(new File(ngrokFile.getRawLocation().toString()));
				StringWriter writer = new StringWriter();
				try {
					IOUtils.copy(reader, writer);
				}
				finally {
					reader.close();
				}
				String fileContents = writer.toString();
				if (fileContents != null) {
					String[] segments = fileContents.split(">");
					if (segments.length >= 2) {
						Matcher matcher = NGROK_OUTPUT_FILE.matcher(segments[1]);
						if (matcher.find()) {
							remoteNgrokOutputFile = segments[1].substring(matcher.start(), matcher.end());
							if (remoteNgrokOutputFile != null) {
								remoteNgrokOutputFile = remoteNgrokOutputFile.trim();
							}
						}
					}
				}
			}
			catch (FileNotFoundException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}

		if (remoteNgrokOutputFile == null) {
			String errorMessage = "Unable to connect the debugger. Failed to resolve a path to an output ngrok.txt file from the local ngrok.sh file. Please ensure the shell script file exists in your project and is in the project's classpath. Also please ensure that the script includes a command for the ngrok executable running on the Cloud to generate output to an ngrok.txt."; //$NON-NLS-1$
			throw CloudErrorUtil.toCoreException(errorMessage);
		}

		String fileContent = getFileContent(appModule, cloudServer, remoteNgrokOutputFile, subMonitor);

		if (fileContent.indexOf("Tunnel established at tcp://ngrok.com:") > -1) { //$NON-NLS-1$
			String pattern = "Tunnel established at tcp://ngrok.com:"; //$NON-NLS-1$
			int start = fileContent.indexOf(pattern);
			String sub = fileContent.substring(start);
			int end = sub.indexOf('\n');
			sub = sub.substring(pattern.length(), end);
			int port = Integer.parseInt(sub.trim());

			DebugConnectionDescriptor descriptor = new DebugConnectionDescriptor("ngrok.com", port); //$NON-NLS-1$

			if (!descriptor.areValidIPandPort()) {
				throw CloudErrorUtil
						.toCoreException("Invalid port:" + descriptor.getPort() + " or ngrok server address: " + descriptor.getIp() //$NON-NLS-1$ //$NON-NLS-2$ 
								+ " parsed from ngrok output file in the Cloud."); //$NON-NLS-1$
			}
			return descriptor;
		}
		else {
			throw CloudErrorUtil
					.toCoreException("Unable to parse port or ngrok server address from the ngrok output file in the Cloud for " + appModule.getDeployedApplicationName() + ". Please verify that ngrok executable is present in the application deployment and running in the Cloud"); //$NON-NLS-1$  //$NON-NLS-2$ 
		}
	}

	/**
	 * @return non-null file content
	 * @throws CoreException if error occurred, or file not found
	 */
	protected String getFileContent(final CloudFoundryApplicationModule appModule,
			final CloudFoundryServer cloudServer, final String outputFilePath, IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {

		final int attempts = 100;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100 * attempts);
		String content = null;
		CoreException error = null;
		try {
			content = new AbstractWaitWithProgressJob<String>(attempts, 3000, true) {

				protected String runInWait(IProgressMonitor monitor) throws CoreException {
					if (monitor.isCanceled()) {
						return null;
					}
					SubMonitor subMonitor = SubMonitor.convert(monitor);
					CloudApplication app = null;
					try {
						app = cloudServer.getBehaviour().getCloudApplication(appModule.getDeployedApplicationName(),
								subMonitor.newChild(50));
					}
					catch (CoreException e) {
						// Handle app errors separately
						CloudFoundryPlugin.logError(e);
					}

					// Stop checking for the file if the application no longer
					// exists or is not running
					if (app != null && app.getState() == AppState.STARTED) {
						return cloudServer.getBehaviour().getFile(appModule.getDeployedApplicationName(), 0,
								outputFilePath, subMonitor.newChild(50));
					}
					else {
						return null;
					}
				}

				// Any result is valid for this operation, as errors are handled
				// via exception
				protected boolean isValid(String result) {
					return true;
				}

			}.run(subMonitor);
		}
		catch (CoreException e) {
			error = e;
		}

		if (subMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		if (content == null) {
			String message = "Failed to connect debugger to Cloud application - Timed out fetching ngrok output file for: "//$NON-NLS-1$
					+ appModule.getDeployedApplicationName()
					+ ". Please verify that the ngrok output file exists in the Cloud or that the application is running correctly.";//$NON-NLS-1$
			if (error != null) {
				throw CloudErrorUtil.asCoreException(message, error, false);
			}
			else {
				throw CloudErrorUtil.toCoreException(message);
			}
		}

		return content;
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

	protected boolean containsDebugFiles(IJavaProject project) {
		try {

			IClasspathEntry[] entries = project.getResolvedClasspath(true);

			if (entries != null) {
				for (IClasspathEntry entry : entries) {
					if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath projectPath = project.getPath();
						IPath relativePath = entry.getPath().makeRelativeTo(projectPath);
						IFolder folder = project.getProject().getFolder(relativePath);
						if (getFile(folder, ".profile.d", "ngrok.sh") != null) {//$NON-NLS-1$ //$NON-NLS-2$
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

	protected IFile getFile(IResource resource, String containingFolderName, String fileName) throws CoreException {

		if (resource == null || !resource.exists()) {
			return null;
		}
		if (resource instanceof IFile && resource.getName().equals(fileName) && resource.getParent() != null
				&& resource.getParent().getName().equals(containingFolderName)) {
			return (IFile) resource;
		}
		else if (resource instanceof IContainer) {
			IContainer container = (IContainer) resource;
			IResource[] children = container.members();

			if (children != null) {
				for (IResource child : children) {

					IFile file = getFile(child, containingFolderName, fileName);
					if (file != null) {
						return file;
					}
				}
			}
		}

		return null;
	}
}
