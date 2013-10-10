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
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
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

	public static final String DEFAULT = "manifest.yml";

	public ManifestParser(CloudFoundryApplicationModule appModule) {
		this(DEFAULT, appModule);
	}

	public ManifestParser(String relativePath, CloudFoundryApplicationModule appModule) {
		Assert.isNotNull(relativePath);
		this.relativePath = relativePath;
		this.appModule = appModule;
	}

	protected InputStream getInputStream() {

		File file = getFile();
		if (file != null && file.exists()) {
			try {
				return new FileInputStream(file);
			}
			catch (FileNotFoundException e) {
				CloudFoundryPlugin.logError("Unable to read manifest.yml file. Check if file is accessible at: "
						+ file.getAbsolutePath().toString());
			}
		}
		return null;
	}

	protected File getFile() {
		IProject project = CloudUtil.getProject(appModule);
		if (project == null) {
			return null;
		}
		IResource resource = project.getFile(relativePath);
		if (resource.exists()) {
			URI locationURI = resource.getLocationURI();
			return new File(locationURI);

		}
		return null;
	}

	protected Map<?, ?> getContainingPropertiesMap(Map<?, ?> containerMap, String propertyName) {
		Object yamlElementObj = containerMap.get(propertyName);
		if (!isPropertiesMap(yamlElementObj, propertyName)) {
			return Collections.EMPTY_MAP;
		}
		return (Map<?, ?>) yamlElementObj;
	}

	protected String getValue(Map<?, ?> containingMap, String propertyName) {
		Object valObj = containingMap.get(propertyName);

		if (isStringValue(valObj, propertyName)) {
			return (String) valObj;
		}
		return null;
	}

	protected boolean isPropertiesMap(Object possibleMap, String propertyName) {
		if (!(possibleMap instanceof Map<?, ?>)) {
			CloudFoundryPlugin.logError("Problem parsing manifest file for application: "
					+ appModule.getDeployedApplicationName() + ". Expected map of properties for: " + propertyName);

			return false;
		}
		return true;
	}

	protected boolean isStringValue(Object valObj, String propertyName) {

		if (!(valObj instanceof String)) {
			CloudFoundryPlugin.logError("Problem parsing manifest file for application: "
					+ appModule.getDeployedApplicationName() + ". Expected String value for: " + propertyName);

			return false;
		}
		return true;
	}

	public ApplicationDeploymentInfo parse(ApplicationDeploymentInfo existingInfo) {
		
		if (existingInfo != null) {
			return existingInfo;
		}
		ApplicationDeploymentInfo deploymentInfo = null;
		InputStream inputStream = getInputStream();

		if (inputStream != null) {
			Yaml yaml = new Yaml();

			Object results = yaml.load(inputStream);
			if (results instanceof Map<?, ?>) {
				Map<?, ?> mapResult = (Map<?, ?>) results;

				Map<?, ?> applications = getContainingPropertiesMap(mapResult, APPLICATIONS_PROP);
				String appName = getValue(applications, NAME_PROP);

				deploymentInfo = new ApplicationDeploymentInfo(appName);

				String memoryVal = getValue(applications, MEMORY_PROP);
				String instancesVal = getValue(applications, INSTANCES_PROP);
				String host = getValue(applications, SUB_DOMAIN_PROP);

				Map<?, ?> services = getContainingPropertiesMap(applications, SERVICES_PROP);

				List<String> servicesToBind = new ArrayList<String>();

				for (Entry<?, ?> entry : services.entrySet()) {
					Object serviceNameObj = entry.getKey();
					if (isStringValue(serviceNameObj, SERVICES_PROP)) {
						String serviceName = (String) entry.getKey();
						if (!servicesToBind.contains(serviceName)) {
							servicesToBind.add(serviceName);
						}

					}

				}

			}
		}

		return deploymentInfo;
	}

	public void write(ApplicationDeploymentInfo deploymentInfo) {
		// if (results != null) {
		// Yaml yaml = new Yaml();
		// String string = yaml.dump(results);
		// System.out.println("RESULT MANIFEST DUMP:" + string);
		// }
	}
}
