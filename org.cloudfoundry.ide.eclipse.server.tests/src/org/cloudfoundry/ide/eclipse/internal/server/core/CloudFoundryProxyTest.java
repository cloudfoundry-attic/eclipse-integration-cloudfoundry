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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ProxyHandler;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.springframework.web.client.ResourceAccessException;

public class CloudFoundryProxyTest extends AbstractCloudFoundryTest {

	public static final String VALID_V1_HTTP_URL = "http://api.cloudfoundry.com";

	public static final String VALID_V1_HTTPS_URL = "https://api.cloudfoundry.com";

	public void testCreateApplicationInvalidProxyWithClientReset() throws Exception {
		// ensure valid session
		getClient();

		final boolean[] ran = { false };
		new ProxyHandler("invalid.proxy.test", 8080) {

			@Override
			protected void handleProxyChange() {
				serverBehavior.resetClient();

				// Create app. Should fail
				CloudFoundryOperations client = null;
				try {
					List<String> uris = new ArrayList<String>();
					uris.add("test-proxy-upload.cloudfoundry.com");
					client = getClient();
					client.createApplication("test", DeploymentConstants.SPRING, 128, uris, new ArrayList<String>());
					fail("Expected ResourceAccessException due to invalid proxy configuration");
				}
				catch (Exception e) {
					assertTrue("Expected ResourceAccessException, got: " + e, e instanceof ResourceAccessException);
					assertEquals("invalid.proxy.test", e.getCause().getMessage());
					ran[0] = true;
				}

				assertNull("Expected no client due to invalid proxy", client);
			}

		}.run();

		assertTrue(ran[0]);
	}

	public void testCreateApplicationInvalidProxyWithoutClientReset() throws Exception {
		// ensure valid session
		getClient();

		harness.createProjectAndAddModule("dynamic-webapp");

		final IModule[] modules = server.getModules();
		assertEquals("Expected dynamic-webapp module, got " + Arrays.toString(modules), 1, modules.length);
		int moduleState = server.getModulePublishState(modules);
		assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);

		serverBehavior.deployOrStartModule(modules, true, null);
		moduleState = server.getModuleState(modules);
		assertEquals(IServer.STATE_STARTED, moduleState);
		CloudApplication cloudApplication = getCloudApplication(modules[0]);
		assertEquals(cloudApplication.getState(), CloudApplication.AppState.STARTED);

		final boolean[] ran = { false };

		new ProxyHandler("invalid.proxy.test", 8080) {

			@Override
			protected void handleProxyChange() throws CoreException {
				IProxyService proxyService = getProxyService();
				try {
					// Should fail, as its now going through invalid proxy
					serverBehavior.stopModule(modules, null);

					getCloudApplication(modules[0]);
					fail("Expected invalid.proxy.test failure");

				}
				catch (Exception e) {
					assertTrue(e.getCause().getMessage().contains("invalid.proxy.test"));
					ran[0] = true;
				}

				// Restore proxy settings and try again
				proxyService.setSystemProxiesEnabled(getOriginalSystemProxiesEnabled());
				proxyService.setProxiesEnabled(getOriginalProxiesEnabled());
				proxyService.setProxyData(getOriginalProxyData());

				serverBehavior.stopModule(modules, null);

				int moduleState = server.getModuleState(modules);
				assertEquals(IServer.STATE_STOPPED, moduleState);
				CloudApplication cloudApplication = getCloudApplication(modules[0]);
				assertEquals(cloudApplication.getState(), CloudApplication.AppState.STOPPED);
			}

		}.run();

		assertTrue(ran[0]);
	}

	public void testNoProxySetHTTP() throws Exception {

		new ProxyHandler(null, -1, false, IProxyData.HTTP_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTP_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTPS_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudUtil.toCoreException(e);
				}

			}
		};
	}

	public void testNoProxySetHTTPS() throws Exception {

		new ProxyHandler(null, -1, false, IProxyData.HTTPS_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory.getProxy(new URL(
							VALID_V1_HTTPS_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTP_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxySetDirectHTTPS() throws Exception {
		// Direct provider means not using proxy settings even if they are set.
		// To set direct provider, disable proxies even when set
		boolean enableProxies = false;
		new ProxyHandler("invalid.proxy.test", 8080, enableProxies, IProxyData.HTTPS_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory.getProxy(new URL(
							VALID_V1_HTTPS_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTP_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxyDirectSetHTTP() throws Exception {
		// Direct provider means not using proxy settings even if they are set.
		// To set direct provider, disable proxies even when set
		boolean enableProxies = false;
		new ProxyHandler("invalid.proxy.test", 8080, enableProxies, IProxyData.HTTP_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTP_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTPS_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxyManualSetHTTP() throws Exception {

		boolean setProxy = true;
		new ProxyHandler("invalid.proxy.test", 8080, setProxy, IProxyData.HTTP_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTP_URL));
					assertNotNull(configuration);
					assertTrue(configuration.getProxyHost().equals("invalid.proxy.test"));
					assertTrue(configuration.getProxyPort() == (8080));

					// verify it is null for https, since proxy lookup should
					// match the specified
					// protocol in the given URL (i.e. HTTP proxy -> HTTP URL,
					// HTTPS proxy -> HTTPS URL)
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTPS_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxySetManualHTTPS() throws Exception {
		boolean setProxy = true;
		new ProxyHandler("invalid.proxy.test", 8080, setProxy, IProxyData.HTTPS_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory.getProxy(new URL(
							VALID_V1_HTTPS_URL));
					assertNotNull(configuration);
					assertTrue(configuration.getProxyHost().equals("invalid.proxy.test"));
					assertTrue(configuration.getProxyPort() == (8080));

					// verify it is null for http, since proxy lookup should
					// match the specified
					// protocol in the given URL (i.e. HTTP proxy -> HTTP URL,
					// HTTPS proxy -> HTTPS URL)
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTP_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudUtil.toCoreException(e);
				}

			}
		}.run();
	}

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

}
