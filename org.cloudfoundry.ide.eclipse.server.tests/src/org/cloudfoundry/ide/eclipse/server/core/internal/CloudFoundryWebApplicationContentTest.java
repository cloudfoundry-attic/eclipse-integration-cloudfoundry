/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal;

import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;

/**
 * Disabled for now until web content can be retrieved for an application URL
 */
public class CloudFoundryWebApplicationContentTest extends AbstractCloudFoundryTest {

	// public void testStartModuleCheckWebContent() throws Exception {
	//
	// String prefix = "startModuleCheckWebContent";
	// createPerTestWebApplication(prefix);
	//
	// assertApplicationIsRunning(module, appPrefix)
	//
	// IModule[] modules = server.getModules();
	//
	// serverBehavior.startModuleWaitForDeployment(modules, new
	// NullProgressMonitor());
	//
	// assertApplicationIsRunning(modules, DYNAMIC_WEBAPP_NAME);
	// URI uri = new URI("http://" +
	// harness.getExpectedURL(AbstractCloudFoundryTest.DYNAMIC_WEBAPP_NAME)
	// + "/index.html");
	// assertEquals("Hello World.", getContent(uri, modules[0],
	// serverBehavior));
	// }
	//
	// public void testStartModuleWithUsedUrl() throws Exception {
	//
	// assertCreateLocalAppModule(AbstractCloudFoundryTest.DYNAMIC_WEBAPP_NAME);
	// IModule[] modules = server.getModules();
	//
	// serverBehavior.startModuleWaitForDeployment(modules, new
	// NullProgressMonitor());
	//
	// assertWebApplicationURL(modules,
	// AbstractCloudFoundryTest.DYNAMIC_WEBAPP_NAME);
	// assertApplicationIsRunning(modules,
	// AbstractCloudFoundryTest.DYNAMIC_WEBAPP_NAME);
	//
	// CloudFoundryApplicationModule module =
	// cloudServer.getExistingCloudModule(modules[0]);
	//
	// Assert.assertNull(module.getErrorMessage());
	//
	// harness =
	// CloudFoundryTestFixture.current("dynamic-webapp-with-appclient-module",
	// harness.getExpectedURL(AbstractCloudFoundryTest.DYNAMIC_WEBAPP_NAME)).harness();
	// server = harness.createServer();
	// cloudServer = (CloudFoundryServer)
	// server.loadAdapter(CloudFoundryServer.class, null);
	// serverBehavior = (CloudFoundryServerBehaviour)
	// server.loadAdapter(CloudFoundryServerBehaviour.class, null);
	//
	// harness.createProjectAndAddModule("dynamic-webapp-with-appclient-module");
	// modules = server.getModules();
	//
	// // FIXME: once we verify what the proper behavior is, we should fail
	// // appropriately
	// // try {
	// // serverBehavior.deployOrStartModule(modules, true, null);
	// // Assert.fail("Expects CoreException due to duplicate URL");
	// // }
	// // catch (CoreException e) {
	// // }
	// //
	// // module = cloudServer.getApplication(modules[0]);
	// // Assert.assertNotNull(module.getErrorMessage());
	// // try {
	// //
	// serverBehavior.getClient().deleteApplication("dynamic-webapp-with-appclient-module");
	// // }
	// // catch (Exception e) {
	// //
	// // }
	// }
	//
	// // This case should never pass since the wizard should guard against
	// // duplicate ID
	// public void testStartModuleWithDuplicatedId() throws Exception {
	//
	// createPerTestDefaultWebApplication("startModuleDuplicateId");
	//
	// // There should only be one module
	// IModule[] modules = server.getModules();
	//
	// serverBehavior.startModuleWaitForDeployment(modules, new
	// NullProgressMonitor());
	//
	// assertWebApplicationURL(modules, "startModuleDuplicateId");
	// assertApplicationIsRunning(modules, "startModuleDuplicateId");
	//
	// serverBehavior.refreshModules(new NullProgressMonitor());
	// List<CloudApplication> applications = serverBehavior.getApplications(new
	// NullProgressMonitor());
	// boolean found = false;
	//
	// for (CloudApplication application : applications) {
	// if (application.getName().equals("dynamic-webapp-test")) {
	// found = true;
	// }
	// }
	//
	// Assert.assertTrue(found);
	//
	// harness.createProjectAndAddModule("dynamic-webapp-with-appclient-module");
	//
	// modules = server.getModules();
	// serverBehavior.startModuleWaitForDeployment(modules, new
	// NullProgressMonitor());
	//
	// // wait 1s until app is actually started
	// URI uri = new URI("http://" +
	// harness.getExpectedURL("dynamic-webapp-with-appclient-module") +
	// "/index.html");
	// assertEquals("Hello World.", getContent(uri, modules[0],
	// serverBehavior));
	//
	// serverBehavior.refreshModules(new NullProgressMonitor());
	// applications = serverBehavior.getApplications(new NullProgressMonitor());
	// found = false;
	//
	// for (CloudApplication application : applications) {
	// if (application.getName().equals("dynamic-webapp-test")) {
	// found = true;
	// }
	// }
	//
	// Assert.assertTrue(found);
	// }

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

	// protected String getContent(final URI uri, IModule module) throws
	// Exception {
	//
	// CloudFoundryApplicationModule appModule =
	// cloudServer.getExistingCloudModule(module);
	// // wait for app to be running before fetching content
	// assertApplicationIsRunning(appModule);
	//
	// BufferedReader reader = new BufferedReader(new
	// InputStreamReader(download(uri, new NullProgressMonitor())));
	// try {
	// String val = reader.readLine();
	// return val;
	// }
	// finally {
	// if (reader != null) {
	// reader.close();
	// }
	// }
	//
	// }
	//
	// public InputStream download(java.net.URI uri, IProgressMonitor
	// progressMonitor) throws IOException {
	// HttpURLConnection connection = (HttpURLConnection)
	// uri.toURL().openConnection();
	// connection.setUseCaches(false);
	// return connection.getInputStream();
	// }

}
