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

import junit.framework.TestCase;

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
public class CloudFoundryClientTest extends TestCase {

	public static final String VALID_V1_HTTP_URL = "http://api.cloudfoundry.com";

	public static final String VALID_V1_HTTPS_URL = "https://api.cloudfoundry.com";

	public void testConnectToNonSecureUrl() throws Exception {
		CloudFoundryOperations client = CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory()
				.getCloudFoundryOperations(VALID_V1_HTTP_URL);

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testConnectToSecureUrl() throws Exception {
		CloudFoundryOperations client = CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory()
				.getCloudFoundryOperations(VALID_V1_HTTPS_URL);
		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testValidPasswordOperationHandler() throws Exception {

		CredentialProperties credentials = CloudFoundryTestFixture.getUserTestCredentials();

		CloudFoundryOperations client = CloudFoundryServerBehaviour.createClient(VALID_V1_HTTP_URL,
				credentials.getUserEmail(), credentials.getPassword());

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client, null);

		// Throws exception if it failed login. Let junit detect the exception
		// as an error.
		operationsHandler.login(new NullProgressMonitor());

	}

	public void testInvalidPasswordOperationHandler() throws Exception {

		CredentialProperties credentials = CloudFoundryTestFixture.getUserTestCredentials();

		String invalidPassword = "invalid password";

		CloudFoundryOperations client = CloudFoundryServerBehaviour.createClient(VALID_V1_HTTP_URL,
				credentials.getUserEmail(), invalidPassword);

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client, null);
		CoreException error = null;

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			error = e;
		}

		handleErrorCheck(error);
	}

	public void testInvalidUsernameOperationHandler() throws Exception {

		CredentialProperties credentials = CloudFoundryTestFixture.getUserTestCredentials();

		String invalidUsername = "invalid username";

		CloudFoundryOperations client = CloudFoundryServerBehaviour.createClient(VALID_V1_HTTP_URL, invalidUsername,
				credentials.getPassword());

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client, null);
		CoreException error = null;

		try {
			operationsHandler.login(new NullProgressMonitor());
		}
		catch (CoreException e) {
			error = e;
		}

		handleErrorCheck(error);
	}

	protected void handleErrorCheck(CoreException ce) {
		Throwable cause = ce.getCause();

		assertTrue("Expected " + CloudFoundryException.class.getName() + " but got " + cause.getClass().getName(),
				cause instanceof CloudFoundryException);

		CloudFoundryException cfe = (CloudFoundryException) cause;

		assertEquals("403 Error requesting access token.", cfe.getMessage());
	}
}
