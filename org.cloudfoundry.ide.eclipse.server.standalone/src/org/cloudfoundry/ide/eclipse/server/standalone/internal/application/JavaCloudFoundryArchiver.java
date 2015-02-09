/*******************************************************************************
 * Copyright (c) 2013, 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ManifestParser;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.standalone.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarRsrcUrlBuilder;
import org.eclipse.jdt.ui.jarpackager.IJarBuilder;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;

/**
 * Generates a Cloud Foundry client archive represent the Java application that
 * should be pushed to a Cloud Foundry server.
 * <p/>
 * Handles Spring boot application repackaging via Spring Boot loader tools
 * <p/>
 * Also supports packaged apps pointed to by the "path" property in an
 * application's manifest.yml
 * 
 */
public class JavaCloudFoundryArchiver {

	private final CloudFoundryApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	private static final String META_FOLDER_NAME = "META-INF"; //$NON-NLS-1$

	private static final String MANIFEST_FILE = "MANIFEST.MF"; //$NON-NLS-1$

	public JavaCloudFoundryArchiver(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	public ApplicationArchive getApplicationArchive(IProgressMonitor monitor)
			throws CoreException {

		String archivePath = null;
		ManifestParser parser = new ManifestParser(appModule, cloudServer);
		// Read the path again instead of deployment info, as a user may be
		// correcting the path after the module was creating and simply attempting to push it again without the
		// deployment wizard
		if (parser.hasManifest()) {
			archivePath = parser.getApplicationProperty(null,
					ManifestParser.PATH_PROP);
		}

		File packagedFile = null;
		if (archivePath != null) {
			// Only support paths that point to archive files
			IPath path = new Path(archivePath);
			if (path.getFileExtension() != null) {
				// Check if it is project relative first
				IFile projectRelativeFile = null;
				IProject project = CloudFoundryProjectUtil
						.getProject(appModule);

				if (project != null) {
					projectRelativeFile = project.getFile(archivePath);
				}

				if (projectRelativeFile != null && projectRelativeFile.exists()) {
					packagedFile = projectRelativeFile.getLocation().toFile();
				} else {
					// See if it is an absolute path
					File absoluteFile = new File(archivePath);
					if (absoluteFile.exists() && absoluteFile.canRead()) {
						packagedFile = absoluteFile;
					}
				}
			}
			// If a path is specified but no file found stop further deployment
			if (packagedFile == null) {
				String message = NLS
						.bind(Messages.JavaCloudFoundryArchiver_ERROR_FILE_NOT_FOUND_MANIFEST_YML,
								archivePath);
				throw CloudErrorUtil.toCoreException(message);
			}
		}

		if (packagedFile == null) {

			IJavaProject javaProject = CloudFoundryProjectUtil
					.getJavaProject(appModule);

			if (javaProject == null) {
				handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_JAVA_PROJ_RESOLVED);
			}

			JavaPackageFragmentRootHandler rootResolver = getPackageFragmentRootHandler(
					javaProject, monitor);

			IType mainType = rootResolver.getMainType(monitor);

			final IPackageFragmentRoot[] roots = rootResolver
					.getPackageFragmentRoots(monitor);

			if (roots == null || roots.length == 0) {
				handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_PACKAGE_FRAG_ROOTS);
			}

			JarPackageData jarPackageData = getJarPackageData(roots, mainType,
					monitor);

			boolean isBoot = isBootProject(javaProject);

			// Search for existing MANIFEST.MF
			IFile metaFile = getManifest(roots, javaProject);

			// Only use existing manifest files for non-Spring boot, as Spring
			// boot repackager will
			// generate it own manifest file.
			if (!isBoot && metaFile != null) {
				// If it is not a boot project, use a standard library jar
				// builder
				jarPackageData.setJarBuilder(getDefaultLibJarBuilder());

				jarPackageData.setManifestLocation(metaFile.getFullPath());
				jarPackageData.setSaveManifest(false);
				jarPackageData.setGenerateManifest(false);
				// Check manifest accessibility through the jar package data
				// API
				// to verify the packaging won't fail
				if (!jarPackageData.isManifestAccessible()) {
					handleApplicationDeploymentFailure(NLS
							.bind(Messages.JavaCloudFoundryArchiver_ERROR_MANIFEST_NOT_ACCESSIBLE,
									metaFile.getLocation().toString()));
				}

				InputStream inputStream = null;
				try {

					inputStream = new FileInputStream(metaFile.getLocation()
							.toFile());
					Manifest manifest = new Manifest(inputStream);
					Attributes att = manifest.getMainAttributes();
					if (att.getValue("Main-Class") == null) { //$NON-NLS-1$
						handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_MAIN_CLASS_IN_MANIFEST);
					}
				} catch (FileNotFoundException e) {
					handleApplicationDeploymentFailure(NLS
							.bind(Messages.JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST,
									e.getLocalizedMessage()));

				} catch (IOException e) {
					handleApplicationDeploymentFailure(NLS
							.bind(Messages.JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST,
									e.getLocalizedMessage()));

				} finally {

					if (inputStream != null) {
						try {
							inputStream.close();

						} catch (IOException io) {
							// Ignore
						}
					}
				}

			} else {
				// Otherwise generate a manifest file. Note that manifest files
				// are only generated in the temporary jar meant only for
				// deployment.
				// The associated Java project is no modified.
				jarPackageData.setGenerateManifest(true);

				// This ensures that folders in output folders appear at root
				// level
				// Example: src/main/resources, which is in the project's
				// classpath, contains non-Java templates folder and
				// has output folder target/classes. If not exporting output
				// folder,
				// templates will be packaged in the jar using this path:
				// resources/templates
				// This may cause problems with the application's dependencies
				// if they are looking for just /templates at top level of the
				// jar
				// If exporting output folders, templates folder will be
				// packaged at top level in the jar.
				jarPackageData.setExportOutputFolders(true);
			}

			try {
				packagedFile = packageApplication(jarPackageData, monitor);
			} catch (CoreException e) {
				handleApplicationDeploymentFailure(NLS
						.bind(Messages.JavaCloudFoundryArchiver_ERROR_JAVA_APP_PACKAGE,
								e.getMessage()));
			}

			if (packagedFile == null || !packagedFile.exists()) {
				handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_PACKAGED_FILE_CREATED);
			}

			if (isBoot) {
				bootRepackage(roots, packagedFile);
			}
		}

		// At this stage a packaged file should have been created or found
		try {
			return new ZipApplicationArchive(new ZipFile(packagedFile));
		} catch (IOException ioe) {
			handleApplicationDeploymentFailure(NLS.bind(
					Messages.JavaCloudFoundryArchiver_ERROR_CREATE_CF_ARCHIVE,
					ioe.getMessage()));
		}
		return null;
	}

	/**
	 * 
	 * @param resource
	 *            that may contain a META-INF folder
	 * @return META-INF folder, if found. Null otherwise
	 * @throws CoreException
	 */
	protected IFolder getMetaFolder(IResource resource) throws CoreException {
		if (!(resource instanceof IContainer)) {
			return null;
		}
		IContainer folder = (IContainer) resource;
		// Only look for META-INF folder at top-level in the given container.
		IResource[] members = folder.members();
		if (members != null) {
			for (IResource mem : members) {
				if (META_FOLDER_NAME.equals(mem.getName())
						&& mem instanceof IFolder) {
					return (IFolder) mem;
				}
			}
		}
		return null;
	}

	protected IFile getManifest(IPackageFragmentRoot[] roots,
			IJavaProject javaProject) throws CoreException {

		IFolder metaFolder = null;
		for (IPackageFragmentRoot root : roots) {
			if (!root.isArchive() && !root.isExternal()) {
				IResource resource = root.getResource();
				metaFolder = getMetaFolder(resource);
				if (metaFolder != null) {
					break;
				}
			}
		}

		// Otherwise look for manifest file in the java project:
		if (metaFolder == null) {
			metaFolder = getMetaFolder(javaProject.getProject());
		}

		if (metaFolder != null) {
			IResource[] members = metaFolder.members();
			if (members != null) {
				for (IResource mem : members) {
					if (MANIFEST_FILE.equals(mem.getName().toUpperCase())
							&& mem instanceof IFile) {
						return (IFile) mem;
					}
				}
			}
		}

		return null;

	}

	protected IJarBuilder getDefaultLibJarBuilder() {
		return new FatJarRsrcUrlBuilder() {

			public void writeRsrcUrlClasses() throws IOException {
				// Do not unpack and repackage the Eclipse jar loader
			}
		};
	}

	protected JavaPackageFragmentRootHandler getPackageFragmentRootHandler(
			IJavaProject javaProject, IProgressMonitor monitor)
			throws CoreException {

		return new JavaPackageFragmentRootHandler(javaProject, null);
	}

	protected void bootRepackage(final IPackageFragmentRoot[] roots,
			File packagedFile) throws CoreException {
		Repackager bootRepackager = new Repackager(packagedFile);
		try {
			bootRepackager.repackage(new Libraries() {

				public void doWithLibraries(LibraryCallback callBack)
						throws IOException {
					for (IPackageFragmentRoot root : roots) {

						if (root.isArchive()) {

							File rootFile = new File(root.getPath()
									.toOSString());
							if (rootFile.exists()) {
								callBack.library(new Library(rootFile,
										LibraryScope.COMPILE));
							}
						}
					}
				}
			});
		} catch (IOException e) {
			handleApplicationDeploymentFailure(NLS.bind(
					Messages.JavaCloudFoundryArchiver_ERROR_REPACKAGE_SPRING,
					e.getMessage()));
		}
	}

	protected JarPackageData getJarPackageData(IPackageFragmentRoot[] roots,
			IType mainType, IProgressMonitor monitor) throws CoreException {

		String filePath = getTempJarPath(appModule.getLocalModule());

		if (filePath == null) {
			handleApplicationDeploymentFailure();
		}

		IPath location = new Path(filePath);

		// Note that if no jar builder is specified in the package data
		// then a default one is used internally by the data that does NOT
		// package any jar dependencies.
		JarPackageData packageData = new JarPackageData();

		packageData.setJarLocation(location);

		// Don't create a manifest. A repackager should determine if a generated
		// manifest is necessary
		// or use a user-defined manifest.
		packageData.setGenerateManifest(false);

		// Since user manifest is not used, do not save to manifest (save to
		// manifest saves to user defined manifest)
		packageData.setSaveManifest(false);

		packageData.setManifestMainClass(mainType);
		packageData.setElements(roots);
		return packageData;
	}

	protected File packageApplication(final JarPackageData packageData,
			IProgressMonitor monitor) throws CoreException {

		int progressWork = 10;
		final SubMonitor subProgress = SubMonitor
				.convert(monitor, progressWork);

		final File[] createdFile = new File[1];

		final CoreException[] error = new CoreException[1];
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				try {

					Shell shell = CloudUiUtil.getShell();

					IJarExportRunnable runnable = packageData
							.createJarExportRunnable(shell);
					try {
						runnable.run(subProgress);

						File file = new File(packageData.getJarLocation()
								.toString());
						if (!file.exists()) {
							handleApplicationDeploymentFailure();
						} else {
							createdFile[0] = file;
						}

					} catch (InvocationTargetException e) {
						throw CloudErrorUtil.toCoreException(e);
					} catch (InterruptedException ie) {
						throw CloudErrorUtil.toCoreException(ie);
					} finally {
						subProgress.done();
					}
				} catch (CoreException e) {
					error[0] = e;
				}
			}

		});
		if (error[0] != null) {
			throw error[0];
		}

		return createdFile[0];
	}

	protected void handleApplicationDeploymentFailure(String errorMessage)
			throws CoreException {
		if (errorMessage == null) {
			errorMessage = Messages.JavaCloudFoundryArchiver_ERROR_CREATE_PACKAGED_FILE;
		}
		throw CloudErrorUtil.toCoreException(errorMessage
				+ " - " //$NON-NLS-1$
				+ appModule.getDeployedApplicationName()
				+ ". Unable to package application for deployment."); //$NON-NLS-1$
	}

	protected void handleApplicationDeploymentFailure() throws CoreException {
		handleApplicationDeploymentFailure(null);
	}

	public static String getTempJarPath(IModule module) throws CoreException {
		try {
			File tempFolder = File.createTempFile("tempFolderForJavaAppJar", //$NON-NLS-1$
					null);
			tempFolder.delete();
			tempFolder.mkdirs();

			if (!tempFolder.exists()) {
				throw CloudErrorUtil
						.toCoreException(NLS
								.bind(Messages.JavaCloudFoundryArchiver_ERROR_CREATE_TEMP_DIR,
										tempFolder.getPath()));
			}

			File targetFile = new File(tempFolder, module.getName() + ".jar"); //$NON-NLS-1$
			targetFile.deleteOnExit();

			String path = new Path(targetFile.getAbsolutePath()).toString();

			return path;

		} catch (IOException io) {
			CloudErrorUtil.toCoreException(io);
		}
		return null;
	}

	/*
	 * Derived from org.springframework.ide.eclipse.boot.core.BootPropertyTester
	 * 
	 * FIXNS: Remove when boot detection is moved to a common STS plug-in that
	 * can be shared with CF Eclipse.
	 */
	public static boolean isBootProject(IJavaProject project) {
		if (project == null) {
			return false;
		}
		try {
			IClasspathEntry[] classpath = project.getResolvedClasspath(true);
			// Look for a 'spring-boot' jar entry
			for (IClasspathEntry e : classpath) {
				if (isBootJar(e)) {
					return true;
				}
			}
		} catch (Exception e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	private static boolean isBootJar(IClasspathEntry e) {
		if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
			IPath path = e.getPath();
			String name = path.lastSegment();
			return name.endsWith(".jar") && name.startsWith("spring-boot"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return false;
	}
}