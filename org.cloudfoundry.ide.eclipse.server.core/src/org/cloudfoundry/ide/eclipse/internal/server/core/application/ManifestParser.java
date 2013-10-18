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
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.yaml.snakeyaml.Yaml;

public class ManifestParser {

	public static final String APPLICATIONS_PROP = "applications";

	public static final String NAME_PROP = "name";

	public static final String MEMORY_PROP = "memory";

	public static final String INSTANCES_PROP = "instances";

	public static final String SUB_DOMAIN_PROP = "host";

	public static final String DOMAIN_PROP = "domain";

	public static final String SERVICES_PROP = "services";

	public static final String LABEL_PROP = "label";

	public static final String PROVIDER_PROP = "provider";

	public static final String VERSION_PROP = "version";

	public static final String PLAN_PROP = "plan";

	private final String relativePath;

	private final CloudFoundryApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	public static final String DEFAULT = "manifest.yml";

	static final String ERROR_MESSAGE = "Unable to write changes to the application's manifest file";

	public ManifestParser(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		this(DEFAULT, appModule, cloudServer);
	}

	public ManifestParser(String relativePath, CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		Assert.isNotNull(relativePath);
		this.relativePath = relativePath;
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	/**
	 * Input stream to the manifest file, if the file exists. Caller must
	 * properly dispose any open resources and close the stream.
	 * @return input stream to the manifest file, ONLY if the file exists and is
	 * accessible. Null otherwise.
	 * @throws FileNotFoundException if file exists, but input stream could not
	 * be opened to it.
	 */
	protected InputStream getInputStream() throws FileNotFoundException {

		File file = getFile();
		if (file != null && file.exists()) {
			return new FileInputStream(file);
		}
		return null;
	}

	/**
	 * Output stream to the manifest file. Existing manifest files are deleted.
	 * If the manifest does not exist, it will be created. Caller must properly
	 * dispose any open resources and close the stream.
	 * @return output stream to an existing manifest file, or null if it does
	 * not exist
	 * @throws IOException if error while creating a manifest file.
	 */
	protected OutputStream getOutStream() throws IOException, FileNotFoundException {
		File file = getFile();
		if (file != null) {
			if (file.exists()) {
				file.delete();
			}

			if (file.createNewFile()) {
				return new FileOutputStream(file);
			}
		}

		return null;
	}

	/**
	 * If the application module has an accessible workspace project, return the
	 * manifest file contained in the project. Otherwise return null. The file
	 * itself may not yet exist, but if returned, it at least means the
	 * workspace project does indeed exist.
	 * @return Manifest file in the workspace project for the application, or
	 * null if the project does not exists or is not accessible.
	 */
	protected File getFile() {
		IProject project = CloudUtil.getProject(appModule);
		if (project == null) {
			return null;
		}
		IResource resource = project.getFile(relativePath);
		if (resource != null) {
			URI locationURI = resource.getLocationURI();
			return new File(locationURI);

		}
		return null;
	}

	/**
	 * @return true if the application has an accessible manifest file that
	 * exists. False otherwise, even if the application does have a manifest
	 * file. A false in this case would mean the file is not accessible.
	 */
	public boolean hasManifest() {
		File file = getFile();
		return file != null && file.exists();
	}

	/**
	 * @return true if the application has an accessible workspace project where
	 * a manifest file can be written too. False otherwise.
	 */
	public boolean canWriteToManifest() {
		return CloudUtil.getProject(appModule) != null;
	}

	/**
	 * 
	 * @param containerMap
	 * @param propertyName
	 * @return
	 */
	protected Map<Object, Object> getContainingPropertiesMap(Map<Object, Object> containerMap, String propertyName) {
		if (containerMap == null || propertyName == null) {
			return null;
		}
		Object yamlElementObj = containerMap.get(propertyName);

		if (yamlElementObj instanceof Map<?, ?>) {
			return (Map<Object, Object>) yamlElementObj;
		}
		else {
			return null;
		}
	}

	protected String getStringValue(Map<?, ?> containingMap, String propertyName) {
		Object valObj = containingMap.get(propertyName);

		if (valObj instanceof String) {
			return (String) valObj;
		}
		return null;
	}

	protected Integer getIntegerValue(Map<?, ?> containingMap, String propertyName) {
		Object valObj = containingMap.get(propertyName);

		if (valObj instanceof Integer) {
			return (Integer) valObj;
		}
		return null;
	}

	/**
	 * 
	 * @return Deployment copy if a manifest file was successfully loaded into
	 * an app's deployment info working copy. Note that the copy is NOT saved.
	 * Null if there is no content to load into the app's deployment info
	 * working copy.
	 * @throws CoreException if error occurred while loading an existing
	 * manifest file.
	 */
	public DeploymentInfoWorkingCopy load() throws CoreException {

		DeploymentInfoWorkingCopy workingCopy = appModule.getDeploymentInfoWorkingCopy();

		Map<Object, Object> results = parseManifestFromFile();

		if (results == null) {
			return null;
		}

		Map<Object, Object> applications = getContainingPropertiesMap(results, APPLICATIONS_PROP);
		if (applications == null) {
			throw CloudErrorUtil
					.toCoreException("Expected a top-level map with application properties for: "
							+ relativePath
							+ ". Unable to continue parsing manifest values. No manifest values will be loaded into the application deployment info.");
		}

		String appName = getStringValue(applications, NAME_PROP);

		if (appName != null) {
			workingCopy.setDeploymentName(appName);
		}

		Integer memoryVal = getIntegerValue(applications, MEMORY_PROP);
		if (memoryVal != null) {
			workingCopy.setMemory(memoryVal.intValue());
		}

		String host = getStringValue(applications, SUB_DOMAIN_PROP);

		if (host != null) {
			// Select a default URL with the given host:
			CloudApplicationUrlLookup lookup = CloudApplicationUrlLookup.getCurrentLookup(cloudServer);
			CloudApplicationURL defaultUrl = lookup.getDefaultApplicationURL(host);
			if (defaultUrl != null) {
				List<String> urls = Arrays.asList(defaultUrl.getUrl());
				workingCopy.setUris(urls);
			}
		}

		Map<Object, Object> services = getContainingPropertiesMap(applications, SERVICES_PROP);
		if (services != null) {
			List<String> servicesToBind = new ArrayList<String>();

			for (Entry<Object, Object> entry : services.entrySet()) {
				Object serviceNameObj = entry.getKey();
				if (serviceNameObj instanceof String) {
					String serviceName = (String) serviceNameObj;
					if (!servicesToBind.contains(serviceName)) {
						servicesToBind.add(serviceName);
					}
				}
			}

			workingCopy.setServices(servicesToBind);
		}

		return workingCopy;
	}

	/**
	 * 
	 * @return map of parsed manifest file, if the file exists. If the file does
	 * not exist, return null.
	 * @throws CoreException if manifest file exists, but error occurred that
	 * prevents a map to be generated.
	 */
	protected Map<Object, Object> parseManifestFromFile() throws CoreException {

		InputStream inputStream = null;
		try {
			inputStream = getInputStream();
		}
		catch (FileNotFoundException fileException) {
			throw CloudErrorUtil.toCoreException(fileException);
		}

		if (inputStream != null) {
			Yaml yaml = new Yaml();

			try {

				Object results = yaml.load(inputStream);

				if (results instanceof Map<?, ?>) {
					return (Map<Object, Object>) results;
				}
				else {
					throw CloudErrorUtil.toCoreException("Expected a map of values for manifest file: " + relativePath
							+ ". Unable to load manifest content.  Actual results: " + results);
				}

			}
			finally {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					// Ignore
				}
			}

		}
		return null;
	}

	/**
	 * Writes the app's current deployment info into a manifest file in the
	 * app's related workspace project. If the workspace project is not
	 * accessible, false is returned. If the manifest file does not exist in the
	 * app's workspace project, one will be created. If manifest file failed to
	 * create, exception is thrown. Returns true if the manifest file was
	 * successfully written. If so, the project is also refreshed.
	 * @return true if deployment info for the cloud module was written to
	 * manifest file. False if there was no content to write to the manifest
	 * file.
	 * @throws CoreException if error occurred while writing to a Manifest file.
	 */
	public boolean write(IProgressMonitor monitor) throws CoreException {

		ApplicationDeploymentInfo deploymentInfo = appModule.getDeploymentInfo();

		if (deploymentInfo == null) {
			return false;
		}

		Map<Object, Object> deploymentInfoYaml = parseManifestFromFile();

		if (deploymentInfoYaml == null) {
			deploymentInfoYaml = new HashMap<Object, Object>();
		}

		Map<Object, Object> applicationProperties = getContainingPropertiesMap(deploymentInfoYaml, APPLICATIONS_PROP);

		if (applicationProperties == null) {
			applicationProperties = new HashMap<Object, Object>();
			deploymentInfoYaml.put(APPLICATIONS_PROP, applicationProperties);
		}

		String appName = deploymentInfo.getDeploymentName();

		if (appName != null) {
			applicationProperties.put(NAME_PROP, appName);
		}

		int memory = deploymentInfo.getMemory();
		if (memory > 0) {
			applicationProperties.put(MEMORY_PROP, memory);
		}

		// Regardless if there are services or not, always clear list of
		// services in the manifest, and replace with new list. The list of
		// services in the
		// deployment info has to match the content in the manifest.
		Map<Object, Object> services = new HashMap<Object, Object>();
		applicationProperties.put(SERVICES_PROP, services);

		List<String> servicesToBind = deploymentInfo.getServices();

		if (servicesToBind != null) {

			for (String service : servicesToBind) {
				// Service name is the key in the yaml map
				services.put(service, null);
			}
		}

		if (deploymentInfoYaml.isEmpty()) {
			return false;
		}

		Yaml yaml = new Yaml();
		String manifestValue = yaml.dump(deploymentInfoYaml);

		if (manifestValue == null) {
			throw CloudErrorUtil.toCoreException("Manifest map for " + appModule.getDeployedApplicationName()
					+ " contained values but yaml parser failed to serialise the map. : " + deploymentInfoYaml);
		}

		OutputStream outStream = null;
		try {
			outStream = getOutStream();
			if (outStream == null) {
				throw CloudErrorUtil.toCoreException("No output stream could be opened to: " + relativePath
						+ ". Unable to write changes to the app's manifest file for: "
						+ appModule.getDeployedApplicationName());
			}

			outStream.write(manifestValue.getBytes());
			outStream.flush();
			// Refresh the associated project
			IProject project = CloudUtil.getProject(appModule);
			if (project != null) {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
			return true;

		}
		catch (IOException io) {
			throw CloudErrorUtil.toCoreException(io);
		}
		finally {
			if (outStream != null) {
				try {
					outStream.close();
				}
				catch (IOException io) {
					// Ignore
				}
			}
		}

	}
}
