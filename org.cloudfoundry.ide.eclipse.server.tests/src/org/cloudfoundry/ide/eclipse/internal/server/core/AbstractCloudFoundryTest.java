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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.WaitForApplicationToStopOp;
import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

/**
 * The base test setup for all Cloud Foundry tests involves creating a NEW
 * harness each time {@link #setUp()} is run, which mean, a new harness is
 * created for each test case. The harness is responsible for creating the
 * server instance used to deploy applications during each test case, so it is
 * IMPORTANT to use the same harness that is created during setup through the
 * entire test case, including any helper methods invoked by the test case.
 * 
 */
public abstract class AbstractCloudFoundryTest extends TestCase {

	/**
	 * IMPORTANT: Since each {@link #setUp()} is invoked on EACH test case, be
	 * sure to reference the harness created during setup as to use the same
	 * harness that created the server instance throughout all the helper
	 * methods that may be invoked by a particular test case.
	 */
	protected Harness harness;

	protected IServer server;

	protected CloudFoundryServerBehaviour serverBehavior;

	protected CloudFoundryServer cloudServer;

	protected TestServlet testServlet;

	@Override
	protected void setUp() throws Exception {

		// This same harness MUST be used through out the entire lifetime of the
		// test case,
		// therefore if the test case or any helper methods need to reference
		// the harness
		// they must all reference this instance variable.
		harness = getTestFixture().createHarness();
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

	protected void assertApplicationIsRunning(IModule module, String prefix) throws Exception {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		assertApplicationIsRunning(appModule);
		// Verify that the Application URL is set
		List<String> uris = appModule.getApplication().getUris();
		assertEquals(Collections.singletonList(harness.getExpectedDefaultURL(prefix)), uris);
	}

	protected void assertApplicationIsRunning(CloudFoundryApplicationModule appModule) throws Exception {
		int attempts = 5;
		long wait = 2000;
		boolean running = false;

		// Test the Server behaviour API that checks if application is running
		for (; !running && attempts > 0; attempts--) {
			running = cloudServer.getBehaviour().isApplicationRunning(appModule, new NullProgressMonitor());

			if (!running) {
				try {
					Thread.sleep(wait);
				}
				catch (InterruptedException e) {

				}
			}

		}

		// Verify separately that the app did indeed start
		assertTrue("Application has not started after waiting for (ms): " + (wait * attempts), running);
		assertEquals(IServer.STATE_STARTED, appModule.getState());
		assertEquals(AppState.STARTED, appModule.getApplication().getState());

	}

	/**
	 * 
	 * Creates an application project based on the default project name defined
	 * in the test fixture harness. Does NOT deploy the application. Also
	 * creates the {@link IModule} associated with the application (but it does
	 * not create the actual Cloud Foundry module or application, as these are
	 * only created upon actual deployment)
	 * 
	 * @throws Exception
	 */
	protected void createWebApplicationProject() throws Exception {

		// Create the default web project in the workspace and create the local
		// IModule for it. This does NOT create the application remotely, it
		// only
		// creates a "pre-deployment" WST IModule for the given project.
		IProject project = harness.createDefaultProjectAndAddModule();
		String projectName = harness.getDefaultWebAppProjectName();

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

	}

	protected void assertStartModule(CloudFoundryApplicationModule appModule) throws Exception {

		serverBehavior.startModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		assertApplicationIsRunning(appModule);

	}

	protected void assertRestartModule(CloudFoundryApplicationModule appModule) throws Exception {

		serverBehavior.restartModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		assertApplicationIsRunning(appModule);

	}

	protected void assertStopModule(CloudFoundryApplicationModule appModule) throws Exception {
		serverBehavior.stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		boolean stopped = new WaitForApplicationToStopOp(cloudServer, appModule).run(new NullProgressMonitor());
		assertTrue("Expected application to be stopped", stopped);
		assertTrue("Expected application to be stopped", appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

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

	/**
	 * Asserts that the application is deployed although it may not necessarily
	 * be started.
	 * @param appPrefix
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule assertApplicationIsDeployed(String appPrefix) throws Exception {
		// Get the local WST IModule. NOTE that the PROJECT name needs to be
		// used as opposed to the
		// app name, as the project name and app name may differ, and the
		// IModule is mapped to the project.
		IModule module = getModule(harness.getDefaultWebAppProjectName());
		int moduleState = server.getModuleState(new IModule[] { module });
		assertEquals(IServer.STATE_STARTED, moduleState);

		// Once the application is started, verify that the Cloud module (the
		// "Enhanced" WST
		// Imodule that contains additional CF related information) is valid,
		// and mapped to
		// an actual CloudApplication representing the deployed application.
		CloudFoundryApplicationModule appModule = assertCloudFoundryModuleExists(module, appPrefix);

		assertNotNull("No Cloud Application mapping in Cloud module. Failed to refresh deployed application",
				appModule.getApplication());

		// The app state in the cloud module must be correct
		assertEquals(IServer.STATE_STARTED, appModule.getState());

		return appModule;
	}

	/**
	 * Verifies that a {@link CloudFoundryApplicationModule} associated with the
	 * given {@link IModule} exists. Note that
	 * {@link CloudFoundryApplicationModule} are ONLY created after an attempted
	 * deployment or application start/restart. This should only be called after
	 * an app has been:
	 * <p/>
	 * 1. Created locally
	 * <p/>
	 * 2. Deployed or started
	 * @param module
	 * @param appPrefix
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule assertCloudFoundryModuleExists(IModule module, String appPrefix)
			throws Exception {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		// The deployed application name in the Cloud module MUST match the
		// expected application name
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModule.getDeployedApplicationName());

		return appModule;
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

	/**
	 * Deploys application and starts it in the CF server.
	 * @param appPrefix
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule deployApplicationStartMode(String appPrefix) throws Exception {
		return deployApplication(appPrefix, false);
	}

	/**
	 * Deploys an app with the given prefix name and asserts it is deployed
	 * @param appPrefix
	 * @param deployStopped if the app is to be deployed in stopped mode.
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule deployApplication(String appPrefix, boolean deployStopped) throws Exception {

		String projectName = harness.getDefaultWebAppProjectName();

		String expectedAppName = harness.getDefaultWebAppName(appPrefix);

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(expectedAppName, deployStopped);

		IModule module = getModule(projectName);

		assertNotNull("Expected non-null IModule when deploying application", module);

		serverBehavior.startModuleWaitForDeployment(new IModule[] { module }, null);

		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(appPrefix);

		// Do a separate check to verify that there is in fact a
		// CloudApplication for the
		// given app (i.e. verify that is is indeed deployed, even though this
		// has been checked
		// above, this is another way to verify all is OK.
		CloudApplication actualCloudApp = getUpdatedApplication(expectedAppName);

		assertNotNull("Expected non-null CloudApplication when checking if application is deployed", actualCloudApp);
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
