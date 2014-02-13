/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.WaitForApplicationToStopOp;
import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public abstract class AbstractCloudFoundryTest extends TestCase {

	static final String DYNAMIC_WEBAPP_NAME = "basic-dynamic-webapp";

	protected Harness harness;

	protected IServer server;

	protected CloudFoundryServerBehaviour serverBehavior;

	protected CloudFoundryServer cloudServer;

	protected TestServlet testServlet;

	@Override
	protected void setUp() throws Exception {
		harness = createHarness();
		server = harness.createServer();
		cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		serverBehavior = (CloudFoundryServerBehaviour) server.loadAdapter(CloudFoundryServerBehaviour.class, null);

		harness.setup();
		connectClient();
	}

	/**
	 * Connects to the server via a session client that is reset in the CF
	 * server instance. Credentials are not changed, either locally or remotely.
	 * This only resets and reconnects the Java client used by the server
	 * instance in the test harness.
	 * @return
	 * @throws CoreException
	 */
	protected void connectClient() throws CoreException {
		connectClient(null);
	}

	/**
	 * Resets the client in the server behaviour based on the given credentials.
	 * @param username
	 * @param password
	 * @throws CoreException
	 */
	protected void connectClient(String username, String password) throws CoreException {
		CloudCredentials credentials = new CloudCredentials(username, password);
		connectClient(credentials);
	}

	/**
	 * Resets the client in the server behaviour based on the given credentials.
	 * If passing null, the stored server credentials will be used instead
	 * @param credentials
	 * @return
	 * @throws CoreException
	 */
	protected void connectClient(CloudCredentials credentials) throws CoreException {
		serverBehavior.disconnect(new NullProgressMonitor());
		serverBehavior.resetClient(credentials, new NullProgressMonitor());
		serverBehavior.connect(new NullProgressMonitor());
	}

	@Override
	protected void tearDown() throws Exception {
		serverBehavior.deleteAllApplications(new NullProgressMonitor());
		harness.dispose();
	}

	protected void assertIsApplicationRunningServerBehaviour(IModule module, CloudFoundryServerBehaviour serverBehaviour)
			throws Exception {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		int attempts = 5;
		long wait = 2000;
		boolean running = false;
		for (; !running && attempts > 0; attempts--) {
			running = serverBehaviour.isApplicationRunning(appModule, new NullProgressMonitor());

			if (!running) {
				try {
					Thread.sleep(wait);
				}
				catch (InterruptedException e) {

				}
			}

		}

		if (!running) {
			throw CloudErrorUtil.toCoreException("Application has not started after waiting for (ms): "
					+ (wait * attempts) + ". Unable to fetch content from application URL");
		}
	}

	protected String getContent(final URI uri, IModule module, CloudFoundryServerBehaviour behaviour) throws Exception {

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
		// wait for app to be running before fetching content
		assertIsApplicationRunningServerBehaviour(appModule, behaviour);

		BufferedReader reader = new BufferedReader(new InputStreamReader(download(uri, new NullProgressMonitor())));
		try {
			String val = reader.readLine();
			return val;
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}

	}

	public InputStream download(java.net.URI uri, IProgressMonitor progressMonitor) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setUseCaches(false);
		return connection.getInputStream();
	}

	abstract protected Harness createHarness() throws CoreException;

	protected CloudApplication createAndAssertTestApp() throws Exception {
		return createDefaultWebApplication(DYNAMIC_WEBAPP_NAME);
	}

	// Utility methods
	protected CloudApplication createDefaultWebApplication(String appName) throws Exception {

		harness.createProjectAndAddModule(appName);

		IModule module = getModule(appName);
		CloudApplication cloudApplication = deployAndStartModule(module, appName);
		return cloudApplication;
	}

	/**
	 * Creates the application and local application module, but does NOT deploy
	 * to the Cloud Foundry Server.
	 * @param appName
	 * @throws Exception
	 */
	protected void assertCreateLocalAppModule(String appName) throws Exception {
		harness.createProjectAndAddModule(appName);

		IModule[] modules = server.getModules();
		assertEquals(
				"Expected only 1 web application module created by local Cloud server instance, but got "
						+ Arrays.toString(modules) + ". Modules from previous deployments may be present.", 1,
				modules.length);
		int moduleState = server.getModulePublishState(modules);
		assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);
	}

	protected void assertStartApplication(CloudApplication cloudApplication) throws Exception {

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(cloudApplication.getName());
		assertNotNull(appModule);
		assertNotNull(appModule.getLocalModule());
		serverBehavior.startModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

	}

	protected void assertStopApplication(CloudApplication cloudApplication) throws Exception {

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(cloudApplication.getName());
		assertNotNull(appModule);
		assertNotNull(appModule.getLocalModule());
		serverBehavior.stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		boolean stopped = new WaitForApplicationToStopOp(cloudServer, appModule).run(new NullProgressMonitor());

		serverBehavior.refreshModules(new NullProgressMonitor());

		assertTrue(stopped);
	}

	protected void assertRemoveApplication(CloudApplication cloudApplication) throws Exception {
		IModule module = getModule(cloudApplication.getName());
		assertNotNull(module);
		serverBehavior.deleteModules(new IModule[] { module }, false, new NullProgressMonitor());

		serverBehavior.refreshModules(new NullProgressMonitor());
		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(cloudApplication.getName())) {
				found = true;
				break;
			}
		}
		assertFalse(found);
	}

	protected void assertApplicationIsRunning(IModule[] modules, String appName) throws Exception {
		int moduleState = server.getModuleState(modules);
		assertEquals(IServer.STATE_STARTED, moduleState);

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(modules[0]);
		assertNotNull("No Cloud Application mapping in Cloud module. Failed to refresh deployed application",
				appModule.getApplication());

		assertTrue(serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));
	}

	protected void assertWebApplicationURL(IModule[] modules, String appName) throws Exception {

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(modules[0]);
		assertNotNull("No Cloud Application mapping in Cloud module. Failed to refresh deployed application",
				appModule.getApplication());

		List<String> uris = appModule.getApplication().getUris();
		assertEquals(Collections.singletonList(harness.getExpectedURL(appName)), uris);
	}

	/**
	 * 
	 * @param appName
	 * @return Application if found, or null if application is no longer in
	 * server
	 */
	protected CloudApplication getUpdatedApplication(String appName) {
		try {
			return serverBehavior.getApplication(appName, new NullProgressMonitor());
		}
		catch (CoreException ce) {
			return null;
		}
	}

	protected CloudApplication deployAndStartModule(IModule module, String appName) throws Exception {
		serverBehavior.startModuleWaitForDeployment(new IModule[] { module }, null);

		assertApplicationIsRunning(new IModule[] { module }, appName);
		return getCloudApplication(module);
	}

	protected CloudApplication getCloudApplication(IModule module) throws CoreException {
		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		CloudApplication cloudApp = null;

		for (CloudApplication application : applications) {
			if (application.getName().equals(module.getName())) {
				cloudApp = application;
				break;
			}
		}
		return cloudApp;
	}

	protected IModule getModule(String appName) {
		IModule[] modules = server.getModules();
		for (IModule module : modules) {
			if (appName.equals(module.getName())) {
				return module;
			}
		}
		return null;
	}

}
