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
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public abstract class AbstractCloudFoundryTest extends TestCase {

	protected Harness harness;

	protected IServer server;

	protected CloudFoundryServerBehaviour serverBehavior;

	protected CloudFoundryServer cloudServer;

	protected TestServlet testServlet;

	@Override
	protected void setUp() throws Exception {
		harness = getTestFixture().harness();
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

	/**
	 * 
	 * Creates an application with the given prefix in the application based off
	 * a default web app project defined in the test fixture harness. NOTE that
	 * this assumes there is ONLY ONE app deployed in the server. Do not use
	 * this to test multiple application deployments in the same test case.
	 * @param appPrefix
	 * 
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule createPerTestWebApplication(String appPrefix) throws Exception {

		// NOTE: application name is NOT necessarily equal to the web app
		// project name
		// The application name can be different, and for test purposes, it is
		// different, and it's derived
		// from the given application name prefix, as defined by the harness
		// (typically by appending the prefix to the
		// project name)
		CloudFoundryTestFixture fixture = getTestFixture();

		fixture.configureForApplicationDeployment(appPrefix);

		// Create the default web project in the workspace and create the local
		// IModule for it. This does NOT create the application remotely, it
		// only
		// creates a "pre-deployment" WST IModule for the given project.
		IProject project = fixture.harness().createDefaultProjectAndAddModule();
		String projectName = fixture.harness().getDefaultWebAppProjectName();

		assertEquals(project.getName(), projectName);

		// There should only be one module available in the WST CF local server
		// instance
		IModule[] modules = server.getModules();
		assertEquals(
				"Expected only 1 web application module created by local Cloud server instance, but got "
						+ Arrays.toString(modules) + ". Modules from previous deployments may be present.", 1,
				modules.length);
		int moduleState = server.getModulePublishState(modules);
		assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);

		// Verify that the WST module that exists matches the app project app
		IModule module = getModule(projectName);

		assertNotNull(module);
		assertTrue(module.getName().equals(projectName));

		CloudFoundryApplicationModule appModule = assertCloudApplicationModuleIsValid(module, appPrefix);

		return appModule;
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

	protected CloudFoundryApplicationModule assertApplicationFromDefaultProjectIsRunning(String appPrefix)
			throws Exception {
		IModule module = getModule(getTestFixture().harness().getDefaultWebAppProjectName());
		int moduleState = server.getModuleState(new IModule[] { module });
		assertEquals(IServer.STATE_STARTED, moduleState);

		// Once the application is started, verify that the Cloud module (the
		// "Enhanced" WST
		// Imodule that contains additional CF related information) is valid,
		// and mapped to
		// an actual CloudApplication representing the deployed application.
		CloudFoundryApplicationModule appModule = assertCloudApplicationModuleIsValid(module, appPrefix);

		assertNotNull("No Cloud Application mapping in Cloud module. Failed to refresh deployed application",
				appModule.getApplication());

		// The app state in the cloud module must be correct
		assertEquals(IServer.STATE_STARTED, appModule.getState());

		assertTrue(serverBehavior.isApplicationRunning(appModule, new NullProgressMonitor()));

		assertRunningApplicationURL(module, appPrefix);

		return appModule;
	}

	protected CloudFoundryApplicationModule assertCloudApplicationModuleIsValid(IModule module, String appPrefix)
			throws Exception {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		assertNotNull(appModule);

		// The deployed application name in the Cloud module MUST match the
		// expected application name
		assertEquals(getTestFixture().harness().getDefaultWebAppName(appPrefix), appModule.getDeployedApplicationName());

		return appModule;
	}

	protected void assertRunningApplicationURL(IModule module, String appPrefix) throws Exception {

		CloudFoundryApplicationModule appModule = assertCloudApplicationModuleIsValid(module, appPrefix);

		List<String> uris = appModule.getApplication().getUris();
		assertEquals(Collections.singletonList(harness.getExpectedDefaultURL(appPrefix)), uris);
	}

	/**
	 * Finds the {@link CloudApplication} for an already deployed application
	 * with the given name. The name MUST be the full application name.
	 * @param appName. Must pass the FULL application name, not just the app
	 * name prefix.
	 * @return Application if found, or null if application is no longer in
	 * server
	 */
	protected CloudApplication getUpdatedApplication(String appName) throws CoreException {
		return serverBehavior.getApplication(appName, new NullProgressMonitor());

	}

	protected CloudFoundryApplicationModule assertDeployAndStartApplication(String appPrefix) throws Exception {
		Harness harness = getTestFixture().harness();

		String projectName = harness.getDefaultWebAppProjectName();

		String expectedAppName = harness.getDefaultWebAppName(appPrefix);

		IModule module = getModule(projectName);

		assertNotNull(module);

		serverBehavior.startModuleWaitForDeployment(new IModule[] { module }, null);

		CloudFoundryApplicationModule appModule = assertApplicationFromDefaultProjectIsRunning(appPrefix);

		// Do a separate check to verify that there is in fact a
		// CloudApplication for the
		// given app (i.e. verify that is is indeed deployed, even though this
		// has been checked
		// above, this is another way to verify all is OK.
		CloudApplication actualCloudApp = getUpdatedApplication(expectedAppName);

		assertNotNull(actualCloudApp);
		assertEquals(actualCloudApp.getName(), expectedAppName);

		return appModule;
	}

	protected IModule getModule(String projectName) {

		IModule[] modules = server.getModules();
		for (IModule module : modules) {
			if (projectName.equals(module.getName())) {
				return module;
			}
		}
		return null;
	}

	abstract protected CloudFoundryTestFixture getTestFixture() throws CoreException;

}
