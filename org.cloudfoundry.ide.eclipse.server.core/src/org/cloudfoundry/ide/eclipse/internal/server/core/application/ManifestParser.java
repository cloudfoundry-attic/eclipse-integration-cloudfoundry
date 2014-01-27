/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.LocalCloudService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
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

	public static final String PATH_PROP = "path";

	public static final String BUILDPACK_PROP = "buildpack";

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
		IProject project = CloudFoundryProjectUtil.getProject(appModule);
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
		return CloudFoundryProjectUtil.getProject(appModule) != null;
	}

	/**
	 * 
	 * @param applicationName name of application to lookup in the manifest
	 * file.
	 * @param propertyName String value property to retrieve from manifest for
	 * given application entry.
	 * @return Value of property, or null if not found, or entry for application
	 * in manifest does not exist.
	 */
	public String getApplicationProperty(String applicationName, String propertyName) {
		try {
			Map<?, ?> map = getApplication(applicationName);
			if (map != null) {
				return getStringValue(map, propertyName);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	/**
	 * 
	 * @param containerMap
	 * @param propertyName
	 * @return
	 */
	protected Map<?, ?> getContainingPropertiesMap(Map<?, ?> containerMap, String propertyName) {
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

	protected Map<?, ?> getApplication(String applicationName) throws CoreException {
		Map<Object, Object> results = parseManifestFromFile();

		if (results == null) {
			return null;
		}

		Object applicationsObj = results.get(APPLICATIONS_PROP);
		if (!(applicationsObj instanceof List<?>)) {
			throw CloudErrorUtil
					.toCoreException("Expected a top-level list of applications in: "
							+ relativePath
							+ ". Unable to continue parsing manifest values. No manifest values will be loaded into the application deployment info.");
		}

		List<?> applicationsList = (List<?>) applicationsObj;

		// Use only the first application entry
		if (applicationsList.isEmpty()) {
			return null;
		}

		Map<?, ?> application = null;
		String errorMessage = null;
		// If no application name specified, get the first one.
		if (applicationName == null) {
			Object mapObj = applicationsList.get(0);
			application = (mapObj instanceof Map<?, ?>) ? (Map<?, ?>) mapObj : null;
			if (application == null) {
				errorMessage = "Expected a map of application properties in: "
						+ relativePath
						+ ". Unable to continue parsing manifest values. No manifest values will be loaded into the application deployment info.";

			}
		}
		else {
			for (Object mapObj : applicationsList) {
				if (mapObj instanceof Map<?, ?>) {
					application = (Map<?, ?>) mapObj;
					String appName = getStringValue(application, NAME_PROP);
					if (applicationName.equals(appName)) {
						break;
					}
					else {
						application = null;
					}
				}
			}
		}

		if (errorMessage != null) {
			throw CloudErrorUtil.toCoreException(errorMessage);
		}

		return application;
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

		Map<?, ?> application = getApplication(null);

		if (application == null) {
			return null;
		}

		// NOTE: When reading from manifest, the manifest may be INCOMPLETE,
		// therefore do not automatically
		// set all properties in the deployment info. Check if the value of the
		// property is actually set before set value
		// in the info
		String appName = getStringValue(application, NAME_PROP);

		if (appName != null) {
			workingCopy.setDeploymentName(appName);
		}

		readMemory(application, workingCopy);

		readApplicationURL(application, workingCopy, appName);

		String buildpackurl = getStringValue(application, BUILDPACK_PROP);
		if (buildpackurl != null) {
			Staging staging = new Staging(null, buildpackurl);
			workingCopy.setStaging(staging);
		}

		readServices(workingCopy, application);

		String archiveURL = getStringValue(application, PATH_PROP);
		if (archiveURL != null) {
			workingCopy.setArchive(archiveURL);
		}

		return workingCopy;
	}

	protected void readServices(DeploymentInfoWorkingCopy workingCopy, Map<?, ?> applications) {
		Map<?, ?> services = getContainingPropertiesMap(applications, SERVICES_PROP);
		if (services != null) {
			Map<String, CloudService> servicesToBind = new LinkedHashMap<String, CloudService>();

			for (Entry<?, ?> entry : services.entrySet()) {
				Object serviceNameObj = entry.getKey();
				if (serviceNameObj instanceof String) {
					String serviceName = (String) serviceNameObj;
					if (!servicesToBind.containsKey(serviceName)) {
						LocalCloudService service = new LocalCloudService(serviceName);
						servicesToBind.put(serviceName, service);

						Object servicePropertiesObj = entry.getValue();
						if (servicePropertiesObj instanceof Map<?, ?>) {
							Map<?, ?> serviceProperties = (Map<?, ?>) servicePropertiesObj;
							String label = getStringValue(serviceProperties, LABEL_PROP);
							if (label != null) {
								service.setLabel(label);
							}
							String provider = getStringValue(serviceProperties, PROVIDER_PROP);
							if (provider != null) {
								service.setProvider(provider);
							}
							String version = getStringValue(serviceProperties, VERSION_PROP);
							if (version != null) {
								service.setVersion(version);
							}
							String plan = getStringValue(serviceProperties, PLAN_PROP);
							if (plan != null) {
								service.setPlan(plan);
							}
						}
					}
				}
			}

			workingCopy.setServices(new ArrayList<CloudService>(servicesToBind.values()));
		}
	}

	protected void readApplicationURL(Map<?, ?> application, DeploymentInfoWorkingCopy workingCopy, String appName) {
		String subdomain = getStringValue(application, SUB_DOMAIN_PROP);
		String domain = getStringValue(application, DOMAIN_PROP);

		// IF one or the other is set, set a default value for the missing part
		if (subdomain != null || domain != null) {

			String url = null;
			if (subdomain == null) {
				subdomain = appName;
			}
			else {
				// Get a default domain since no domain has been specified
				CloudApplicationUrlLookup lookup = CloudApplicationUrlLookup.getCurrentLookup(cloudServer);
				CloudApplicationURL cloudURL = lookup.getDefaultApplicationURL(subdomain);
				if (cloudURL != null) {
					url = cloudURL.getUrl();
				}
			}

			if (url == null) {
				if (domain != null) {
					url = subdomain + '.' + domain;
				}
				else {
					CloudFoundryPlugin
							.logWarning("No domain found while parsing manifest for "
									+ appName
									+ " - No URL will be set for this application. Manual URL entry may be required when pushing the application to Cloud Foundry.");
				}
			}

			if (url != null) {
				List<String> urls = Arrays.asList(url);
				workingCopy.setUris(urls);
			}

		}
	}

	protected void readMemory(Map<?, ?> application, DeploymentInfoWorkingCopy workingCopy) {
		Integer memoryVal = getIntegerValue(application, MEMORY_PROP);

		// If not in Integer form, try String as the memory may end in with a
		// 'G'
		if (memoryVal == null) {
			String memoryStringVal = getStringValue(application, MEMORY_PROP);
			if (memoryStringVal != null && memoryStringVal.length() > 0) {

				char memoryIndicator[] = { 'M', 'G', 'm', 'g' };
				int gIndex = -1;

				for (char indicator : memoryIndicator) {
					gIndex = memoryStringVal.indexOf(indicator);
					if (gIndex >= 0) {
						break;
					}
				}

				// There has to be a number before the 'G' or 'M', if 'G' or 'M'
				// is used, or its not a valid
				// memory
				if (gIndex > 0) {
					memoryStringVal = memoryStringVal.substring(0, gIndex);
				}
				else if (gIndex == 0) {
					CloudFoundryPlugin.logError("Failed to read memory value in manifest file: " + relativePath
							+ " for: " + appModule.getDeployedApplicationName() + ". Invalid memory: "
							+ memoryStringVal);
				}

				try {
					memoryVal = Integer.valueOf(memoryStringVal);
				}
				catch (NumberFormatException e) {
					// Log an error but do not stop the parsing
					CloudFoundryPlugin.logError("Failed to parse memory from manifest file: " + relativePath + " for: "
							+ appModule.getDeployedApplicationName() + " due to: " + e.getMessage());
				}
			}
		}

		if (memoryVal != null) {
			int actualMemory = -1;
			switch (memoryVal.intValue()) {
			case 1:
				actualMemory = 1024;
				break;
			case 2:
				actualMemory = 2048;
				break;
			default:
				actualMemory = memoryVal.intValue();
				break;
			}
			if (actualMemory > 0) {
				workingCopy.setMemory(actualMemory);
			}
		}
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
	 * 
	 * @param descriptor
	 * @return true if it contains description to create a service
	 */
	protected boolean containsServiceCreationDescription(CloudService service) {
		return service.getVersion() != null && service.getLabel() != null && service.getProvider() != null
				&& service.getPlan() != null;
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
	 * <p/>
	 * WHne writing content to manifest, if the application has a previous
	 * deployment information, and the app name changed, meaning the current
	 * name of the application does not match the one in the old deployment
	 * information, the old entry in the manifest will be updated to the new
	 * name, rather than creating a new entry with the new name.
	 * <p/>
	 * If both an existing entry with the current application name, as well as
	 * an old entry with the old application name, exist, the old entry has
	 * higher priority, as the old entry truly represents the application that
	 * is being edited.
	 * @param monitor progress monitor
	 * @param previousInfo Previous deployment information pertaining to the
	 * give application module. May be null if there is no previous information
	 * available.
	 * @throws CoreException if error occurred while writing to a Manifest file.
	 */
	public boolean write(IProgressMonitor monitor, ApplicationDeploymentInfo previousInfo) throws CoreException {

		ApplicationDeploymentInfo deploymentInfo = appModule.getDeploymentInfo();

		if (deploymentInfo == null) {
			return false;
		}

		// Fetch the previous name, in case the app name was changed. This will
		// allow the old
		// entry to be replaced by the new one, since application entries are
		// looked up by application name.
		String previousName = previousInfo != null ? previousInfo.getDeploymentName() : null;

		String appName = deploymentInfo.getDeploymentName();

		Map<Object, Object> deploymentInfoYaml = parseManifestFromFile();

		if (deploymentInfoYaml == null) {
			deploymentInfoYaml = new LinkedHashMap<Object, Object>();
		}

		Object applicationsObj = deploymentInfoYaml.get(APPLICATIONS_PROP);
		List<Map<Object, Object>> applicationsList = null;
		if (applicationsObj == null) {
			applicationsList = new ArrayList<Map<Object, Object>>();
			deploymentInfoYaml.put(APPLICATIONS_PROP, applicationsList);
		}
		else if (applicationsObj instanceof List<?>) {
			applicationsList = (List<Map<Object, Object>>) applicationsObj;
		}
		else {
			throw CloudErrorUtil.toCoreException("Expected a top-level list of applications in: " + relativePath
					+ ". Unable to continue writing manifest values.");
		}

		Map<Object, Object> applicationWithSameName = null;

		Map<Object, Object> oldApplication = null;

		// Each application listing should be a map. Find both an entry with the
		// same name as the application name
		// As well as an entry with an older name of the application, in case
		// the application has changed.
		for (Object appMap : applicationsList) {
			if (appMap instanceof Map<?, ?>) {
				Map<Object, Object> properties = (Map<Object, Object>) appMap;
				String name = getStringValue(properties, NAME_PROP);
				if (appName.equals(name)) {
					applicationWithSameName = properties;
				}
				else if (previousName != null && previousName.equals(name)) {
					oldApplication = properties;
				}
			}
		}

		// The order of priority in terms of replacing an existing entry is : 1.
		// old application entry that
		// has been changed will get replaced 2. existing entry with same name
		// as app will now get replaced2.
		Map<Object, Object> application = oldApplication != null ? oldApplication : applicationWithSameName;

		if (application == null) {
			application = new LinkedHashMap<Object, Object>();
			applicationsList.add(application);
		}

		application.put(NAME_PROP, appName);

		String memory = getMemoryAsString(deploymentInfo.getMemory());
		if (memory != null) {
			application.put(MEMORY_PROP, memory);
		}

		int instances = deploymentInfo.getInstances();
		if (instances > 0) {
			application.put(INSTANCES_PROP, instances);
		}

		List<String> urls = deploymentInfo.getUris();
		if (urls != null && !urls.isEmpty()) {
			// Persist only the first URL
			String url = urls.get(0);

			CloudApplicationUrlLookup lookup = CloudApplicationUrlLookup.getCurrentLookup(cloudServer);
			CloudApplicationURL cloudUrl = lookup.getCloudApplicationURL(url);
			String subdomain = cloudUrl.getSubdomain();
			String domain = cloudUrl.getDomain();

			if (subdomain != null) {
				application.put(SUB_DOMAIN_PROP, subdomain);
			}

			if (domain != null) {
				application.put(DOMAIN_PROP, domain);
			}
		}

		Staging staging = deploymentInfo.getStaging();
		if (staging != null && staging.getBuildpackUrl() != null) {
			application.put(BUILDPACK_PROP, staging.getBuildpackUrl());
		}

		String archiveURL = deploymentInfo.getArchive();
		if (archiveURL != null) {
			application.put(PATH_PROP, archiveURL);
		}

		// Regardless if there are services or not, always clear list of
		// services in the manifest, and replace with new list. The list of
		// services in the
		// deployment info has to match the content in the manifest.

		Map<Object, Object> services = new LinkedHashMap<Object, Object>();
		application.put(SERVICES_PROP, services);

		List<CloudService> servicesToBind = deploymentInfo.getServices();

		if (servicesToBind != null) {

			for (CloudService service : servicesToBind) {
				String serviceName = service.getName();
				if (!services.containsKey(serviceName)) {

					// Only persist the service if it has complete information
					if (containsServiceCreationDescription(service)) {
						Map<String, String> serviceDescription = new LinkedHashMap<String, String>();
						String label = service.getLabel();
						if (label != null) {
							serviceDescription.put(LABEL_PROP, label);
						}
						String version = service.getVersion();
						if (version != null) {
							serviceDescription.put(VERSION_PROP, version);
						}
						String plan = service.getPlan();
						if (plan != null) {
							serviceDescription.put(PLAN_PROP, plan);
						}
						String provider = service.getProvider();
						if (provider != null) {
							serviceDescription.put(PROVIDER_PROP, provider);
						}

						// Service name is the key in the yaml map
						services.put(serviceName, serviceDescription);
					}
				}
			}
		}

		if (deploymentInfoYaml.isEmpty()) {
			return false;
		}

		DumperOptions options = new DumperOptions();
		options.setCanonical(false);
		options.setPrettyFlow(true);
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
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
						+ ". Unable to write changes to the application's manifest file for: "
						+ appModule.getDeployedApplicationName());
			}

			outStream.write(manifestValue.getBytes());
			outStream.flush();
			// Refresh the associated project
			IProject project = CloudFoundryProjectUtil.getProject(appModule);
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

	protected String getMemoryAsString(int memory) {
		if (memory < 1) {
			return null;
		}
		return memory + "M";
	}

}
