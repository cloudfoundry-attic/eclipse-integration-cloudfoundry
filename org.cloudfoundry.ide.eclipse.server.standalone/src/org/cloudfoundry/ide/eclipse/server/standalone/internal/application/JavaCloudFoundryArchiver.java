/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.wst.server.core.IModule;
import org.springframework.boot.loader.tools.Libraries;
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

	private static final String META_FOLDER_NAME = "META-INF";

	private static final String MANIFEST_FILE = "MANIFEST.MF";

	private static final String NO_MANIFEST_ERROR = "No META-INF/MANIFEST.MF file found in source folders";

	public JavaCloudFoundryArchiver(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	public ApplicationArchive getApplicationArchive(IProgressMonitor monitor)
			throws CoreException {
		String archivePath = appModule.getDeploymentInfo().getArchive();

		// FIXNS:
		// Workaround to the fact that path manifest property does not get
		// persisted in the server for the application,
		// therefore if the deploymentinfo does not have it, parse it from the
		// manifest, if one can be found for the application
		// in a local project. The reason this is done here as opposed to when
		// deployment info is updated for an application
		// is that the path only gets used when pushing the application (either
		// initial push, or through start/update restart)
		// so it will keep manifest reading I/O only to these cases, rather than
		// on deployment info update, which occurs on
		// every refresh.
		if (archivePath == null) {
			archivePath = new ManifestParser(appModule, cloudServer)
					.getApplicationProperty(null, ManifestParser.PATH_PROP);
		}

		File packagedFile = null;
		if (archivePath != null) {
			// URLs should be project relative
			IPath path = new Path(archivePath);
			if (path.getFileExtension() != null) {
				IProject project = CloudFoundryProjectUtil
						.getProject(appModule);

				if (project != null) {
					IFile file = project.getFile(archivePath);
					if (file.exists()) {
						packagedFile = file.getLocation().toFile();
					}
				}
			}
		}

		if (packagedFile == null) {

			IJavaProject javaProject = CloudFoundryProjectUtil
					.getJavaProject(appModule);

			if (javaProject == null) {
				handleApplicationDeploymentFailure("No Java project resolved");
			}

			JavaPackageFragmentRootHandler rootResolver = getPackageFragmentRootHandler(
					javaProject, monitor);

			final IPackageFragmentRoot[] roots = rootResolver
					.getPackageFragmentRoots(monitor);
			IType mainType = rootResolver.getMainType();

			JarPackageData jarPackageData = getJarPackageData(roots, mainType,
					monitor);

			boolean isBoot = isBootProject(javaProject);

			if (!isBoot) {
				// If it is not a boot project, use a standard library jar
				// builder
				jarPackageData.setJarBuilder(getDefaultLibJarBuilder());

				// Search for META-INF in source package fragment roots
				IFile metaFile = getManifest(roots);

				if (metaFile != null) {

					jarPackageData.setManifestLocation(metaFile.getFullPath());
					jarPackageData.setSaveManifest(false);
					jarPackageData.setGenerateManifest(false);
					// Check manifest accessibility through the jar package data
					// API
					// to verify the packaging won't fail
					if (!jarPackageData.isManifestAccessible()) {
						handleApplicationDeploymentFailure(NO_MANIFEST_ERROR);
					}

					InputStream inputStream = null;
					try {

						inputStream = new FileInputStream(metaFile
								.getLocation().toFile());
						Manifest manifest = new Manifest(inputStream);
						Attributes att = manifest.getMainAttributes();
						if (att.getValue("Main-Class") == null) {
							handleApplicationDeploymentFailure("No Main Class found in manifest file");
						}
					} catch (FileNotFoundException e) {
						// Dont terminate deployment, just log error
						CloudFoundryPlugin.logError(e);
					} catch (IOException e) {
						// Dont terminate deployment, just log error
						CloudFoundryPlugin.logError(e);
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
					handleApplicationDeploymentFailure(NO_MANIFEST_ERROR);
				}

			} else {
				// Otherwise use the default jar builder which does not handle
				// exporting jar dependencies,
				// as the boot repackage will repackage the jar dependencies
				// separately.
				// Also generated manifest for spring boot, although it will
				// later be edited by the boot repackager to include a spring
				// boot loader
				jarPackageData.setGenerateManifest(true);
			}

			try {
				packagedFile = packageApplication(jarPackageData, monitor);
			} catch (CoreException e) {
				handleApplicationDeploymentFailure("Java application packaging failed - "
						+ e.getMessage());
			}

			if (packagedFile == null || !packagedFile.exists()) {
				handleApplicationDeploymentFailure("Java application packaging failed. No packaged file was created");
			}

			if (isBoot) {
				bootRepackage(roots, packagedFile);
			}
		}

		// At this stage a packaged file should have been created or found
		try {
			return new ZipApplicationArchive(new ZipFile(packagedFile));
		} catch (IOException ioe) {
			handleApplicationDeploymentFailure("Error creating Cloud Foundry archive due to - "
					+ ioe.getMessage());
		}
		return null;
	}

	protected IFolder getMetaFolder(IResource resource) throws CoreException {
		if (!(resource instanceof IFolder)) {
			return null;
		}
		IFolder folder = (IFolder) resource;
		// META-INF can only be contained in a source folder at root level
		// relative
		// to that source folder, since it needs to appear in the packaged app
		// at root level
		// (source folders themselves do not appear in the jar structure, only
		// their content).
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

	protected IFile getManifest(IPackageFragmentRoot[] roots)
			throws CoreException {
		for (IPackageFragmentRoot root : roots) {
			if (!root.isArchive() && !root.isExternal()) {
				IResource resource = root.getResource();

				IFolder metaFolder = getMetaFolder(resource);
				if (metaFolder != null) {
					IResource[] members = metaFolder.members();
					if (members != null) {
						for (IResource mem : members) {
							if (MANIFEST_FILE.equals(mem.getName()
									.toUpperCase()) && mem instanceof IFile) {
								return (IFile) mem;
							}
						}
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
								callBack.library(rootFile, LibraryScope.COMPILE);
							}
						}
					}
				}
			});
		} catch (IOException e) {
			handleApplicationDeploymentFailure("Failed to repackage Spring boot application due to "
					+ e.getMessage());
		}
	}

	protected JarPackageData getJarPackageData(IPackageFragmentRoot[] roots,
			IType mainType, IProgressMonitor monitor) throws CoreException {
		if (roots == null || roots.length == 0) {
			handleApplicationDeploymentFailure("No package fragment roots found");
		}

		if (mainType == null) {
			handleApplicationDeploymentFailure("No main type found");
		}

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

	protected File packageApplication(JarPackageData packageData,
			IProgressMonitor monitor) throws CoreException {

		int progressWork = 10;
		SubMonitor subProgress = SubMonitor.convert(monitor, progressWork);

		IJarExportRunnable runnable = packageData.createJarExportRunnable(null);
		try {
			runnable.run(monitor);
			File file = new File(packageData.getJarLocation().toString());
			if (!file.exists()) {
				handleApplicationDeploymentFailure();
			} else {
				return file;
			}

		} catch (InvocationTargetException e) {
			throw CloudErrorUtil.toCoreException(e);
		} catch (InterruptedException ie) {
			throw CloudErrorUtil.toCoreException(ie);
		} finally {
			subProgress.done();
		}

		return null;
	}

	protected void handleApplicationDeploymentFailure(String errorMessage)
			throws CoreException {
		if (errorMessage == null) {
			errorMessage = "Failed to create packaged file";
		}
		throw CloudErrorUtil.toCoreException(errorMessage + " - "
				+ appModule.getDeployedApplicationName()
				+ ". Unable to package application for deployment.");
	}

	protected void handleApplicationDeploymentFailure() throws CoreException {
		handleApplicationDeploymentFailure(null);
	}

	public static String getTempJarPath(IModule module) throws CoreException {
		try {
			File tempFolder = File.createTempFile("tempFolderForJavaAppJar",
					null);
			tempFolder.delete();
			tempFolder.mkdirs();

			if (!tempFolder.exists()) {
				throw CloudErrorUtil
						.toCoreException("Failed to created temporary directory when packaging application for deployment. Check permissions at: "
								+ tempFolder.getPath());
			}

			File targetFile = new File(tempFolder, module.getName() + ".jar");
			targetFile.deleteOnExit();

			String path = new Path(targetFile.getAbsolutePath()).toString();

			CloudFoundryPlugin.log(CloudFoundryPlugin.getStatus(
					"Created temporary jar for " + module.getName() + " - "
							+ path, IStatus.INFO));
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
			return name.endsWith(".jar") && name.startsWith("spring-boot");
		}
		return false;
	}
}