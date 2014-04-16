/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "LicenseÓ); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.tests.sts.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Kris De Volder
 */
public class StsTestUtil {

	public static final boolean ECLIPSE_3_4 = Platform.getBundle("org.eclipse.equinox.p2.repository") == null;

	public static final boolean ECLIPSE_3_6_OR_LATER;

	public static final boolean ECLIPSE_3_7_OR_LATER;
	static {
		ECLIPSE_3_6_OR_LATER = isEclipseVersionAtLeast(new Version(3, 6, 0));
		ECLIPSE_3_7_OR_LATER = isEclipseVersionAtLeast(new Version(3, 7, 0));
	}

	public static boolean isEclipseVersionAtLeast(Version minimalVersion) {
		System.err.println("StsTestUtil: " + minimalVersion + " or later? ...");
		boolean found = false;
		try {
			Bundle platformBundle = Platform.getBundle("org.eclipse.platform");
			System.err.println("platform bundle: " + platformBundle);
			Version version = platformBundle.getVersion();
			System.err.println("platform bundle version: " + version);
			if (version.compareTo(minimalVersion) >= 0) {
				found = true;
			}
		}
		catch (Throwable e) {
			System.err.println("StsTestUtil: Couldn't determine Eclipse version");
			e.printStackTrace(System.err);
		}
		System.err.println("StsTestUtil: " + minimalVersion + " or later? => " + found);
		return found;
	}

	public static final long WAIT_TIME = 2000;

	public static String canocalizeXml(String originalServerXml) throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(originalServerXml)));
		document.normalize();

		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		StringWriter writer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(new DOMSource(document.getDocumentElement()), new StreamResult(writer));
		return writer.toString().replace("\\s+\\n", "\\n");
	}

	public static void cleanUpProjects() throws CoreException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				page.closeAllEditors(false);
			}
		}
		deleteAllProjects();
	}

	/**
	 * Copy file from src (path to the original file) to dest (path to the
	 * destination file).
	 */
	private static void copy(File src, File dest) throws IOException {
		InputStream in = new FileInputStream(src);
		try {
			OutputStream out = new FileOutputStream(dest);
			try {
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}

	/**
	 * Copy the given source directory (and all its contents) to the given
	 * target directory.
	 */
	private static void copyDirectory(File source, File target) throws IOException {
		if (!target.exists()) {
			target.mkdirs();
		}
		File[] files = source.listFiles();
		if (files == null) {
			return;
		}
		for (File sourceChild : files) {
			String name = sourceChild.getName();
			if (name.equals(".svn")) {
				continue;
			}
			File targetChild = new File(target, name);
			if (sourceChild.isDirectory()) {
				copyDirectory(sourceChild, targetChild);
			}
			else {
				copy(sourceChild, targetChild);
			}
		}
	}

	public static IProject createPredefinedProject(final String projectName, String bundleName) throws CoreException,
			IOException {
		IJavaProject jp = setUpJavaProject(projectName, bundleName);
		StsTestUtil.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		return jp.getProject();
	}

	public static File createTempDirectory() throws IOException {
		return createTempDirectory("sts", null);
	}

	public static File createTempDirectory(String prefix, String suffix) throws IOException {
		File file = File.createTempFile(prefix, suffix);
		file.delete();
		file.mkdirs();
		return file;
	}

	public static void deleteAllProjects() throws CoreException {
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		boolean deleted = false;
		for (IProject project : allProjects) {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			deleteProject(project, true);
			deleted = true;
		}

		if (deleted) {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		}
	}

	public static void deleteProject(IProject project, boolean force) throws CoreException {
		if (project.exists() && !project.isOpen()) {
			// force opening so that project can be deleted without logging (see
			// bug 23629)
			project.open(null);
		}
		deleteResource(project, force);
	}

	/**
	 * Delete this resource.
	 */
	private static void deleteResource(IResource resource, boolean force) throws CoreException {
		if (!resource.exists() || !resource.isAccessible()) {
			return;
		}
		waitForManualBuild();
		waitForAutoBuild();
		CoreException lastException = null;
		try {
			resource.delete(force, null);
		}
		catch (CoreException e) {
			lastException = e;
			// just print for info
			System.out.println("(CoreException): " + e.getMessage() + " Resource " + resource.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
		catch (IllegalArgumentException iae) {
			// just print for info
			System.out
					.println("(IllegalArgumentException): " + iae.getMessage() + ", resource " + resource.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!force) {
			return;
		}
		int retryCount = 10; // wait 1 minute at most
		while (resource.isAccessible() && --retryCount >= 0) {
			waitForAutoBuild();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
			}
			try {
				resource.delete(true, null);
			}
			catch (CoreException e) {
				lastException = e;
				// just print for info
				System.out
						.println("(CoreException) Retry " + retryCount + ": " + e.getMessage() + ", resource " + resource.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			catch (IllegalArgumentException iae) {
				// just print for info
				System.out
						.println("(IllegalArgumentException) Retry " + retryCount + ": " + iae.getMessage() + ", resource " + resource.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		if (!resource.isAccessible()) {
			return;
		}
		System.err.println("Failed to delete " + resource.getFullPath()); //$NON-NLS-1$
		if (lastException != null) {
			throw lastException;
		}
	}

	public static File getBundlePath(String pluginId) throws IOException {
		URL platformURL = Platform.getBundle(pluginId).getEntry("/"); //$NON-NLS-1$
		return new File(FileLocator.toFileURL(platformURL).getFile());
	}

	public static File getFilePath(String pluginId, String segment) throws IOException {
		URL platformURL = Platform.getBundle(pluginId).getEntry(segment);
		Assert.assertNotNull("Couldn't find file '" + segment + "' in bundle '" + pluginId + "'", platformURL);
		return new File(FileLocator.toFileURL(platformURL).getFile());
	}

	public static String getMarkerMessages(IMarker[] markers) throws CoreException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < markers.length; i++) {
			IMarker currMarker = markers[i];
			String message = (String) currMarker.getAttribute("message");
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(message);
		}
		return sb.toString();
	}

	/**
	 * Returns the OS path to the directory that contains this plugin.
	 */
	private static String getPluginDirectoryPath(String bundleName) {
		try {
			URL platformURL = Platform.getBundle(bundleName).getEntry("/"); //$NON-NLS-1$
			return new File(FileLocator.toFileURL(platformURL).getFile()).getAbsolutePath();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static IProject getProject(String project) {
		return getWorkspaceRoot().getProject(project);
	}

	/**
	 * Get an IResource indicated by a given path starting at the workspace
	 * root.
	 * <p>
	 * Different type of resource is returned based on the length of the path
	 * and whether or not it ends with a path separator.
	 */
	public static IResource getResource(IPath path) {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (path.segmentCount() == 0) {
				return root;
			}
			IProject project = root.getProject(path.segment(0));
			if (path.segmentCount() == 1) {
				return project;
			}
			if (path.hasTrailingSeparator()) {
				return root.getFolder(path);
			}
			else {
				return root.getFile(path);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get an IResource from a path String starting at the workspace root.
	 * <p>
	 * Different type of resource is returned based on the length of the path
	 * and whether or not it ends with a path separator.
	 * <p>
	 * For example
	 * 
	 * "" length = 0 => type of resource is IWorkspaceRoot "foo" length = 1 =>
	 * type of resource is IProject "foo/src/Foo.java" length > 1 and no
	 * trailing "/" => type is IFile
	 * "foo/src/          length > 1 and a trailing "/" => type is IFolder
	 */
	public static IResource getResource(String pathToFile) {
		return getResource(Path.ROOT.append(pathToFile));
	}

	static String getSourceWorkspacePath(String bundleName) {
		return getPluginDirectoryPath(bundleName) + java.io.File.separator + "workspace"; //$NON-NLS-1$
	}

	/**
	 * Returns the IWorkspace this test suite is running on.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static IWorkspaceRoot getWorkspaceRoot() {
		return getWorkspace().getRoot();
	}

	// public static void saveAndWaitForEditor(final IEditorPart editor) throws
	// CoreException {
	// Display.getDefault().syncExec(new Runnable() {
	//
	// public void run() {
	// editor.doSave(null);
	// }
	// });
	// waitForEditor(editor);
	// }

	private static IJavaProject setUpJavaProject(final String projectName, String bundleName) throws CoreException,
			IOException {
		return StsTestUtil.setUpJavaProject(projectName, "1.4", getSourceWorkspacePath(bundleName)); //$NON-NLS-1$
	}

	public static IJavaProject setUpJavaProject(final String projectName, String compliance, String sourceWorkspacePath)
			throws CoreException, IOException {
		IProject project = setUpProject(projectName, compliance, sourceWorkspacePath);
		IJavaProject javaProject = JavaCore.create(project);
		return javaProject;
	}

	public static IProject setUpProject(final String projectName, String compliance, String sourceWorkspacePath)
			throws CoreException, IOException {
		// copy files in project from source workspace to target workspace
		String targetWorkspacePath = getWorkspaceRoot().getLocation().toFile().getCanonicalPath();
		copyDirectory(new File(sourceWorkspacePath, projectName), new File(targetWorkspacePath, projectName));

		// create project
		final IProject project = getWorkspaceRoot().getProject(projectName);
		IWorkspaceRunnable populate = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.create(null);
				try {
					project.open(null);
				}
				catch (ConcurrentModificationException e) {
					// wait and try again to work-around
					// ConcurrentModificationException (bug 280488)
					try {
						Thread.sleep(500);
						project.open(null);
						project.refreshLocal(IResource.DEPTH_INFINITE, null);
					}
					catch (InterruptedException e1) {
						Thread.currentThread().interrupt();
					}
				}
			}
		};
		getWorkspace().run(populate, null);
		return project;
	}

	/**
	 * Wait for autobuild notification to occur
	 */
	public static void waitForAutoBuild() {
		waitForJobFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
	}

	/**
	 * Allows Display to process events, so UI can make progress. Tests running
	 * in the UI thread may need to call this to avoid UI deadlocks.
	 * <p>
	 * For convenience, it is allowed to call this method from a non UI thread,
	 * but such calls have no effect.
	 */
	public static void waitForDisplay() {
		if (inUIThread()) {
			while (Display.getDefault().readAndDispatch()) {
				// do nothing
			}
		}
	}

	// public static void waitForEditor(IEditorPart editor) throws CoreException
	// {
	// IFileEditorInput editorInput = (IFileEditorInput)
	// editor.getEditorInput();
	// IFile file = editorInput.getFile();
	// waitForResource(file);
	// }

	public static void waitForJobFamily(Object jobFamily) {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(jobFamily, null);
				wasInterrupted = false;
			}
			catch (OperationCanceledException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}

	public static void waitForManualBuild() {
		waitForJobFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
	}

	public static void waitForResource(IResource resource) throws CoreException {
		waitForAutoBuild();
		waitForManualBuild();
		waitForJobFamily(ResourcesPlugin.FAMILY_AUTO_REFRESH);
		waitForJobFamily(ResourcesPlugin.FAMILY_MANUAL_REFRESH);
		resource.refreshLocal(IResource.DEPTH_ONE, null);
	}

	public static void setAutoBuilding(boolean enabled) throws CoreException {
		IWorkspaceDescription wsd = getWorkspace().getDescription();
		if (!wsd.isAutoBuilding() == enabled) {
			wsd.setAutoBuilding(enabled);
			getWorkspace().setDescription(wsd);
		}
	}

	public static void assertNoErrors(IProject project) throws CoreException {
		setAutoBuilding(false);
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForManualBuild();
		waitForAutoBuild();

		IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		for (IMarker problem : problems) {
			if (problem.getAttribute(IMarker.SEVERITY, 0) >= IMarker.SEVERITY_ERROR) {
				Assert.fail("Expecting no problems but found: " + markerMessage(problem));
			}
		}
	}

	public static String markerMessage(IMarker m) throws CoreException {
		StringBuffer msg = new StringBuffer("Marker {\n");
		final Map attributes = m.getAttributes();
		IResource rsrc = m.getResource();
		msg.append("   rsrc = " + (rsrc == null ? "unknown" : rsrc.getFullPath() + "\n"));
		for (Object atrName : attributes.keySet()) {
			msg.append("   " + atrName + " = " + attributes.get(atrName) + "\n");
		}
		msg.append("}");
		if (rsrc != null) {
			if (rsrc.getType() == IResource.FILE) {
				IFile file = (IFile) rsrc;
				if (isGroovyOrJava(file)) {
					InputStream content = file.getContents();
					if (content != null) {
						try {
							msg.append(">>>>>>>>> " + file.getFullPath() + "\n");
							BufferedReader reader = new BufferedReader(new InputStreamReader(content));
							String line = reader.readLine();
							int lineNumber = 1;
							while (line != null) {
								msg.append(String.format("%3d", lineNumber++) + ": " + line);
								line = reader.readLine();
							}
						}
						catch (IOException e) {
							msg.append("error reading file: (" + e.getClass().getName() + ") " + e.getMessage());
						}
						finally {
							msg.append("<<<<<<<<< " + file.getFullPath() + "\n");
							if (content != null) {
								try {
									content.close();
								}
								catch (IOException e) {
								}
							}
						}
					}
				}
			}
		}
		return msg.toString();
		// return m.getAttribute(IMarker.MESSAGE, "") + " line: " +
		// m.getAttribute(IMarker.LINE_NUMBER, "unknown")
		// + " location: " + m.getAttribute(IMarker.LOCATION, "unknown");
	}

	private static boolean isGroovyOrJava(IFile file) {
		String ext = file.getFileExtension();
		return "groovy".equals(ext) || "java".equals(ext);
	}

	public static boolean inUIThread() {
		return Display.getDefault().getThread() == Thread.currentThread();
	}

	/**
	 * Returns a port number that is available to start a server on.
	 */
	public static int findFreeSocketPort() throws IOException {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0); // port=0 will bind to a free port.
			int port = socket.getLocalPort();
			return port;
		}
		finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	/**
	 * Rethrow a 'Throwable' exception if it isn't null, without forcing calling
	 * method to declare throwing a Throwable (which really doesn't make much
	 * sense).
	 * @throws Exception
	 */
	public static void rethrow(Throwable e) throws Exception {
		if (e == null) {
			return;
		}
		else if (e instanceof Exception) {
			throw (Exception) e;
		}
		else if (e instanceof Error) {
			// There are only two kinds of throwables, so this must be an
			// unchecked runtime Exception
			throw (Error) e;
		}
		else {
			// This really shouldn't be possible... but just in case...
			throw new Error(e);
		}
	}

	/**
	 * Determines if we are running on an automated build machine or somewhere
	 * else.
	 */
	public static boolean isOnBuildSite() {
		if (isOnBuildSite == null) {
			String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toString();
			isOnBuildSite = workspacePath.contains("com.springsource.sts.releng") || workspacePath.contains("bamboo");
		}
		return isOnBuildSite;
	}

	private static Boolean isOnBuildSite;

	/**
	 * Set a bunch of preferences so that m2eclipse hopefully isn't doing a lot
	 * of time consuming stuff in the background.
	 */
	public static void mavenOffline() throws Error {
		System.out.println("Pacifying m2eclipse...");
		IEclipsePreferences m2EclipsePrefs = new InstanceScope().getNode("org.eclipse.m2e.core");
		m2EclipsePrefs.putBoolean("eclipse.m2.offline", true);
		m2EclipsePrefs.putBoolean("eclipse.m2.globalUpdatePolicy", false);
		m2EclipsePrefs.putBoolean("eclipse.m2.updateIndexes", false);
		try {
			m2EclipsePrefs.flush();
		}
		catch (BackingStoreException e) {
			throw new Error(e);
		}

		// LegacyProjectChecker.NON_BLOCKING = true;
		System.out.println("Pacifying m2eclipse...DONE");
	}

	// public static boolean isRunningOnBuildServer() {
	// List<Proxy> proxies = ProxySelector.getDefault().select(new
	// URI("http://springsource.com");
	// if (proxies != null && proxies.size() > 0) {
	// return
	// "HTTP @ proxy.eng.Pivotal Software.com:3128".equals(proxies.get(0).toString());
	// }
	// return false;
	// }

}
