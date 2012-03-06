package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.net.HttpURLConnection;
import java.net.URI;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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

	protected CloudFoundryClient getClient() throws CoreException {
		CloudFoundryClient client = serverBehavior.getClient();
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

}
