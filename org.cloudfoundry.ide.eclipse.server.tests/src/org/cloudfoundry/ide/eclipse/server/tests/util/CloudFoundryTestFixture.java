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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.tests.AllCloudFoundryTests;
import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet;
import org.cloudfoundry.ide.eclipse.server.tests.server.WebApplicationContainerBean;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ServerDescriptor;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ServerHandler;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.StsTestUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.osgi.framework.Bundle;

/**
 * Holds connection properties and provides utility methods for testing.
 * 
 * @author Steffen Pingel
 */
public class CloudFoundryTestFixture {

	public class Harness {

		private boolean projectCreated;

		private IServer server;

		private WebApplicationContainerBean webContainer;

		public IProject createProjectAndAddModule(String projectName) throws Exception {
			IProject project = createProject(projectName);
			projectCreated = true;
			addModule(project);
			return project;
		}

		public void addModule(IProject project) throws CoreException {
			Assert.isNotNull(server, "Invoke createServer() first");

			IModule[] modules = ServerUtil.getModules(project);
			IServerWorkingCopy wc = server.createWorkingCopy();
			wc.modifyModules(modules, new IModule[0], null);
			wc.save(true, null);
		}

		public IProject createProject(String projectName) throws CoreException, IOException {
			return StsTestUtil.createPredefinedProject(projectName, CloudFoundryTestFixture.PLUGIN_ID);
		}

		public IServer createServer() throws CoreException {
			Assert.isTrue(server == null, "createServer() already invoked");

			server = handler.createServer(new NullProgressMonitor(), ServerHandler.ALWAYS_OVERWRITE);
			IServerWorkingCopy serverWC = server.createWorkingCopy();
			CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class,
					null);
			cloudFoundryServer.setPassword(password);
			cloudFoundryServer.setUsername(username);
			cloudFoundryServer.setUrl(getUrl());
			serverWC.save(true, null);
			return server;
		}

		public String getUrl() {
			if (webContainer != null) {
				return "http://localhost:" + webContainer.getPort() + "/";
			}
			else {
				return url;
			}
		}

		public void dispose() throws CoreException {
			if (webContainer != null) {
				webContainer.stop();
			}

			if (server != null) {
				CloudFoundryServerBehaviour cloudFoundryServer = (CloudFoundryServerBehaviour) server.loadAdapter(
						CloudFoundryServerBehaviour.class, null);
				if (projectCreated) {
					try {
						cloudFoundryServer.deleteAllApplications(null);
					}
					catch (CoreException e) {
						e.printStackTrace();
					}
				}
				try {
					cloudFoundryServer.disconnect(null);
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
			}
			try {
				handler.deleteServerAndRuntime(new NullProgressMonitor());
			}
			catch (CoreException e) {
				e.printStackTrace();
			}

			if (projectCreated) {
				StsTestUtil.deleteAllProjects();
			}
		}

		public String getUrl(String projectName) {
			return projectName + "." + domain;
		}

		public TestServlet startMockServer() throws Exception {
			Bundle bundle = Platform.getBundle(AllCloudFoundryTests.PLUGIN_ID);
			URL resourceUrl = bundle.getResource("webapp");
			URL localURL = FileLocator.toFileURL(resourceUrl);
			File file = new File(localURL.getFile());
			webContainer = new WebApplicationContainerBean(file);
			webContainer.start();
			return getServer();
		}

		public TestServlet getServer() {
			return TestServlet.getInstance();
		}

	}

	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.tests";

	private static final String DOMAIN = System.getProperty("vcap.target", "cloudfoundry.com");

	private static final String USER_EMAIL = System.getProperty("vcap.email", "java-client-test-user@vmware.com");

	private static final String USER_PASS = System.getProperty("vcap.passwd", "test-pass");

	public static final CloudFoundryTestFixture VCLOUDLABS = new CloudFoundryTestFixture(DOMAIN, USER_EMAIL, USER_PASS);

	public static final CloudFoundryTestFixture LOCAL = new CloudFoundryTestFixture("localhost", "user", "password");

	public static final CloudFoundryTestFixture LOCAL_CLOUD = new CloudFoundryTestFixture("vcap.me", USER_EMAIL,
			USER_PASS);

	private static CloudFoundryTestFixture current = VCLOUDLABS;

	public static CloudFoundryTestFixture current() {
		CloudFoundryPlugin.setCallback(new TestCallback());
		return current;
	}

	public static CloudFoundryTestFixture current(String appName) {
		CloudFoundryPlugin.setCallback(new TestCallback(appName));
		return current;
	}

	public static CloudFoundryTestFixture current(String appName, String url) {
		CloudFoundryPlugin.setCallback(new TestCallback(appName, url));
		return current;
	}

	public static CloudFoundryTestFixture currentLocalDebug() {
		CloudFoundryPlugin.setCallback(new TestCallback());
		return LOCAL_CLOUD;
	}

	private final String domain;

	private final ServerHandler handler;

	private final String password;

	private final String url;

	private final String username;

	public CloudFoundryTestFixture(String domain, String username, String password) {
		this.domain = domain;
		this.url = "http://api." + domain;
		this.username = username;
		this.password = password;

		ServerDescriptor descriptor = new ServerDescriptor("server") {
			{
				setRuntimeTypeId("org.cloudfoundry.cloudfoundryserver.test.runtime.10");
				setServerTypeId("org.cloudfoundry.cloudfoundryserver.test.10");
				setRuntimeName("Cloud Foundry Test Runtime");
				setServerName("Cloud Foundry Test Server");
				setForceCreateRuntime(true);
			}
		};
		handler = new ServerHandler(descriptor);
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public Harness harness() {
		return new Harness();
	}

}
