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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Messages;
import org.eclipse.wst.server.core.internal.ProgressUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ModuleFolder;
import org.eclipse.wst.server.core.util.PublishHelper;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class CloudUtil {

	public static final int DEFAULT_MEMORY = 512;

	public static final String DEFAULT_FRAMEWORK = DeploymentConstants.SPRING;

	private static final IStatus[] EMPTY_STATUS = new IStatus[0];

	public static IWebModule getWebModule(IModule[] modules) {

		IModuleType moduleType = modules[0].getModuleType();

		if (modules.length == 1 && moduleType != null && "jst.web".equals(moduleType.getId())) {
			return (IWebModule) modules[0].loadAdapter(IWebModule.class, null);
		}
		return null;

	}

	/**
	 * Creates a partial war file containing only the resources listed in the
	 * list to filter in. Note that at least one content must be present in the
	 * list to filter in, otherwise null is returned.
	 * @param resources
	 * @param module
	 * @param server
	 * @param monitor
	 * @return partial war file with resources specified in the filter in list,
	 * or null if filter list is empty or null
	 * @throws CoreException
	 */
	public static File createWarFile(List<IModuleResource> allResources, IModule module,
			Set<IModuleResource> filterInResources, IProgressMonitor monitor) throws CoreException {
		if (allResources == null || allResources.isEmpty() || filterInResources == null || filterInResources.isEmpty()) {
			return null;
		}
		List<IStatus> result = new ArrayList<IStatus>();
		try {
			File tempDirectory = getTempFolder(module);
			// tempFile needs to be in the same location as the war file
			// otherwise PublishHelper will fail
			String fileName = module.getName() + ".war";

			File warFile = new File(tempDirectory, fileName);
			warFile.createNewFile();
			warFile.deleteOnExit();
			List<IModuleResource> newResources = new ArrayList<IModuleResource>();
			for (IModuleResource mr : allResources) {
				newResources.add(processModuleResource(mr));
			}

			IStatus[] status = publishZip(allResources, warFile, filterInResources, monitor);
			merge(result, status);
			throwException(result, NLS.bind("Publishing of ''{0}'' failed", module.getName()));

			return warFile;
		}
		catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
					"Failed to create war file: {0}", e.getMessage()), e));
		}
	}

	public static File createWarFile(IModule[] modules, Server server, IProgressMonitor monitor) throws CoreException {
		List<IStatus> result = new ArrayList<IStatus>();
		try {
			File tempFile = getTempFolder(modules[0]);
			// tempFile needs to be in the same location as the war file
			// otherwise PublishHelper will fail
			File targetFile = new File(tempFile, modules[0].getName() + ".war");
			targetFile.deleteOnExit();
			PublishHelper helper = new PublishHelper(tempFile);

			ArrayList<IModuleResource> resources = new ArrayList<IModuleResource>(Arrays.asList(server
					.getResources(modules)));

			IWebModule webModule = getWebModule(modules);

			if (webModule != null) {

				IModule[] children = webModule.getModules();

				if (children != null) {
					for (IModule child : children) {
						String childUri = null;
						if (webModule != null) {
							childUri = webModule.getURI(child);
						}
						IJ2EEModule childModule = (IJ2EEModule) child.loadAdapter(IJ2EEModule.class, monitor);
						boolean isBinary = false;
						if (childModule != null) {
							isBinary = childModule.isBinary();
						}
						if (isBinary) {
							// binaries are copied to the destination
							// directory
							if (childUri == null) {
								childUri = "WEB-INF/lib/" + child.getName();
							}
							IPath jarPath = new Path(childUri);
							File jarFile = new File(tempFile, jarPath.lastSegment());
							jarPath = jarPath.removeLastSegments(1);

							IModuleResource[] mr = server.getResources(new IModule[] { child });
							IStatus[] status = helper.publishToPath(mr, new Path(jarFile.getAbsolutePath()), monitor);
							merge(result, status);
							resources.add(new ModuleFile(jarFile, jarFile.getName(), jarPath));
						}
						else {
							// other modules are assembled into a jar
							if (childUri == null) {
								childUri = "WEB-INF/lib/" + child.getName() + ".jar";
							}
							IPath jarPath = new Path(childUri);
							File jarFile = new File(tempFile, jarPath.lastSegment());
							jarPath = jarPath.removeLastSegments(1);

							IModuleResource[] mr = server.getResources(new IModule[] { child });
							IStatus[] status = helper.publishZip(mr, new Path(jarFile.getAbsolutePath()), monitor);
							merge(result, status);
							resources.add(new ModuleFile(jarFile, jarFile.getName(), jarPath));
						}
					}
				}
			}

			List<IModuleResource> newResources = new ArrayList<IModuleResource>();
			for (IModuleResource mr : resources) {
				newResources.add(processModuleResource(mr));
			}

			IStatus[] status = helper.publishZip(newResources.toArray(new IModuleResource[0]),
					new Path(targetFile.getAbsolutePath()), monitor);
			merge(result, status);
			throwException(result, NLS.bind("Publishing of ''{0}'' failed", modules[0].getName()));

			return targetFile;
		}
		catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
					"Failed to create war file: {0}", e.getMessage()), e));
		}

	}

	private static IModuleResource processModuleResource(IModuleResource or) {
		if (or instanceof IModuleFolder) {
			IModuleFolder of = (IModuleFolder) or;
			IPath p = of.getModuleRelativePath();
			if (p.isAbsolute()) {
				p = p.makeRelative();
			}
			ModuleFolder nf = new ModuleFolder(null, of.getName(), p);
			List<IModuleResource> c = new ArrayList<IModuleResource>();
			for (IModuleResource mc : of.members()) {
				c.add(processModuleResource(mc));
			}
			nf.setMembers(c.toArray(new IModuleResource[0]));
			return nf;
		}
		return or;
	}

	public static String getValidationErrorMessage(CoreException e) {
		if (isForbiddenException(e)) {
			return "Validation failed: Wrong email or password";
		}
		else if (isUnknownHostException(e)) {
			return "Validation failed: Unable to establish connection";
		}
		else if (isRestClientException(e)) {
			return "Validation failed: Unknown URL";
		}

		return "Validation failed";
	}

	public static boolean isCloudFoundryServer(IServer server) {
		String serverId = server.getServerType().getId();
		return serverId.startsWith("org.cloudfoundry.appcloudserver.");
	}

	// check if error is caused by wrong credentials
	public static boolean isWrongCredentialsException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			if (statusCode.equals(HttpStatus.FORBIDDEN) && httpException instanceof CloudFoundryException) {
				return ((CloudFoundryException) httpException).getDescription().equals("Operation not permitted");
			}
		}
		return false;
	}

	public static CoreException toCoreException(Exception e) {
		if (e instanceof CloudFoundryException) {
			if (((CloudFoundryException) e).getDescription() != null) {
				return new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind("{0} ({1})",
						((CloudFoundryException) e).getDescription(), e.getMessage()), e));
			}
		}
		return new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
				"Communication with server failed: {0}", e.getMessage()), e));
	}

	// check if error is 403 - take CoreException
	public static boolean isForbiddenException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			return statusCode.equals(HttpStatus.FORBIDDEN);
		}
		return false;
	}

	// check if error is 404 - take CoreException
	public static boolean isNotFoundException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			return statusCode.equals(HttpStatus.NOT_FOUND);
		}
		return false;
	}

	public static boolean isUnknownHostException(CoreException e) {
		Throwable cause = e.getStatus().getException();
		if (cause instanceof ResourceAccessException) {
			return ((ResourceAccessException) cause).getCause() instanceof UnknownHostException;
		}
		return false;
	}

	public static boolean isRestClientException(CoreException e) {
		Throwable cause = e.getStatus().getException();
		return cause instanceof RestClientException;
	}

	public static void merge(List<IStatus> result, IStatus[] status) {
		if (result == null || status == null || status.length == 0) {
			return;
		}

		int size = status.length;
		for (int i = 0; i < size; i++) {
			result.add(status[i]);
		}
	}

	private static File getTempFolder(IModule module) throws IOException {
		File tempFile = File.createTempFile("tempFileForWar", null);
		tempFile.delete();
		tempFile.mkdirs();
		return tempFile;
	}

	protected static void throwException(List<IStatus> status, String message) throws CoreException {
		if (status == null || status.size() == 0) {
			return;
		}
		throw new CoreException(new MultiStatus(CloudFoundryPlugin.PLUGIN_ID, 0, status.toArray(new IStatus[0]),
				message, null));
	}

	public static IStatus[] publishZip(List<IModuleResource> allResources, File tempFile,
			Set<IModuleResource> filterInFiles, IProgressMonitor monitor) {

		monitor = ProgressUtil.getMonitorFor(monitor);

		try {
			BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(tempFile));
			ZipOutputStream zout = new ZipOutputStream(bout);
			addZipEntries(zout, allResources, filterInFiles);
			zout.close();

		}
		catch (CoreException e) {
			return new IStatus[] { e.getStatus() };
		}
		catch (Exception e) {

			return new Status[] { new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(
					Messages.errorCreatingZipFile, tempFile.getName(), e.getLocalizedMessage()), e) };
		}
		finally {
			if (tempFile != null && tempFile.exists())
				tempFile.deleteOnExit();
		}
		return EMPTY_STATUS;
	}

	private static final int BUFFER = 65536;

	private static byte[] buf = new byte[BUFFER];

	public static String getZipRelativeName(IModuleResource resource) {
		IPath path = resource.getModuleRelativePath().append(resource.getName());
		String entryPath = path.toPortableString();
		if (resource instanceof IModuleFolder && !entryPath.endsWith("/")) {
			entryPath += '/';
		}

		return entryPath;

	}

	private static void addZipEntries(ZipOutputStream out, List<IModuleResource> allResources,
			Set<IModuleResource> filterInFiles) throws Exception {
		if (allResources == null)
			return;

		for (IModuleResource resource : allResources) {
			if (resource instanceof IModuleFolder) {

				IModuleResource[] folderResources = ((IModuleFolder) resource).members();

				String entryPath = getZipRelativeName(resource);

				ZipEntry zipEntry = new ZipEntry(entryPath);

				long timeStamp = 0;
				IContainer folder = (IContainer) resource.getAdapter(IContainer.class);
				if (folder != null) {
					timeStamp = folder.getLocalTimeStamp();
				}

				if (timeStamp != IResource.NULL_STAMP && timeStamp != 0) {
					zipEntry.setTime(timeStamp);
				}

				out.putNextEntry(zipEntry);
				out.closeEntry();

				addZipEntries(out, Arrays.asList(folderResources), filterInFiles);
				continue;
			}

			IModuleFile moduleFile = (IModuleFile) resource;
			// Only add files that are in the filterInList
			if (!filterInFiles.contains(moduleFile)) {
				continue;
			}

			String entryPath = getZipRelativeName(resource);

			ZipEntry zipEntry = new ZipEntry(entryPath);

			InputStream input = null;
			long timeStamp = 0;
			IFile iFile = (IFile) moduleFile.getAdapter(IFile.class);
			if (iFile != null) {
				timeStamp = iFile.getLocalTimeStamp();
				input = iFile.getContents();
			}
			else {
				File file = (File) moduleFile.getAdapter(File.class);
				timeStamp = file.lastModified();
				input = new FileInputStream(file);
			}

			if (timeStamp != IResource.NULL_STAMP && timeStamp != 0) {
				zipEntry.setTime(timeStamp);
			}

			out.putNextEntry(zipEntry);

			try {
				int n = 0;
				while (n > -1) {
					n = input.read(buf);
					if (n > 0) {
						out.write(buf, 0, n);
					}
				}
			}
			finally {
				input.close();
			}

			out.closeEntry();
		}
	}

	private static boolean isLiftLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches("lift-webkit.*\\.jar", name);
		}
		return false;
	}

	private static boolean isSpringLibrary(IClasspathEntry entry) {
		if (entry.getPath() != null) {
			String name = entry.getPath().lastSegment();
			return Pattern.matches(".*spring.*\\.jar", name);
		}
		return false;
	}

	public static String getFramework(ApplicationModule module) {
		if (module != null && module.getLocalModule() != null) {
			IProject project = module.getLocalModule().getProject();
			String framework = getFramework(project);
			// If no framework can be determined from the project, assume a Java
			// web project
			if (framework != null) {
				return framework;
			}
		}

		return DeploymentConstants.JAVA_WEB;
	}

	public static String getFramework(IProject project) {
		if (project != null) {
			IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(project);
			if (javaProject != null) {
				if (CloudFoundryProjectUtil.hasNature(project, DeploymentConstants.GRAILS_NATURE)) {
					return DeploymentConstants.GRAILS;
				}

				// in case user has Grails projects without the nature
				// attached
				if (project.isAccessible() && project.getFolder("grails-app").exists()
						&& project.getFile("application.properties").exists()) {
					return DeploymentConstants.GRAILS;
				}

				IClasspathEntry[] entries;
				boolean foundSpringLibrary = false;
				try {
					entries = javaProject.getRawClasspath();
					for (IClasspathEntry entry : entries) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
							if (isLiftLibrary(entry)) {
								return DeploymentConstants.LIFT;
							}
							if (isSpringLibrary(entry)) {
								foundSpringLibrary = true;
							}
						}
						else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
							IClasspathContainer container = JavaCore
									.getClasspathContainer(entry.getPath(), javaProject);
							if (container != null) {
								for (IClasspathEntry childEntry : container.getClasspathEntries()) {
									if (isLiftLibrary(childEntry)) {
										return DeploymentConstants.LIFT;
									}
									if (isSpringLibrary(childEntry)) {
										foundSpringLibrary = true;
									}
								}
							}
						}
					}
				}
				catch (JavaModelException e) {

					CloudFoundryPlugin.logError(new Status(IStatus.WARNING, CloudFoundryPlugin.PLUGIN_ID,
							"Unexpected error during auto detection of application type", e));
				}

				if (CloudFoundryProjectUtil.isSpringProject(project)) {
					return DeploymentConstants.SPRING;
				}

				if (foundSpringLibrary) {
					return DeploymentConstants.SPRING;
				}
			}
		}
		return null;
	}
}
