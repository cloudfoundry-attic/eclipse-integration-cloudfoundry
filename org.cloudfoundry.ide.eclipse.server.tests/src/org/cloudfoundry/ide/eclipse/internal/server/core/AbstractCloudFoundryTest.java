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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
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
		harness = createHarness();
		server = harness.createServer();
		cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		serverBehavior = (CloudFoundryServerBehaviour) server.loadAdapter(CloudFoundryServerBehaviour.class, null);
	}

	protected CloudFoundryOperations getClient() throws CoreException {
		CloudFoundryOperations client = serverBehavior.getClient(null);
		client.login();
		return client;
	}

	protected CloudFoundryOperations getClient(String token) throws CoreException {
		CloudCredentials credentials = new CloudCredentials(token);
		return getClient(credentials);
	}

	protected CloudFoundryOperations getClient(CloudCredentials credentials) throws CoreException {
		CloudFoundryOperations client = serverBehavior.getClient(credentials, null);
		client.login();
		return client;
	}

	@Override
	protected void tearDown() throws Exception {
		getClient().deleteAllApplications();
		harness.dispose();
	}

	protected String getContent(URI uri) throws Exception {
		Exception lastException = null;
		for (int i = 0; i < 180; i++) {
			try {
				// InputStream in = uri.toURL().openStream();
				CloudFoundryPlugin.trace("Probing " + uri);
				BufferedReader reader = new BufferedReader(new InputStreamReader(download(uri,
						new NullProgressMonitor())));
				try {
					return reader.readLine();
				}
				finally {
					reader.close();
				}
			}
			catch (FileNotFoundException e) {
				// ignore
				lastException = e;
				CloudFoundryPlugin.trace("Not found: " + uri);
			}
			catch (Exception e) {
				if (e.getCause() instanceof FileNotFoundException) {
					// ignore
					lastException = e;
					CloudFoundryPlugin.trace("Not found: " + uri);
				}
				else {
					throw e;
				}
			}
			Thread.sleep(1000);
		}
		// fail
		AssertionFailedError e = new AssertionFailedError("Failed to download " + uri + " within 3 min: 404 not found");
		e.initCause(lastException);
		throw e;
	}

	public InputStream download(java.net.URI uri, IProgressMonitor progressMonitor) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setUseCaches(false);
		return connection.getInputStream();
	}

	abstract protected Harness createHarness();

	protected CloudApplication createAndAssertTestApp() throws Exception {
		return createWebApplication("dynamic-webapp");
	}

	// Utility methods
	protected CloudApplication createWebApplication(String appName) throws Exception {

		harness.createProjectAndAddModule(appName);

		IModule module = getModule(appName);
		CloudApplication cloudApplication = deployAndStartModule(module);
		return cloudApplication;

	}

	protected void assertStartApplication(CloudApplication cloudApplication) throws Exception {

		boolean started = new StartApplicationInWaitOperation(cloudServer, "Starting test app").run(
				new NullProgressMonitor(), getClient(), cloudApplication);
		serverBehavior.refreshModules(new NullProgressMonitor());

		assertTrue(started);

	}

	protected void assertStopApplication(CloudApplication cloudApplication) throws Exception {
		boolean stopped = new StopApplicationInWaitOperation(cloudServer).run(new NullProgressMonitor(), getClient(),
				cloudApplication);
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

	protected CloudApplication getUpdatedApplication(String appName) throws CoreException {
		return serverBehavior.getApplication(appName, new NullProgressMonitor());
	}

	protected CloudApplication deployAndStartModule(IModule module) throws CoreException {
		serverBehavior.deployOrStartModule(new IModule[] { module }, true, null);

		// wait 1s until app is actually started
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
