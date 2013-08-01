/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.uaa;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.json.simple.JSONObject;
import org.springframework.uaa.client.TransmissionAwareUaaService;
import org.springframework.uaa.client.TransmissionEventListener;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient.FeatureUse;
import org.springframework.uaa.client.protobuf.UaaClient.Product;
import org.springframework.uaa.client.util.HexUtils;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author Christian Dupuis
 */
public class UaaAwareCloudFoundryClient extends CloudFoundryClient implements TransmissionEventListener {

	public final static String VCLOUD_URL = "http://api.cloudfoundry.com";

	public final static String VCLOUD_SECURE_URL = "https://api.cloudfoundry.com";

	private final static int HTTP_SUCCESS_CODE = 200;

	private UaaService uaaService;

	private Set<String> discoveredAppNames = new HashSet<String>();

	private URL cloudControllerUrl;

	/**
	 * key: method name, value: sorted map of HTTP response code keys to count
	 * of that response code
	 */
	private Map<String, SortedMap<Integer, Integer>> methodToResponses = new HashMap<String, SortedMap<Integer, Integer>>();

	private final Product PRODUCT = VersionHelper.getProduct("Cloud Foundry Java API", "1.0.0.SNAPSHOT",
			"d4bfc41476f83ecfa164511b3d3448cca9807266");

	private int cloudMajorVersion = 0;

	private int cloudMinorVersion = 0;

	private int cloudPatchVersion = 0;

	public UaaAwareCloudFoundryClient(UaaService _uaaService, CloudCredentials credentials, URL cloudControllerUrl,
			HttpProxyConfiguration proxyConfiguration) throws MalformedURLException {
		super(credentials, cloudControllerUrl, proxyConfiguration);
		this.uaaService = _uaaService;
		this.cloudControllerUrl = cloudControllerUrl;
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService) uaaService).addTransmissionEventListener(this);
		}
	}

	public UaaAwareCloudFoundryClient(UaaService _uaaService, CloudCredentials credentials, URL cloudControllerUrl,
			CloudSpace session) throws MalformedURLException {
		super(credentials, cloudControllerUrl, session);
		this.uaaService = _uaaService;
		this.cloudControllerUrl = cloudControllerUrl;
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService) uaaService).addTransmissionEventListener(this);
		}
	}

	public void stop() {
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService) uaaService).removeTransmissionEventListener(this);
		}
		flushToUaa();
	}

	public void afterTransmission(TransmissionType type, boolean successful) {
		if (type == TransmissionType.UPLOAD && successful) {
			discoveredAppNames.clear();
			methodToResponses.clear();
		}
	}

	public void beforeTransmission(TransmissionType type) {
		if (type == TransmissionType.UPLOAD) {
			flushToUaa();
		}
	}

	private void flushToUaa() {
		// Store the app names being used
		for (String appName : discoveredAppNames) {
			uaaService.registerProductUsage(PRODUCT, appName);
		}

		// Store the cloud controller URL being used
		String ccType = "Cloud Controller: Custom";
		if (VCLOUD_URL.equals(cloudControllerUrl.toExternalForm())) {
			ccType = "Cloud Controller: Public Cloud";
		}
		else if (VCLOUD_SECURE_URL.equals(cloudControllerUrl.toExternalForm())) {
			ccType = "Cloud Controller: Public Cloud";
		}
		else if (cloudControllerUrl.getHost().equals("localhost")) {
			ccType = "Cloud Controller: Localhost";
		}
		else if (cloudControllerUrl.getHost().equals("127.0.0.1")) {
			ccType = "Cloud Controller: Localhost";
		}
		// Store the cloud controller hostname SHA 256
		String ccUrlHashed = sha256(cloudControllerUrl.getHost());

		// Create a feature use record for the cloud controller
		Map<String, Object> ccJson = new HashMap<String, Object>();
		ccJson.put("type", "cc_info");
		ccJson.put("cc_hostname_sha256", JSONObject.escape(ccUrlHashed));
		registerFeatureUse(ccType, ccJson);

		// Crate feature uses for each method name
		for (String methodName : methodToResponses.keySet()) {
			SortedMap<Integer, Integer> resultCounts = methodToResponses.get(methodName);
			Map<String, Object> methodCallInfo = new HashMap<String, Object>();
			methodCallInfo.put("type", "method_call_info");
			methodCallInfo.put("cc_hostname_sha256", JSONObject.escape(ccUrlHashed));
			methodCallInfo.put("http_results_to_counts", resultCounts);
			registerFeatureUse(methodName, methodCallInfo);
		}
	}

	private void registerFeatureUse(String featureName, Map<String, Object> jsonPayload) {
		jsonPayload.put("version",
				PRODUCT.getMajorVersion() + "." + PRODUCT.getMinorVersion() + "." + PRODUCT.getPatchVersion());
		String jsonAsString = JSONObject.toJSONString(jsonPayload);
		FeatureUse featureToRegister = FeatureUse.newBuilder().setName(featureName)
				.setDateLastUsed(System.currentTimeMillis()).setMajorVersion(cloudMajorVersion)
				.setMinorVersion(cloudMinorVersion).setPatchVersion(cloudPatchVersion).build();
		try {
			uaaService.registerFeatureUsage(PRODUCT, featureToRegister, jsonAsString.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException ignore) {
		}
	}

	private String sha256(String input) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
			byte[] digest = sha1.digest(input.getBytes("UTF-8"));
			return HexUtils.toHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			// This can't happen as we know that there is an SHA-256 algorithm
		}
		catch (UnsupportedEncodingException e) {
			// This can't happen as we know that there is an UTF-8 encoding
		}
		return null;
	}

	private void recordHttpResult(String methodName, int resultCode) {
		recordHttpResult(methodName, resultCode, null);
	}

	private void recordHttpResult(String methodName, int resultCode, String appName) {
		if (appName != null) {
			discoveredAppNames.add(appName);
		}
		SortedMap<Integer, Integer> results = methodToResponses.get(methodName);
		if (results == null) {
			results = new TreeMap<Integer, Integer>();
			methodToResponses.put(methodName, results);
		}
		Integer countSoFar = results.get(resultCode);
		if (countSoFar == null) {
			countSoFar = 0;
		}
		results.put(resultCode, countSoFar + 1);
	}

	@Override
	public void bindService(String appName, String serviceName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.bindService(appName, serviceName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("bindService", resultCode, appName);
		}
	}

	// @Override
	// public void createAndUploadAndStartApplication(String appName, String
	// framework, int memory, File warFile, List<String> uris, List<String>
	// serviceNames) throws IOException {
	// int resultCode = HTTP_SUCCESS_CODE;
	// try {
	// super.createAndUploadAndStartApplication(appName, framework, memory,
	// warFile, uris, serviceNames);
	// } catch (HttpStatusCodeException he) {
	// resultCode = he.getStatusCode().value();
	// throw he;
	// } finally {
	// recordHttpResult("createAndUploadAndStartApplication", resultCode,
	// appName);
	// }
	// }

	@Override
	public void createService(CloudService service) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.createService(service);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("createService", resultCode);
		}
	}

	@Override
	public void deleteAllApplications() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteAllApplications();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("deleteAllApplications", resultCode);
		}
	}

	@Override
	public void deleteAllServices() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteAllServices();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("deleteAllServices", resultCode);
		}
	}

	@Override
	public void deleteApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteApplication(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("deleteApplication", resultCode, appName);
		}
	}

	@Override
	public void deleteService(String service) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteService(service);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("deleteService", resultCode);
		}
	}

	@Override
	public CloudApplication getApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplication(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getApplication", resultCode, appName);
		}
	}

	@Override
	public InstancesInfo getApplicationInstances(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplicationInstances(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getApplicationInstances", resultCode, appName);
		}
	}

	@Override
	public int[] getApplicationMemoryChoices() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplicationMemoryChoices();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getApplicationMemoryChoices", resultCode);
		}
	}

	@Override
	public List<CloudApplication> getApplications() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplications();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getApplications", resultCode);
		}
	}

	@Override
	public ApplicationStats getApplicationStats(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplicationStats(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getApplicationStats", resultCode, appName);
		}
	}

	@Override
	public URL getCloudControllerUrl() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getCloudControllerUrl();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getCloudControllerUrl", resultCode);
		}
	}

	@Override
	public CloudInfo getCloudInfo() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getCloudInfo();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getCloudInfo", resultCode);
		}
	}

	@Override
	public CrashesInfo getCrashes(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getCrashes(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getCrashes", resultCode, appName);
		}
	}

	@Override
	public String getFile(String appName, int instanceIndex, String filePath) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getFile(appName, instanceIndex, filePath);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getFile", resultCode, appName);
		}
	}

	@Override
	public CloudService getService(String service) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getService(service);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getService", resultCode);
		}
	}

	@Override
	public List<CloudServiceOffering> getServiceOfferings() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getServiceOfferings();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getServiceOfferings", resultCode);
		}
	}

	@Override
	public List<CloudService> getServices() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getServices();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("getServices", resultCode);
		}
	}

	@Override
	public String login() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.login();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("login", resultCode);
		}
	}

	// @Override
	// public String loginIfNeeded() {
	// int resultCode = 200;
	// try {
	// return super.loginIfNeeded();
	// } catch (HttpStatusCodeException he) {
	// resultCode = he.getStatusCode().value();
	// throw he;
	// } finally {
	// recordHttpResult("loginIfNeeded", resultCode);
	// }
	// }

	@Override
	public void register(String email, String password) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.register(email, password);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("register", resultCode);
		}
	}

	@Override
	public void rename(String appName, String newName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.rename(appName, newName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("rename", resultCode, appName);
		}
	}

	@Override
	public void restartApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.restartApplication(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("restartApplication", resultCode, appName);
		}
	}

	@Override
	public StartingInfo startApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.startApplication(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("startApplication", resultCode, appName);
		}
	}

	@Override
	public void stopApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.stopApplication(appName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("stopApplication", resultCode, appName);
		}
	}

	@Override
	public void unbindService(String appName, String serviceName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.unbindService(appName, serviceName);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("unbindService", resultCode, appName);
		}
	}

	@Override
	public void unregister() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.unregister();
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("unregister", resultCode);
		}
	}

	@Override
	public void updateApplicationInstances(String appName, int instances) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationInstances(appName, instances);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("updateApplicationInstances", resultCode, appName);
		}
	}

	@Override
	public void updateApplicationMemory(String appName, int memory) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationMemory(appName, memory);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("updateApplicationMemory", resultCode, appName);
		}
	}

	@Override
	public void updateApplicationServices(String appName, List<String> services) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationServices(appName, services);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("updateApplicationServices", resultCode, appName);
		}
	}

	@Override
	public void updateApplicationUris(String appName, List<String> uris) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationUris(appName, uris);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("updateApplicationUris", resultCode, appName);
		}
	}

	@Override
	public void uploadApplication(String appName, File warFile, UploadStatusCallback callback) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.uploadApplication(appName, warFile, callback);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("uploadApplication", resultCode, appName);
		}
	}

	@Override
	public void uploadApplication(String appName, File warFile) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.uploadApplication(appName, warFile);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("uploadApplication", resultCode, appName);
		}
	}

	@Override
	public void uploadApplication(String appName, String warFilePath) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.uploadApplication(appName, warFilePath);
		}
		catch (HttpStatusCodeException he) {
			resultCode = he.getStatusCode().value();
			throw he;
		}
		finally {
			recordHttpResult("uploadApplication", resultCode, appName);
		}
	}
}
