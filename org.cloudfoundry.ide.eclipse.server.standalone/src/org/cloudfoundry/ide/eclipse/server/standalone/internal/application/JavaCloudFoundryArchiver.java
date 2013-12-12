/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ManifestParser;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
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
					.getApplicationProperty(null,
							ManifestParser.RELATIVE_APP_PATH);
		}

		File packagedFile = null;
		if (archivePath != null) {
			// For now assume urls are project relative
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

			// If it is spring boot, use spring boot repackager. Otherwise,
			// package
			// using Eclipse
			if (isBootProject(javaProject)) {
				// The package fragment roots should also list all the jars

				packagedFile = packageApplication(roots, mainType, monitor);

				if (packagedFile != null) {
					bootRepackage(roots, packagedFile);
				} else {
					handleApplicationDeploymentFailure("Spring boot packaging failed. No packaged file was created");
				}

			} else {
				// FIXNS: Create an archive file based on the package fragment
				// roots.
				// This would be for any other Java application
			}
		}

		if (packagedFile != null && packagedFile.exists()) {
			try {
				return new ZipApplicationArchive(new ZipFile(packagedFile));
			} catch (IOException ioe) {
				throw CloudErrorUtil.toCoreException(ioe);
			}
		} else {
			throw CloudErrorUtil
					.toCoreException("Application is not a valid Java application - "
							+ appModule.getDeployedApplicationName()
							+ ". Unable to deploy application.");
		}

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

	protected File packageApplication(IPackageFragmentRoot[] roots,
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

		// Don't create a manifest. A repackager should set a manifest
		packageData.setGenerateManifest(true);

		// Since user manifest is not used, do not save to manifest (save to
		// manifest saves to user defined manifest)
		packageData.setSaveManifest(false);

		packageData.setManifestMainClass(mainType);
		packageData.setElements(roots);

		int progressWork = 10;
		SubMonitor subProgress = SubMonitor.convert(monitor, progressWork);

		IJarExportRunnable runnable = packageData.createJarExportRunnable(null);
		try {
			runnable.run(monitor);
			File file = new File(location.toString());
			if (!file.exists()) {
				handleApplicationDeploymentFailure();
			} else {
				return file;
			}

		} catch (InvocationTargetException e) {
			CloudErrorUtil.toCoreException(e);
		} catch (InterruptedException ie) {
			CloudErrorUtil.toCoreException(ie);
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
			File tempFile = File.createTempFile("tempFileForJar", null);
			tempFile.delete();
			tempFile.mkdirs();

			if (!tempFile.exists()) {
				throw CloudErrorUtil
						.toCoreException("Failed to created temporary directory when packaging application for deployment. Check permissions at: "
								+ tempFile.getPath());
			}

			String path = new Path(tempFile.getPath()).append(
					module.getName() + ".jar").toString();
			tempFile.deleteOnExit();

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