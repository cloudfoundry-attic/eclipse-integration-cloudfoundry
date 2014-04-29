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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.net.URL;

import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.CredentialProperties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;

/**
 * @author Terry Denney
 */
public class CloudFoundryClientConnectionTest extends TestCase {

	public void testConnectToNonSecureUrl() throws Exception {
		CloudFoundryOperations client = CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTP);

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testConnectToSecureUrl() throws Exception {
		CloudFoundryOperations client = CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTPS);
		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testValidPasswordOperationHandler() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();
		CloudCredentials cloudCredentials = new CloudCredentials(credentials.userEmail, credentials.password);

		CloudFoundryOperations client = createClient(cloudCredentials,
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTP);
		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			fail("Failed to log in due to: " + e.getMessage());
		}

	}

	public void testInvalidPasswordOperationHandler() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidPassword = "invalid password";

		CloudFoundryOperations client = createClient(new CloudCredentials(credentials.userEmail, invalidPassword),
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTP);

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);
		CoreException error = null;

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			error = e;
		}

		assertError(error);
	}

	public void testInvalidUsernameOperationHandler() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidUsername = "invalid username";

		CloudFoundryOperations client = createClient(new CloudCredentials(invalidUsername, credentials.password),
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTP);

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);
		CoreException error = null;

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			error = e;
		}

		assertError(error);
	}

	public void testInvalidAndValidCredentials() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidUsername = "invalid username";

		CloudFoundryOperations client = createClient(new CloudCredentials(invalidUsername, credentials.password),
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTP);

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);
		CoreException error = null;

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			error = e;
		}

		assertError(error);

		// CREATE a separate client to test valid connection. the purpose here
		// is to ensure that the server does not retain the incorrect
		// credentials

		client = createClient(new CloudCredentials(credentials.userEmail, credentials.password),
				CloudFoundryTestFixture.CF_PIVOTAL_SERVER_URL_HTTP);

		operationsHandler = new CloudFoundryLoginHandler(client);

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			fail("Failed to log in due to: " + e.getMessage());
		}
	}

	/*
	 * 
	 * Helper methods
	 */

	protected void assertError(CoreException ce) {
		Throwable cause = ce.getCause();

		assertTrue("Expected " + CloudFoundryException.class.getName() + " but got " + cause.getClass().getName(),
				cause instanceof CloudFoundryException);

		CloudFoundryException cfe = (CloudFoundryException) cause;

		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	protected CloudFoundryOperations createClient(CloudCredentials credentials, String url) throws Exception {
		return CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(credentials, new URL(url),
				false);
	}

	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}
}
