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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudOrgsAndSpaces;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ServerHandler;
import org.cloudfoundry.ide.eclipse.server.tests.AllCloudFoundryTests;
import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet;
import org.cloudfoundry.ide.eclipse.server.tests.server.WebApplicationContainerBean;
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

	public static final String PASSWORD_PROPERTY = "password";

	public static final String USEREMAIL_PROPERTY = "username";

	public static final String ORG_PROPERTY = "org";

	public static final String SPACE_PROPERTY = "space";

	public static final String CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY = "test.credentials";

	public static final String CF_PIVOTAL_SERVER_URL_HTTP = "http://api.run.pivotal.io";

	public static final String CF_PIVOTAL_SERVER_URL_HTTPS = "https://api.run.pivotal.io";

	public class Harness {

		private boolean projectCreated;

		private IServer server;

		private WebApplicationContainerBean webContainer;

		private String applicationDomain;

		public IProject createProjectAndAddModule(String projectName) throws Exception {
			IProject project = createProject(projectName);
			projectCreated = true;
			addModule(project);
			return project;
		}

		protected String getDomain() throws CoreException {
			if (applicationDomain == null) {

				List<CloudDomain> domains = getBehaviour().getDomainsForSpace(new NullProgressMonitor());

				// Get a default domain
				applicationDomain = domains.get(0).getName();
				applicationDomain = applicationDomain.replace("http://", "");
			}
			return applicationDomain;
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
			CredentialProperties credentials = getCredentials();
			cloudFoundryServer.setPassword(credentials.password);
			cloudFoundryServer.setUsername(credentials.userEmail);

			cloudFoundryServer.setUrl(getUrl());

			setDefaultCloudSpace(cloudFoundryServer, credentials.organization, credentials.space);

			serverWC.save(true, null);
			return server;
		}

		protected void setDefaultCloudSpace(CloudFoundryServer cloudServer, String orgName, String spaceName)
				throws CoreException {
			CloudOrgsAndSpaces spaces = CloudUiUtil.getCloudSpaces(cloudServer.getUsername(),
					cloudServer.getPassword(), cloudServer.getUrl(), false, null);
			Assert.isTrue(spaces != null, "Failed to resolve orgs and spaces.");
			Assert.isTrue(spaces.getDefaultCloudSpace() != null,
					"No default space selected in cloud space lookup handler.");

			CloudSpace cloudSpace = spaces.getSpace(orgName, spaceName);
			if (cloudSpace == null) {
				throw CloudErrorUtil.toCoreException("Failed to resolve cloud space when running junits: " + orgName
						+ " - " + spaceName);
			}
			cloudServer.setSpace(cloudSpace);
		}

		/**
		 * 
		 * @return standalone client based on the harness credentials, org and
		 * space. This is not the client used by the server instance, but a new
		 * client for testing purposes only.
		 */
		public CloudFoundryOperations createStandaloneClient() throws CoreException {
			CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class,
					null);
			CloudFoundrySpace space = cloudFoundryServer.getCloudFoundrySpace();
			if (space == null) {
				throw CloudErrorUtil.toCoreException("No org and space was set in test harness for: "
						+ cloudFoundryServer.getServerId());
			}
			try {
				return CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(
						new CloudCredentials(cloudFoundryServer.getUsername(), cloudFoundryServer.getPassword()),
						new URL(cloudFoundryServer.getUrl()), space.getOrgName(), space.getSpaceName());
			}
			catch (MalformedURLException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}

		public String getUrl() {
			if (webContainer != null) {
				return "http://localhost:" + webContainer.getPort() + "/";
			}
			else {
				return url;
			}
		}

		protected CloudFoundryServerBehaviour getBehaviour() {
			return (CloudFoundryServerBehaviour) server.loadAdapter(CloudFoundryServerBehaviour.class, null);
		}

		public void setup() throws CoreException {
			// Clean up all projects from workspace
			StsTestUtil.cleanUpProjects();

			// Perform clean up on existing published apps and services
			if (server != null) {
				CloudFoundryServerBehaviour serverBehavior = getBehaviour();
				// Delete all applications
				serverBehavior.deleteAllApplications(null);

				// Delete all services
				deleteAllServices();

			}
		}

		public void deleteService(CloudService serviceToDelete) throws CoreException {
			CloudFoundryServerBehaviour serverBehavior = getBehaviour();

			String serviceName = serviceToDelete.getName();
			List<String> services = new ArrayList<String>();
			services.add(serviceName);

			serverBehavior.getDeleteServicesOperation(services).run(new NullProgressMonitor());
		}

		public void deleteAllServices() throws CoreException {
			List<CloudService> services = getAllServices();
			for (CloudService service : services) {
				deleteService(service);
				CloudFoundryTestUtil.waitIntervals(1000);
			}
		}

		public List<CloudService> getAllServices() throws CoreException {
			List<CloudService> services = getBehaviour().getServices(new NullProgressMonitor());
			if (services == null) {
				services = new ArrayList<CloudService>(0);
			}
			return services;
		}

		public void dispose() throws CoreException {
			if (webContainer != null) {

				// FIXNS: Commented out because of STS-3159
				// webContainer.stop();
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

		public String getExpectedURL(String projectName) throws CoreException {
			return projectName + "." + getDomain();
		}

		public TestServlet startMockServer() throws Exception {
			Bundle bundle = Platform.getBundle(AllCloudFoundryTests.PLUGIN_ID);
			URL resourceUrl = bundle.getResource("webapp");
			URL localURL = FileLocator.toFileURL(resourceUrl);
			File file = new File(localURL.getFile());
			webContainer = new WebApplicationContainerBean(file);

			// FIXNS: Commented out because of STS-3159
			// webContainer.start();
			return getServer();
		}

		public TestServlet getServer() {
			return TestServlet.getInstance();
		}

	}

	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.tests";

	private static CloudFoundryTestFixture current;

	private static CloudFoundryTestFixture getTestFixture() throws CoreException {
		if (current == null) {
			current = new CloudFoundryTestFixture("run.pivotal.io");
		}
		return current;
	}

	public static CloudFoundryTestFixture current() throws CoreException {
		CloudFoundryPlugin.setCallback(new TestCallback());
		return getTestFixture();
	}

	public static CloudFoundryTestFixture current(String appName) throws CoreException {
		CloudFoundryPlugin.setCallback(new TestCallback(appName));
		return getTestFixture();
	}

	public static CloudFoundryTestFixture current(String appName, String url) throws CoreException {
		CloudFoundryPlugin.setCallback(new TestCallback(appName, url));
		return getTestFixture();
	}

	public static CloudFoundryTestFixture currentLocalDebug() {
		CloudFoundryPlugin.setCallback(new TestCallback());
		return null;
	}

	private final ServerHandler handler;

	private static CredentialProperties credentials;

	private final String url;

	public CloudFoundryTestFixture(String serverDomain) {

		this.url = "http://api." + serverDomain;

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

	public CredentialProperties getCredentials() throws CoreException {
		if (credentials == null) {
			credentials = getUserTestCredentials();
		}
		return credentials;
	}

	public String getUrl() {
		return url;
	}

	public Harness harness() {
		return new Harness();
	}

	public static class CredentialProperties {

		public final String userEmail;

		public final String password;

		public final String organization;

		public final String space;

		public CredentialProperties(String userEmail, String password, String organization, String space) {
			this.userEmail = userEmail;
			this.password = password;
			this.organization = organization;
			this.space = space;
		}

	}

	/**
	 * Returns non-null credentials, although values of the credentials may be
	 * empty if failed to read credentials
	 * @return
	 */
	private static CredentialProperties getUserTestCredentials() throws CoreException {
		String propertiesLocation = System.getProperty(CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY);
		String userEmail = null;
		String password = null;
		String org = null;
		String space = null;
		if (propertiesLocation != null) {

			File propertiesFile = new File(propertiesLocation);

			InputStream fileInputStream = null;
			try {
				if (propertiesFile.exists() && propertiesFile.canRead()) {
					fileInputStream = new FileInputStream(propertiesFile);
					Properties properties = new Properties();
					properties.load(fileInputStream);
					userEmail = properties.getProperty(USEREMAIL_PROPERTY);
					password = properties.getProperty(PASSWORD_PROPERTY);
					org = properties.getProperty(ORG_PROPERTY);
					space = properties.getProperty(SPACE_PROPERTY);
				}
			}
			catch (FileNotFoundException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
			finally {
				try {
					if (fileInputStream != null) {
						fileInputStream.close();
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (userEmail == null || password == null) {
			userEmail = System.getProperty("vcap.email", "");
			password = System.getProperty("vcap.passwd", "");
		}

		String missingInfo = "";
		if (userEmail == null) {
			missingInfo += "-Username-";
		}

		if (password == null) {
			missingInfo += "-Password-";
		}

		if (org == null) {
			missingInfo += "-Org-";
		}

		if (space == null) {
			missingInfo += "-Space-";
		}

		if (missingInfo.length() > 0) {
			missingInfo = "Failed to run tests due to missing information: " + missingInfo;
			throw CloudErrorUtil
					.toCoreException(missingInfo
							+ ". Ensure Cloud Foundry credentials are set as properties in a properties file and passed as an argument to the VM using \"-D"
							+ CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY + "=[full file location]\"");
		}

		return new CredentialProperties(userEmail, password, org, space);

	}
}
