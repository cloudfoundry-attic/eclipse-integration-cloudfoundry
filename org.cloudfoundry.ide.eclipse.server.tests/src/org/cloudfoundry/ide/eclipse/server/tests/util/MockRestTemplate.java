package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryMockClientFixture.TestConnectionDescriptor;
import org.eclipse.core.internal.resources.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


public class MockRestTemplate extends RestTemplate {

	protected final URL cloudControllerUrl;

	protected final String email;

	protected final String token;

	protected final String password;

	private RestTemplateValues sessionValue = null;

	public MockRestTemplate(TestConnectionDescriptor descriptor) {
		this.cloudControllerUrl = descriptor.controllerURL;
		this.email = descriptor.email;
		this.token = descriptor.token;
		this.password = descriptor.password;
		sessionValue = new RestTemplateValues();
	}

	enum PropertyURL {
		resources("resources", "resources"),

		apps("apps", "apps"),

		appName("apps/{appName}", "apps"),

		application("apps/{appName}/application", "application"),

		services("services", "services"),

		service("services/{service}", "service"),

		email("users/{email}", "email"),

		instances("apps/{appName}/instances", "instances"),

		files("apps/{appName}/instances/{instanceIndex}/files/{filePath}", "files"),

		info("info", "info"),

		tokens("users/{id}/tokens", "token"),

		stats("apps/{appName}/stats", "stats"),

		user("users/{id}", "users"),

		infoServices("info/services", "services"),

		crashes("apps/{appName}/crashes", "crashes");

		private String url;

		private String key;

		private static Map<String, PropertyURL> index;

		public static PropertyURL getProperty(String url, URL cloudControllerUrl) {
			if (index == null) {
				index = new HashMap<String, MockRestTemplate.PropertyURL>();
				for (PropertyURL name : PropertyURL.values()) {
					index.put(getUrl(cloudControllerUrl, name.getURL()), name);
				}
			}
			return index.get(url);
		}

		private PropertyURL(String url, String key) {
			this.url = url;
			this.key = key;
		}

		public String getURL() {
			return url;
		}

		public String getKey() {
			return key;
		}

	}

	protected static String getUrl(URL cloudControllerUrl, String path) {
		return cloudControllerUrl + "/" + path;
	}

	public synchronized void addValue(PropertyURL name, String key, Object value) {
		sessionValue.getValues(name).put(key, value);
	}

	public synchronized void removeProperty(PropertyURL name) {
		sessionValue.remove(name);
	}

	public synchronized Map<String, Object> getValues(PropertyURL name) {
		if (sessionValue.hasProperty(name)) {
			return sessionValue.getValues(name);
		}
		return null;
	}

	@Override
	public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		PropertyURL property = getProperty(url);
		if (property != null) {
			switch (property) {
			case tokens:
				if (Map.class.isAssignableFrom(responseType)) {

					if (request instanceof Map) {
						Map<?, ?> payload = (Map<?, ?>) request;
						String password = (String) payload.get("password");
						String email = (String) uriVariables[0];
						addValue(property, "password", password);
						addValue(property, "email", email);

						Map<String, String> response = new HashMap<String, String>();
						response.put("token", (String) null);
						return (T) response;
					}

				}
				break;

			case resources:
				if (List.class.isAssignableFrom(responseType)) {
					// matchedResources =
					// restTemplate.postForObject(getUrl("resources"),
					// appWrapper.generateFingerprint(),
					// List.class);
					// [{sha1=E9856D0DD103D59A7CA563D919D983470D81E004,
					// fn=META-INF/MANIFEST.MF, size=39},
					// {sha1=D02D1CED2DCD8FA7A2BE7F33F6A9204D130F6B17,
					// fn=WEB-INF/web.xml, size=704}]

					List<String> response = new ArrayList<String>();

					return (T) response;
				}
				break;

			case application:
				if (MultiValueMap.class.isAssignableFrom(responseType)) {

					// restTemplate.put(
					// getUrl("apps/{appName}/application"),
					// generatePartialResourcePayload(
					// new InputStreamResourceWithName(appStream, appSize,
					// file.getName()), resources),
					// appName);

					Map<String, String> response = new HashMap<String, String>();

					return (T) response;
				}

				break;
			}
		}
		else {
			throwDefaultError(url, uriVariables);
		}

		return null;

	}

	@Override
	public void delete(String url, Object... urlVariables) throws RestClientException {

		PropertyURL property = getProperty(url);

		if (property != null) {

		}
		else {
			throwDefaultError(url, urlVariables);
		}

	}

	protected void throwDefaultError(String url, Object... urlVariables) throws RestClientException {
		throw new RestClientException("Invalid url: " + url + " or variables: " + urlVariables);
	}

	@Override
	public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Object... urlVariables) throws RestClientException {
		// return
		// restTemplate.execute(getUrl("apps/{appName}/instances/{instanceIndex}/files/{filePath}"),
		// HttpMethod.GET, requestCallback, responseHandler, appName,
		// instanceIndex, filePath);

		return super.execute(url, method, requestCallback, responseExtractor, urlVariables);

	}

	@Override
	public <T> T getForObject(String url, Class<T> responseType, Object... urlVariables) throws RestClientException {

		PropertyURL property = getProperty(url);

		if (property != null) {

			switch (property) {

			case info:

				// return
				// forObjects.getRestEntries().get(PropertyName.info).getDefaultValue().getValues();
				break;

			case apps:

				break;

			case appName:
				if (responseType.isAssignableFrom(Map.class)) {
					String appName = (String) urlVariables[0];
					Object appValues = getValues(PropertyURL.apps).get(appName);
					Map<String, Object> cloudApplicationInfo = getCloudApplication(appName);
					return (T) cloudApplicationInfo;

					// Map<String, Object> appAsMap =
					// restTemplate.getForObject(getUrl("apps/{appName}"),
					// Map.class,
					// appName);
					// return new CloudApplication(appAsMap);
				}

				break;

			case stats:
				if (responseType.isAssignableFrom(Map.class)) {
					return (T) getApplicationStats();
				}
				// Map<String, Object> statsAsMap =
				// restTemplate.getForObject(getUrl(), Map.class, appName);
				// return new ApplicationStats(statsAsMap);
				break;
			case files:
				// return
				// restTemplate.getForObject(getUrl("apps/{appName}/instances/{instanceIndex}/files/{filePath}"),
				// String.class, appName, instanceIndex, filePath);
				break;
			case services:
				// List<Map<String, Object>> servicesAsMap =
				// restTemplate.getForObject(getUrl("services"), List.class);
				// List<CloudService> services = new ArrayList<CloudService>();
				// for (Map<String, Object> serviceAsMap : servicesAsMap) {
				// services.add(new CloudService(serviceAsMap));
				// }
				// return services;
				break;

			case service:
				// Map<String, Object> serviceAsMap =
				// restTemplate.getForObject(getUrl("services/{service}"),
				// Map.class,
				// service);
				// return new CloudService(serviceAsMap);

				break;
			case infoServices:

				// Map<String, Object> configurationAsMap =
				// restTemplate.getForObject(getUrl("info/services"),
				// Map.class);
				// if (configurationAsMap == null) {
				// return Collections.emptyList();
				// }
				//
				// List<ServiceConfiguration> configurations = new
				// ArrayList<ServiceConfiguration>();
				//
				// for (Map.Entry<String, Object> typeEntry :
				// configurationAsMap.entrySet()) {
				// Map<String, Object> vendorMap = CloudUtil.parse(Map.class,
				// typeEntry.getValue());
				// if (vendorMap == null) {
				// continue;
				// }
				//
				// for (Map.Entry<String, Object> vendorEntry :
				// vendorMap.entrySet()) {
				// Map<String, Object> versionMap = CloudUtil.parse(Map.class,
				// vendorEntry.getValue());
				// if (versionMap == null) {
				// continue;
				// }
				//
				// for (Map.Entry<String, Object> serviceEntry :
				// versionMap.entrySet()) {
				// Map<String, Object> attributes = CloudUtil.parse(Map.class,
				// serviceEntry.getValue());
				// if (attributes != null) {
				// configurations.add(new ServiceConfiguration(attributes));
				// }
				// }
				// }
				// }
				//
				// return configurations;
				break;
			case instances:

				// Map<String, Object> map =
				// restTemplate.getForObject(getUrl("apps/{appName}/instances"),
				// Map.class, appName);
				// @SuppressWarnings("unchecked")
				// List<Map<String, Object>> instanceData = (List<Map<String,
				// Object>>) map.get("instances");
				// return new InstancesInfo(instanceData);
				break;
			case crashes:
				// @SuppressWarnings("unchecked")
				// Map<String, Object> map =
				// restTemplate.getForObject(getUrl("apps/{appName}/crashes"),
				// Map.class, appName);
				// @SuppressWarnings("unchecked")
				// List<Map<String, Object>> crashData = (List<Map<String,
				// Object>>) map.get("crashes");
				// return new CrashesInfo(crashData);

				break;
			}

		}
		else {
			throwDefaultError(url, urlVariables);
		}
		return null;

	}

	@Override
	public URI postForLocation(String url, Object request, Object... urlVariables) throws RestClientException {

		PropertyURL property = getProperty(url);

		if (property != null) {
			// restTemplate.postForLocation(getUrl("services"), service);
			//
			// addValue(PropertyName.services, request);
			//
			// Map<String, String> payload = new HashMap<String, String>();
			// payload.put("email", email);
			// payload.put("password", password);
			//
			// restTemplate.postForLocation(getUrl("users"), payload);
			//
			// // /
			//
			// CloudApplication payload = new CloudApplication(appName, null,
			// framework, memory, 1, uris, serviceNames,
			// AppState.STOPPED);
			// restTemplate.postForLocation(getUrl("apps"), payload);
			// CloudApplication postedApp = getApplication(appName);
			// if (serviceNames != null && serviceNames.size() != 0) {
			// postedApp.setServices(serviceNames);
			// updateApplication(postedApp);
			// }

		}
		else {
			throwDefaultError(url, urlVariables);
		}

		return null;

	}

	protected PropertyURL getProperty(String url) {
		return PropertyURL.getProperty(url, cloudControllerUrl);
	}

	@Override
	public void put(String url, Object request, Map<String, ?> urlVariables) throws RestClientException {

		PropertyURL property = getProperty(url);

		if (property != null) {
			switch (property) {
			case application:
				// return null;
				// restTemplate.put(
				// getUrl("apps/{appName}/application"),
				// generatePartialResourcePayload(new
				// InputStreamResourceWithName(appStream, appSize,
				// file.getName()),
				// resources), appName);
				break;

			case appName:
				// // ///
				//
				// CloudApplication app = getApplication(appName);
				// if (app == null) {
				// throw new IllegalArgumentException("Application " + appName +
				// " does not exist");
				// }
				//
				// app.setName(newName);
				// restTemplate.put(getUrl("apps/{appName}"), app, appName);
				//
				// // ///
				//
				// restTemplate.put(getUrl("apps/{appName}"), app,
				// app.getName());
				break;

			}

		}
		else {
			throwDefaultError(url, urlVariables);
		}

	}

	private MultiValueMap<String, ?> generatePartialResourcePayload(Resource application, String resources) {
		MultiValueMap<String, Object> payload = new LinkedMultiValueMap<String, Object>(2);
		payload.add("application", application);
		if (resources != null) {
			payload.add("resources", resources);
		}
		return payload;
	}

	class RestTemplateValues {

		private final Map<PropertyURL, Map<String, Object>> values = new HashMap<MockRestTemplate.PropertyURL, Map<String, Object>>();

		public Map<String, Object> getValues(PropertyURL name) {
			Map<String, Object> val = values.get(name);
			if (val == null) {
				val = new HashMap<String, Object>();
				values.put(name, val);
			}
			return val;
		}

		public void remove(PropertyURL name) {
			values.remove(name);
		}

		public boolean hasProperty(PropertyURL name) {
			return values.containsKey(name);
		}

	}

	public Map<String, Object> getCloudApplication(String appName) {
		Map<String, Object> atts = new HashMap<String, Object>();

		atts.put("name", (String) null);
		atts.put("staging", new HashMap<String, String>());
		atts.put("runningInstances", (Integer) null);
		atts.put("instances", (Integer) null);

		atts.put("uris", new ArrayList<String>());

		atts.put("services", new ArrayList<String>());
		atts.put("state", (String) null);

		Map<String, String> meta = new HashMap<String, String>();
		meta.put("debug", (String) null);
		atts.put("meta", meta);

		atts.put("resource", new HashMap<String, Integer>());

		atts.put("env", new ArrayList<String>());

		return atts;
	}

	public Map<String, Map<String, Object>> getApplicationStats() {
		Map<String, Map<String, Object>> atts = new HashMap<String, Map<String, Object>>();

		String instanceID = "";
		// May have to iterate through all instances if multiple exists
		Map<String, Object> instanceStats = new HashMap<String, Object>();
		instanceStats.put("state", null); // String

		// stats

		Map<String, Object> stats = new HashMap<String, Object>();

		stats.put("core", (Integer) null);
		stats.put("name", (String) null);
		stats.put("disk_quota", (Long) null);
		stats.put("port", (Integer) null);
		stats.put("mem_quota", (Long) null);
		stats.put("uris", new ArrayList<String>());
		stats.put("fds_quota", (Integer) null);
		stats.put("host", (String) null);
		stats.put("uptime", (Double) null);

		instanceStats.put("stats", stats);

		atts.put(instanceID, instanceStats);

		return atts;
	}

}
